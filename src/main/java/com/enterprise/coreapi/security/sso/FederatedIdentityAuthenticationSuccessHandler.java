package com.enterprise.coreapi.security.sso;

import com.enterprise.coreapi.domain.user.entity.Role;
import com.enterprise.coreapi.domain.user.entity.User;
import com.enterprise.coreapi.domain.user.entity.UserAuditLog;
import com.enterprise.coreapi.domain.user.repository.UserAuditLogRepository;
import com.enterprise.coreapi.domain.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FederatedIdentityAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final JwtEncoder jwtEncoder;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    public FederatedIdentityAuthenticationSuccessHandler(UserAuditLogRepository auditLogRepository,
                                                        UserRepository userRepository,
                                                        JwtEncoder jwtEncoder) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.jwtEncoder = jwtEncoder;
        setDefaultTargetUrl("/api/v1/users/me");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        User loggedInUser = null;
        String authProvider = "OAUTH2";

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String userPublicIdStr = (String) oauth2User.getAttributes().get("user_id");
            authProvider = (String) oauth2User.getAttributes().get("auth_provider");

            Long internalUserId = null;
            if (userPublicIdStr != null) {
                try {
                    UUID publicId = UUID.fromString(userPublicIdStr);
                    Optional<User> userOpt = userRepository.findByPublicId(publicId);
                    if (userOpt.isPresent()) {
                        loggedInUser = userOpt.get();
                        internalUserId = loggedInUser.getId();
                    }
                } catch (IllegalArgumentException ignored) {}
            }

            if (loggedInUser == null) {
                String email = (String) oauth2User.getAttributes().get("email");
                if (email != null && !email.isBlank()) {
                    Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
                    if (userOpt.isPresent()) {
                        loggedInUser = userOpt.get();
                        internalUserId = loggedInUser.getId();
                    }
                }
            }

            String ipAddress = extractClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            UserAuditLog auditLog = new UserAuditLog(
                    internalUserId,
                    "SOCIAL_" + (authProvider != null ? authProvider : "OAUTH2"),
                    "LOGIN_SUCCESS",
                    ipAddress,
                    userAgent,
                    "Başarılı sosyal federasyon oturumu: " + authProvider
            );
            auditLogRepository.save(auditLog);
            log.info("Sosyal giriş başarılı: userId={}, provider={}", loggedInUser != null ? loggedInUser.getPublicId() : "N/A", authProvider);
        }

        // Eğer SSO PKCE yetkilendirme isteği varsa (savedRequest), akışı SAS authorization-code'a devam ettir
        SavedRequest savedRequest = this.requestCache.getRequest(request, response);
        if (savedRequest != null) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        // SPA'dan doğrudan başlatılan sosyal giriş: JWT üretip Angular callback URL'ine yönlendir
        if (loggedInUser != null) {
            Instant now = Instant.now();
            long expiresInSeconds = 600;
            Instant expiresAt = now.plus(expiresInSeconds, ChronoUnit.SECONDS);

            Set<String> roles = loggedInUser.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());

            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer("http://localhost:8080")
                    .issuedAt(now)
                    .expiresAt(expiresAt)
                    .subject(loggedInUser.getEmail())
                    .claim("user_id", loggedInUser.getPublicId().toString())
                    .claim("roles", roles)
                    .claim("auth_provider", authProvider != null ? authProvider : "SOCIAL")
                    .claim("tenant_id", "enterprise-corp")
                    .build();

            String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, "http://localhost:4200/auth/callback?token=" + accessToken);
            return;
        }

        // Kullanıcı çözümlenememişse frontend login sayfasına hata parametresiyle yönlendir
        log.warn("Sosyal giriş sonrası kullanıcı profili çözümlenemedi, login sayfasına yönlendiriliyor.");
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, "http://localhost:4200/login?error=social_auth_failed");
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
