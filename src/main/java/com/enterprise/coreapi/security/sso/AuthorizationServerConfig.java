package com.enterprise.coreapi.security.sso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.UUID;

/**
 * Spring Authorization Server Çekirdek Kalıcılık Konfigürasyonu:
 * PostgreSQL üzerinde RegisteredClientRepository, OAuth2AuthorizationService ve OAuth2AuthorizationConsentService.
 */
@Configuration
public class AuthorizationServerConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationServerConfig.class);

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
                                                           RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
                                                                         RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    /**
     * Uygulama başlarken varsayılan kurumsal istemcileri (Web Portal SPA & M2M Service)
     * veritabanında yoksa otomatik oluşturur (Seed Clients).
     */
    @Bean
    public ApplicationRunner registeredClientsInitializer(RegisteredClientRepository clientRepository,
                                                          PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Web Portal SPA / Frontend İstemcisi (Public Client + S256 PKCE)
            if (clientRepository.findByClientId("web-portal-client") == null) {
                RegisteredClient webPortalClient = RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId("web-portal-client")
                        .clientName("Enterprise Web Portal (SPA)")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // Public Client
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .redirectUri("http://localhost:3000/oauth2/callback")
                        .redirectUri("http://127.0.0.1:3000/oauth2/callback")
                        .postLogoutRedirectUri("http://localhost:3000/")
                        .scope(OidcScopes.OPENID)
                        .scope(OidcScopes.PROFILE)
                        .scope(OidcScopes.EMAIL)
                        .scope("user:read")
                        .scope("user:write")
                        .clientSettings(ClientSettings.builder()
                                .requireAuthorizationConsent(false)
                                .requireProofKey(true) // PKCE Zorunlu (S256)
                                .build())
                        .tokenSettings(TokenSettings.builder()
                                .accessTokenTimeToLive(Duration.ofMinutes(10))
                                .refreshTokenTimeToLive(Duration.ofDays(30))
                                .reuseRefreshTokens(false) // Single-use Refresh Token Rotation
                                .build())
                        .build();

                clientRepository.save(webPortalClient);
                log.info("Varsayılan OAuth 2.1 PKCE istemcisi kaydedildi: web-portal-client");
            }

            // 2. Mikroservisler Arası M2M İstemcisi (Confidential Client + Client Credentials)
            if (clientRepository.findByClientId("internal-service-client") == null) {
                RegisteredClient serviceClient = RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId("internal-service-client")
                        .clientSecret(passwordEncoder.encode("internal_secret_2026"))
                        .clientName("Internal Microservice M2M Client")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .scope("internal:all")
                        .tokenSettings(TokenSettings.builder()
                                .accessTokenTimeToLive(Duration.ofMinutes(15))
                                .build())
                        .build();

                clientRepository.save(serviceClient);
                log.info("Varsayılan M2M istemcisi kaydedildi: internal-service-client");
            }
        };
    }
}
