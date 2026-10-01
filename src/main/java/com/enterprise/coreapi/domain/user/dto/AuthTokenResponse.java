package com.enterprise.coreapi.domain.user.dto;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        UserProfileResponse user
) {}
