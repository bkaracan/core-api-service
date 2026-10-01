package com.enterprise.coreapi.security.sso;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
public class KeyStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(KeyStoreConfig.class);

    @Value("${sso.keystore.location:classpath:keystore/auth-server.p12}")
    private Resource keyStoreResource;

    @Value("${sso.keystore.password:enterprise_sso_secret_2026}")
    private String keyStorePassword;

    @Value("${sso.keystore.alias:sso-jwt-key}")
    private String keyAlias;

    @Value("${sso.keystore.key-password:enterprise_sso_secret_2026}")
    private String keyPassword;

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = loadOrGenerateRsaKey();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    private RSAKey loadOrGenerateRsaKey() {
        try {
            if (keyStoreResource != null && keyStoreResource.exists()) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                try (InputStream inputStream = keyStoreResource.getInputStream()) {
                    keyStore.load(inputStream, keyStorePassword.toCharArray());
                }

                RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
                Certificate certificate = keyStore.getCertificate(keyAlias);
                if (certificate != null && privateKey != null) {
                    RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();
                    log.info("KeyStore RSA anahtarı başarıyla yüklendi: {}", keyAlias);
                    return new RSAKey.Builder(publicKey)
                            .privateKey(privateKey)
                            .keyID(keyAlias)
                            .build();
                }
            }
        } catch (Exception ex) {
            log.warn("KeyStore yüklenirken hata oluştu ({}), yerel geliştirme RSA anahtarı üretiliyor: {}", keyStoreResource, ex.getMessage());
        }

        // KeyStore dosyası henüz oluşturulmadıysa dinamik RSA anahtarı üret (Local Dev Fallback)
        log.info("Dinamik üretim RSA anahtarı oluşturuldu.");
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("RSA KeyPair oluşturulamadı", ex);
        }
    }
}
