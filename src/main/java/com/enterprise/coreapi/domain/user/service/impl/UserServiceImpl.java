package com.enterprise.coreapi.domain.user.service.impl;

import com.enterprise.coreapi.common.exception.ApiException;
import com.enterprise.coreapi.common.exception.ErrorCode;
import com.enterprise.coreapi.common.exception.ResourceNotFoundException;
import com.enterprise.coreapi.domain.user.dto.*;
import com.enterprise.coreapi.domain.user.entity.Role;
import com.enterprise.coreapi.domain.user.entity.User;
import com.enterprise.coreapi.domain.user.entity.UserAuditLog;
import com.enterprise.coreapi.domain.user.event.UserPasswordUpdatedEvent;
import com.enterprise.coreapi.domain.user.event.UserRegisteredEvent;
import com.enterprise.coreapi.domain.user.mapper.UserMapper;
import com.enterprise.coreapi.domain.user.repository.RoleRepository;
import com.enterprise.coreapi.domain.user.repository.UserAuditLogRepository;
import com.enterprise.coreapi.domain.user.repository.UserRepository;
import com.enterprise.coreapi.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserAuditLogRepository auditLogRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserProfileResponse register(RegisterUserRequest request) {
        log.info("Kullanıcı kayıt isteği alındı: email={}", request.email());

        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ApiException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Bu e-posta adresi ile kayıtlı bir kullanıcı zaten mevcut.");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), hashedPassword, request.firstName(), request.lastName());

        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER", "Standart Kullanıcı")));
        user.addRole(defaultRole);

        User savedUser = userRepository.save(user);

        // Audit Log
        UserAuditLog audit = new UserAuditLog(savedUser.getId(), "LOCAL", "REGISTER_SUCCESS", "127.0.0.1", "API Client", "Yerel kullanıcı kaydı oluşturuldu");
        auditLogRepository.save(audit);

        // Domain Event
        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser.getPublicId(), savedUser.getEmail(), "LOCAL"));

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        log.info("Yerel oturum açma isteği: email={}", request.email());

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED_ACCESS, "Geçersiz e-posta veya şifre."));

        if (user.isAccountLocked()) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Hesap çok sayıda başarısız deneme nedeniyle geçici olarak kilitlenmiştir.");
        }

        if (!user.hasLocalPassword()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_ACCESS, "Bu hesap sosyal oturum açma ile oluşturulmuştur. Lütfen Google veya GitHub ile giriş yapınız.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            userRepository.save(user);

            UserAuditLog failAudit = new UserAuditLog(user.getId(), "LOCAL", "LOGIN_FAILURE", "127.0.0.1", "API Client", "Hatalı şifre girişi");
            auditLogRepository.save(failAudit);

            throw new ApiException(ErrorCode.UNAUTHORIZED_ACCESS, "Geçersiz e-posta veya şifre.");
        }

        user.resetFailedAttempts();
        userRepository.save(user);

        // Audit Log
        UserAuditLog successAudit = new UserAuditLog(user.getId(), "LOCAL", "LOGIN_SUCCESS", "127.0.0.1", "API Client", "Başarılı yerel oturum");
        auditLogRepository.save(successAudit);

        // JWT Token Üretimi
        Instant now = Instant.now();
        long expiresInSeconds = 600; // 10 dakika
        Instant expiresAt = now.plus(expiresInSeconds, ChronoUnit.SECONDS);

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost:8080")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("user_id", user.getPublicId().toString())
                .claim("roles", roles)
                .claim("auth_provider", "LOCAL")
                .claim("tenant_id", "enterprise-corp")
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        String refreshToken = UUID.randomUUID().toString();

        return new AuthTokenResponse(
                accessToken,
                "Bearer",
                expiresInSeconds,
                refreshToken,
                userMapper.toResponse(user)
        );
    }

    @Override
    public UserProfileResponse getProfileByPublicId(UUID publicId) {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "publicId", publicId));
        return userMapper.toResponse(user);
    }

    @Override
    public UserProfileResponse getProfileByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "email", email));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void setPassword(UUID publicId, SetPasswordRequest request) {
        if (!Objects.equals(request.newPassword(), request.confirmPassword())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Yeni şifre ile şifre tekrarı uyuşmuyor.");
        }

        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "publicId", publicId));

        String hashedPassword = passwordEncoder.encode(request.newPassword());
        user.setPassword(hashedPassword);
        userRepository.save(user);

        UserAuditLog audit = new UserAuditLog(user.getId(), "LOCAL", "PASSWORD_SET", "127.0.0.1", "API Client", "Kullanıcı yeni şifre belirledi");
        auditLogRepository.save(audit);

        eventPublisher.publishEvent(new UserPasswordUpdatedEvent(user.getPublicId(), user.getEmail()));
    }

    @Override
    @Transactional
    public void unlinkSocialAccount(UUID publicId, String provider) {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "publicId", publicId));

        try {
            user.unlinkSocialAccount(provider);
            userRepository.save(user);

            UserAuditLog audit = new UserAuditLog(user.getId(), "LOCAL", "ACCOUNT_UNLINKED", "127.0.0.1", "API Client", provider + " bağlantısı koparıldı");
            auditLogRepository.save(audit);
        } catch (IllegalStateException ex) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, ex.getMessage());
        }
    }
}
