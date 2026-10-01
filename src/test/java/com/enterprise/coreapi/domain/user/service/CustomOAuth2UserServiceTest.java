package com.enterprise.coreapi.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterprise.coreapi.domain.user.repository.RoleRepository;
import com.enterprise.coreapi.domain.user.repository.UserRepository;
import com.enterprise.coreapi.domain.user.repository.UserSocialAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSocialAccountRepository socialAccountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("FederatedProfile doğrulaması: emailVerified false ise profil doğrulanamaz")
    void shouldReflectUnverifiedEmailInFederatedProfile() {
        CustomOAuth2UserService.FederatedProfile unverifiedProfile = new CustomOAuth2UserService.FederatedProfile(
                "GOOGLE", "sub-123", "unverified@test.com", false, "Test", "User"
        );

        assertThat(unverifiedProfile.isEmailVerified()).isFalse();
        assertThat(unverifiedProfile.provider()).isEqualTo("GOOGLE");
    }

    @Test
    @DisplayName("FederatedProfile doğrulaması: emailVerified true ise profil geçerlidir")
    void shouldReflectVerifiedEmailInFederatedProfile() {
        CustomOAuth2UserService.FederatedProfile verifiedProfile = new CustomOAuth2UserService.FederatedProfile(
                "GITHUB", "gh-456", "verified@test.com", true, "Git", "Hub"
        );

        assertThat(verifiedProfile.isEmailVerified()).isTrue();
        assertThat(verifiedProfile.email()).isEqualTo("verified@test.com");
    }
}
