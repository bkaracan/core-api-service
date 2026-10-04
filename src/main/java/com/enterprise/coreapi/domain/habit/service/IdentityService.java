package com.enterprise.coreapi.domain.habit.service;

import com.enterprise.coreapi.domain.habit.dto.CreateIdentityRequest;
import com.enterprise.coreapi.domain.habit.dto.IdentityResponse;

import java.util.List;
import java.util.UUID;

public interface IdentityService {

    List<IdentityResponse> getIdentitiesForUser(UUID userPublicId);

    IdentityResponse createIdentity(UUID userPublicId, CreateIdentityRequest request);

    IdentityResponse castVote(UUID identityPublicId, UUID userPublicId);

    void seedDefaultIdentitiesIfEmpty(UUID userPublicId);
}
