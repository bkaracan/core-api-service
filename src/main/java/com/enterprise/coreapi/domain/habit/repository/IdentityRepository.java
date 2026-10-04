package com.enterprise.coreapi.domain.habit.repository;

import com.enterprise.coreapi.domain.habit.entity.Identity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdentityRepository extends JpaRepository<Identity, Long> {

    List<Identity> findAllByUser_PublicIdOrderByDisplayOrderAsc(UUID userPublicId);

    Optional<Identity> findByPublicIdAndUser_PublicId(UUID publicId, UUID userPublicId);

    Optional<Identity> findByPublicId(UUID publicId);
}
