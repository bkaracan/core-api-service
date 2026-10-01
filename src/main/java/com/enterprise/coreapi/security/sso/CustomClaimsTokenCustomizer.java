package com.enterprise.coreapi.security.sso;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class CustomClaimsTokenCustomizer {

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            Authentication principal = context.getPrincipal();

            Set<String> roles = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            String authProvider = "LOCAL";
            String userId = principal.getName();

            if (principal.getPrincipal() instanceof OAuth2User oauth2User) {
                if (oauth2User.getAttributes().containsKey("auth_provider")) {
                    authProvider = String.valueOf(oauth2User.getAttributes().get("auth_provider"));
                }
                if (oauth2User.getAttributes().containsKey("user_id")) {
                    userId = String.valueOf(oauth2User.getAttributes().get("user_id"));
                }
            }

            final String finalAuthProvider = authProvider;
            final String finalUserId = userId;

            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                context.getClaims().claims(claims -> {
                    claims.put("user_id", finalUserId);
                    claims.put("roles", roles);
                    claims.put("auth_provider", finalAuthProvider);
                    claims.put("tenant_id", "enterprise-corp");
                    claims.put("iss_type", "internal-identity-broker");
                });
            }

            if ("id_token".equals(context.getTokenType().getValue())) {
                context.getClaims().claims(claims -> {
                    claims.put("user_id", finalUserId);
                    claims.put("roles", roles);
                    claims.put("auth_provider", finalAuthProvider);
                    claims.put("preferred_username", principal.getName());
                });
            }
        };
    }
}
