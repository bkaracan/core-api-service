package com.enterprise.coreapi.common.config;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Spring Security Context üzerinden aktif kullanıcı bilgisini (JWT Subject / Username)
 * yakalayarak JPA @CreatedBy ve @LastModifiedBy alanlarına otomatik enjekte eder.
 */
@Component("securityAuditorAware")
public class SecurityAuditorAware implements AuditorAware<String> {

    public static final String SYSTEM_USER = "SYSTEM";

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(SYSTEM_USER);
        }

        String username = authentication.getName();
        return (username != null && !username.isBlank()) ? Optional.of(username) : Optional.of(SYSTEM_USER);
    }
}
