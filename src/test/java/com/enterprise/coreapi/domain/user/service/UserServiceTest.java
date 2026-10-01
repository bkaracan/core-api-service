package com.enterprise.coreapi.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.enterprise.coreapi.common.exception.ApiException;
import com.enterprise.coreapi.common.exception.ErrorCode;
import com.enterprise.coreapi.domain.user.dto.AuthTokenResponse;
import com.enterprise.coreapi.domain.user.dto.LoginRequest;
import com.enterprise.coreapi.domain.user.dto.RegisterUserRequest;
import com.enterprise.coreapi.domain.user.dto.SetPasswordRequest;
import com.enterprise.coreapi.domain.user.dto.UserProfileResponse;
import com.enterprise.coreapi.domain.user.entity.Role;
import com.enterprise.coreapi.domain.user.entity.User;
import com.enterprise.coreapi.domain.user.event.UserPasswordUpdatedEvent;
import com.enterprise.coreapi.domain.user.event.UserRegisteredEvent;
import com.enterprise.coreapi.domain.user.mapper.UserMapper;
import com.enterprise.coreapi.domain.user.repository.RoleRepository;
import com.enterprise.coreapi.domain.user.repository.UserAuditLogRepository;
import com.enterprise.coreapi.domain.user.repository.UserRepository;
import com.enterprise.coreapi.domain.user.service.impl.UserServiceImpl;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserAuditLogRepository auditLogRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role("ROLE_USER", "Standart Kullanıcı");
        sampleUser = new User("burak@enterprise.com", "encodedPassword", "Burak", "Kaya");
        sampleUser.addRole(userRole);
    }

    @Test
    @DisplayName("Başarılı kullanıcı kaydı gerçekleşmeli ve event yayınlanmalıdır")
    void shouldRegisterUserSuccessfully() {
        // Arrange
        RegisterUserRequest request = new RegisterUserRequest("burak@enterprise.com", "Secret123!", "Burak", "Kaya");
        UserProfileResponse expectedResponse = new UserProfileResponse(
                sampleUser.getPublicId(),
                "burak@enterprise.com",
                "Burak",
                "Kaya",
                true,
                "ACTIVE",
                Set.of("ROLE_USER"),
                Collections.emptySet(),
                Instant.now()
        );

        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(userMapper.toResponse(sampleUser)).thenReturn(expectedResponse);

        // Act
        UserProfileResponse actual = userService.register(request);

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.email()).isEqualTo("burak@enterprise.com");
        verify(userRepository).save(any(User.class));
        verify(auditLogRepository).save(any());
        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    @DisplayName("Var olan e-posta ile kayıt olunduğunda RESOURCE_ALREADY_EXISTS hatası fırlatılmalıdır")
    void shouldThrowExceptionWhenRegisteringWithExistingEmail() {
        // Arrange
        RegisterUserRequest request = new RegisterUserRequest("burak@enterprise.com", "Secret123!", "Burak", "Kaya");
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_ALREADY_EXISTS);
                });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Geçerli kimlik bilgileriyle yerel oturum açılabilmeli ve JWT Access Token dönmelidir")
    void shouldLoginSuccessfully() {
        // Arrange
        LoginRequest request = new LoginRequest("burak@enterprise.com", "Secret123!");
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mock.jwt.token");

        when(userRepository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(request.password(), sampleUser.getPasswordHash())).thenReturn(true);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);
        when(userMapper.toResponse(sampleUser)).thenReturn(mock(UserProfileResponse.class));

        // Act
        AuthTokenResponse response = userService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Hatalı şifre girildiğinde başarısız deneme sayısı artmalı ve UNAUTHORIZED_ACCESS dönmelidir")
    void shouldRecordFailedLoginOnBadPassword() {
        // Arrange
        LoginRequest request = new LoginRequest("burak@enterprise.com", "WrongPassword");
        when(userRepository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(request.password(), sampleUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED_ACCESS);
                });

        assertThat(sampleUser.getFailedAttempts()).isEqualTo(1);
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Sosyal kullanıcının tek kimlik yöntemi olan sosyal hesabı kaldırması engellenmelidir")
    void shouldPreventUnlinkingOnlyLoginMethod() {
        // Yerel şifresi olmayan sosyal kullanıcı
        User socialOnlyUser = new User("social@enterprise.com", null, "Social", "User");
        socialOnlyUser.linkSocialAccount("GOOGLE", "google-sub-123", "social@enterprise.com");

        when(userRepository.findByPublicId(socialOnlyUser.getPublicId())).thenReturn(Optional.of(socialOnlyUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.unlinkSocialAccount(socialOnlyUser.getPublicId(), "GOOGLE"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                });
    }

    @Test
    @DisplayName("Kullanıcı yerel parola belirleyebilmeli ve event fırlatılmalıdır")
    void shouldSetPasswordSuccessfully() {
        // Arrange
        SetPasswordRequest request = new SetPasswordRequest("NewSecret123!", "NewSecret123!");
        when(userRepository.findByPublicId(sampleUser.getPublicId())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode(request.newPassword())).thenReturn("newEncodedHash");

        // Act
        userService.setPassword(sampleUser.getPublicId(), request);

        // Assert
        assertThat(sampleUser.getPasswordHash()).isEqualTo("newEncodedHash");
        verify(userRepository).save(sampleUser);
        verify(eventPublisher).publishEvent(any(UserPasswordUpdatedEvent.class));
    }
}
