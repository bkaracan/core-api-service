package com.enterprise.coreapi.domain.user.service;

import com.enterprise.coreapi.domain.user.dto.AuthTokenResponse;
import com.enterprise.coreapi.domain.user.dto.LoginRequest;
import com.enterprise.coreapi.domain.user.dto.RegisterUserRequest;
import com.enterprise.coreapi.domain.user.dto.SetPasswordRequest;
import com.enterprise.coreapi.domain.user.dto.UserProfileResponse;

import java.util.UUID;

public interface UserService {

    UserProfileResponse register(RegisterUserRequest request);

    AuthTokenResponse login(LoginRequest request);

    UserProfileResponse getProfileByPublicId(UUID publicId);

    UserProfileResponse getProfileByEmail(String email);

    void setPassword(UUID publicId, SetPasswordRequest request);

    void unlinkSocialAccount(UUID publicId, String provider);
}
