package com.enterprise.coreapi.domain.user.service;

import com.enterprise.coreapi.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OpenID Connect (OIDC) Sağlayıcıları (örn. Google) için özel kullanıcı servisidir.
 * OIDC id_token ve userInfo üzerinden kullanıcıyı çözümler, JIT ile yerel veritabanında
 * hesap oluşturur/bağlar ve zenginleştirilmiş DefaultOidcUser döner.
 */
@Slf4j
@Service
public class CustomOidcUserService extends OidcUserService {

    private final CustomOAuth2UserService customOAuth2UserService;

    public CustomOidcUserService(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase(java.util.Locale.ROOT);

        String sub = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        Boolean emailVerified = oidcUser.getEmailVerified();
        String givenName = oidcUser.getGivenName();
        String familyName = oidcUser.getFamilyName();

        if (email == null) {
            email = oidcUser.getAttribute("email");
        }
        if (emailVerified == null) {
            Object ev = oidcUser.getAttribute("email_verified");
            emailVerified = Boolean.TRUE.equals(ev) || "true".equalsIgnoreCase(String.valueOf(ev));
        }
        if (givenName == null) {
            givenName = oidcUser.getAttribute("given_name");
        }
        if (familyName == null) {
            familyName = oidcUser.getAttribute("family_name");
        }

        CustomOAuth2UserService.FederatedProfile profile = new CustomOAuth2UserService.FederatedProfile(
                registrationId,
                sub != null ? sub : oidcUser.getName(),
                email,
                Boolean.TRUE.equals(emailVerified),
                givenName != null ? givenName : "GoogleUser",
                familyName != null ? familyName : ""
        );

        if (!profile.isEmailVerified()) {
            log.warn("Doğrulanmamış e-posta ile OIDC giriş reddedildi: provider={}, email={}", profile.provider(), profile.email());
            throw new OAuth2AuthenticationException(new OAuth2Error("email_not_verified"),
                    "Sosyal sağlayıcı e-posta adresini doğrulamadığı için hesap ilişkilendirilemez: " + profile.email());
        }

        User user = customOAuth2UserService.resolveOrCreateUser(profile);

        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.getName()))
                .collect(Collectors.toSet());

        Map<String, Object> claims = new HashMap<>();
        if (oidcUser.getUserInfo() != null) {
            claims.putAll(oidcUser.getUserInfo().getClaims());
        }
        claims.putAll(oidcUser.getIdToken().getClaims());
        claims.put("user_id", user.getPublicId().toString());
        claims.put("auth_provider", profile.provider());

        OidcUserInfo oidcUserInfo = new OidcUserInfo(claims);
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        if (userNameAttributeName == null || userNameAttributeName.isBlank()) {
            userNameAttributeName = "sub";
        }

        log.info("OIDC kullanıcısı başarıyla yüklendi: email={}, userId={}", user.getEmail(), user.getPublicId());
        return new DefaultOidcUser(authorities, userRequest.getIdToken(), oidcUserInfo, userNameAttributeName);
    }
}
