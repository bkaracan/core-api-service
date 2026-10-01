package com.enterprise.coreapi.domain.user.controller;

import com.enterprise.coreapi.common.response.ApiResponse;
import com.enterprise.coreapi.domain.user.dto.AuthTokenResponse;
import com.enterprise.coreapi.domain.user.dto.LoginRequest;
import com.enterprise.coreapi.domain.user.dto.RegisterUserRequest;
import com.enterprise.coreapi.domain.user.dto.UserProfileResponse;
import com.enterprise.coreapi.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "1. Kimlik Doğrulama & SSO (Auth)", description = "Yerel kullanıcı kaydı, oturum açma ve kurumsal SSO uç noktaları")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Yerel Kullanıcı Kaydı", description = "Yeni kullanıcı hesabı oluşturur, Argon2id ile şifreler ve varsayılan ROLE_USER atar.")
    public ApiResponse<UserProfileResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserProfileResponse response = userService.register(request);
        String traceId = MDC.get("traceId");
        return ApiResponse.success(response, "Kullanıcı başarıyla kaydedildi.", traceId);
    }

    @PostMapping("/login")
    @Operation(summary = "Yerel Oturum Açma", description = "E-posta ve şifre ile kimlik doğrulayıp 10 dakikalık kurumsal JWT Access Token döner.")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokenResponse response = userService.login(request);
        String traceId = MDC.get("traceId");
        return ApiResponse.success(response, "Oturum açma başarılı.", traceId);
    }
}
