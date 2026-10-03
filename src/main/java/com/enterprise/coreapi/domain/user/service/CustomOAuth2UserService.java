package com.enterprise.coreapi.domain.user.service;

import com.enterprise.coreapi.domain.user.entity.Role;
import com.enterprise.coreapi.domain.user.entity.User;
import com.enterprise.coreapi.domain.user.entity.UserSocialAccount;
import com.enterprise.coreapi.domain.user.event.SocialAccountLinkedEvent;
import com.enterprise.coreapi.domain.user.event.UserRegisteredEvent;
import com.enterprise.coreapi.domain.user.repository.RoleRepository;
import com.enterprise.coreapi.domain.user.repository.UserRepository;
import com.enterprise.coreapi.domain.user.repository.UserSocialAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RestTemplate restTemplate = new RestTemplate();

    public CustomOAuth2UserService(UserRepository userRepository,
                                   UserSocialAccountRepository socialAccountRepository,
                                   RoleRepository roleRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.roleRepository = roleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase(Locale.ROOT);

        // 1. Profil Bilgilerini Sağlayıcıya Göre Ayrıştır (Parsing)
        FederatedProfile profile = extractProfile(registrationId, oAuth2User, userRequest);

        // 2. Pre-Account Takeover Koruması: E-posta doğrulanmış mı?
        if (!profile.isEmailVerified()) {
            log.warn("Doğrulanmamış e-posta ile sosyal giriş reddedildi: provider={}, email={}", profile.provider(), profile.email());
            throw new OAuth2AuthenticationException(new OAuth2Error("email_not_verified"),
                    "Sosyal sağlayıcı e-posta adresini doğrulamadığı için hesap ilişkilendirilemez: " + profile.email());
        }

        // 3. JIT Provisioning ve Güvenli Hesap Eşleme (Account Linking)
        User user = resolveOrCreateUser(profile);

        // 4. Kurumsal Granted Authorities Listesini Oluştur
        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.getName()))
                .collect(Collectors.toSet());

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("user_id", user.getPublicId().toString());
        attributes.put("auth_provider", profile.provider());
        attributes.put("email", user.getEmail());

        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(authorities, attributes, nameAttributeKey);
    }

    public User resolveOrCreateUser(FederatedProfile profile) {
        // A. Sosyal hesap zaten bağlı mı?
        Optional<UserSocialAccount> existingSocial = socialAccountRepository
                .findByProviderAndProviderUserId(profile.provider(), profile.providerUserId());

        if (existingSocial.isPresent()) {
            return existingSocial.get().getUser();
        }

        // B. Aynı e-postaya sahip yerel kullanıcı var mı? (Account Linking)
        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(profile.email());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.linkSocialAccount(profile.provider(), profile.providerUserId(), profile.email());
            User saved = userRepository.save(user);
            log.info("Mevcut kullanıcıya sosyal hesap bağlandı: email={}, provider={}", profile.email(), profile.provider());
            eventPublisher.publishEvent(new SocialAccountLinkedEvent(saved.getPublicId(), profile.provider(), profile.email()));
            return saved;
        }

        // C. Yeni Kullanıcı Oluştur (Just-In-Time Provisioning)
        User newUser = new User(profile.email(), null, profile.firstName(), profile.lastName());
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER", "Standart Kullanıcı")));
        newUser.addRole(userRole);
        newUser.linkSocialAccount(profile.provider(), profile.providerUserId(), profile.email());

        User savedUser = userRepository.save(newUser);
        log.info("Yeni JIT kullanıcısı oluşturuldu: email={}, provider={}", profile.email(), profile.provider());
        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser.getPublicId(), savedUser.getEmail(), "FEDERATED_" + profile.provider()));
        return savedUser;
    }

    private FederatedProfile extractProfile(String provider, OAuth2User oAuth2User, OAuth2UserRequest request) {
        if ("GOOGLE".equalsIgnoreCase(provider)) {
            String sub = oAuth2User.getAttribute("sub");
            String email = oAuth2User.getAttribute("email");
            Boolean emailVerified = oAuth2User.getAttribute("email_verified");
            String givenName = oAuth2User.getAttribute("given_name");
            String familyName = oAuth2User.getAttribute("family_name");
            return new FederatedProfile("GOOGLE", sub, email, Boolean.TRUE.equals(emailVerified),
                    givenName != null ? givenName : "GoogleUser", familyName != null ? familyName : "");
        } else if ("GITHUB".equalsIgnoreCase(provider)) {
            Object idObj = oAuth2User.getAttribute("id");
            String id = idObj != null ? String.valueOf(idObj) : "";
            Object nameObj = oAuth2User.getAttribute("name");
            String name = nameObj != null ? String.valueOf(nameObj) : null;
            Object loginObj = oAuth2User.getAttribute("login");
            String login = loginObj != null ? String.valueOf(loginObj) : null;
            String displayName = (name != null && !name.isBlank()) ? name : (login != null ? login : "GitHubUser");
            String[] names = displayName.split(" ", 2);
            String firstName = names[0];
            String lastName = names.length > 1 ? names[1] : "";

            String token = request.getAccessToken().getTokenValue();
            VerifiedEmail verifiedEmail = fetchGitHubPrimaryVerifiedEmail(token);

            String email = verifiedEmail.email();
            boolean isVerified = verifiedEmail.verified();

            // Eğer emails endpoint'inden alınamadıysa oAuth2User attribute'unu veya noreply e-postasını kullan
            if (!isVerified || email == null || email.isBlank() || "no-verified-email@github.internal".equals(email)) {
                String publicEmail = oAuth2User.getAttribute("email");
                if (publicEmail != null && !publicEmail.isBlank()) {
                    email = publicEmail;
                    isVerified = true;
                } else {
                    String loginUser = login != null ? login : ("user" + id);
                    email = loginUser + "@users.noreply.github.com";
                    isVerified = true;
                    log.info("GitHub e-postası temin edilemedi, resmi noreply e-postası kullanılıyor: {}", email);
                }
            }

            return new FederatedProfile("GITHUB", id, email, isVerified, firstName, lastName);
        }
        throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"), "Desteklenmeyen sağlayıcı: " + provider);
    }

    private VerifiedEmail fetchGitHubPrimaryVerifiedEmail(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("User-Agent", "Enterprise-Core-API-SSO/1.0");
            headers.set("Accept", "application/vnd.github.v3+json");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getBody() != null) {
                // 1. Primary & Verified e-posta
                for (Map<String, Object> emailEntry : response.getBody()) {
                    boolean primary = Boolean.TRUE.equals(emailEntry.get("primary"));
                    boolean verified = Boolean.TRUE.equals(emailEntry.get("verified"));
                    if (primary && verified) {
                        return new VerifiedEmail((String) emailEntry.get("email"), true);
                    }
                }
                // 2. Herhangi bir Verified e-posta
                for (Map<String, Object> emailEntry : response.getBody()) {
                    boolean verified = Boolean.TRUE.equals(emailEntry.get("verified"));
                    if (verified) {
                        return new VerifiedEmail((String) emailEntry.get("email"), true);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("GitHub e-posta listesi çekilirken hata oluştu: {}", ex.getMessage());
        }
        return new VerifiedEmail("no-verified-email@github.internal", false);
    }

    public record FederatedProfile(String provider, String providerUserId, String email, boolean isEmailVerified, String firstName, String lastName) {}
    public record VerifiedEmail(String email, boolean verified) {}
}
