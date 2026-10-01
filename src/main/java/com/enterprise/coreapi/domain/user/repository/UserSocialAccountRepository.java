package com.enterprise.coreapi.domain.user.repository;

import com.enterprise.coreapi.domain.user.entity.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

    Optional<UserSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<UserSocialAccount> findByPublicId(UUID publicId);
}
