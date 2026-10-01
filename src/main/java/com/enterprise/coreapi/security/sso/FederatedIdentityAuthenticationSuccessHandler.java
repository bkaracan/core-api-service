package com.enterprise.coreapi.security.sso;

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
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class FederatedIdentityAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public FederatedIdentityAuthenticationSuccessHandler(UserAuditLogRepository auditLogRepository,
                                                        UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        setDefaultTargetUrl("/api/v1/users/me");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String userPublicIdStr = (String) oauth2User.getAttributes().get("user_id");
            String authProvider = (String) oauth2User.getAttributes().get("auth_provider");

            Long internalUserId = null;
            if (userPublicIdStr != null) {
                try {
                    UUID publicId = UUID.fromString(userPublicIdStr);
                    Optional<User> userOpt = userRepository.findByPublicId(publicId);
                    if (userOpt.isPresent()) {
                        internalUserId = userOpt.get().getId();
                    }
                } catch (IllegalArgumentException ignored) {}
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
            log.info("Sosyal giriş başarılı: userId={}, provider={}", userPublicIdStr, authProvider);
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
