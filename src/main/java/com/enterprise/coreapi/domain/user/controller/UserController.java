package com.enterprise.coreapi.domain.user.controller;

import com.enterprise.coreapi.common.exception.ApiException;
import com.enterprise.coreapi.common.exception.ErrorCode;
import com.enterprise.coreapi.common.response.ApiResponse;
import com.enterprise.coreapi.domain.user.dto.SetPasswordRequest;
import com.enterprise.coreapi.domain.user.dto.UserProfileResponse;
import com.enterprise.coreapi.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "2. Kullanıcı Profil Yönetimi (Users)", description = "Profil görüntüleme, parola belirleme ve sosyal hesap bağlama/koparma uç noktaları")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Aktif Kullanıcı Profilini Getir", description = "Oturum açmış kullanıcının bilgilerini, rollerini ve bağlı sosyal hesaplarını listeler.")
    public ApiResponse<UserProfileResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_ACCESS, "Geçerli bir oturum bulunamadı.");
        }

        UserProfileResponse response;
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String userIdClaim = jwt.getClaimAsString("user_id");
            if (userIdClaim != null) {
                response = userService.getProfileByPublicId(UUID.fromString(userIdClaim));
            } else {
                response = userService.getProfileByEmail(jwt.getSubject());
            }
        } else {
            response = userService.getProfileByEmail(authentication.getName());
        }

        String traceId = MDC.get("traceId");
        return ApiResponse.success(response, traceId);
    }

    @PostMapping("/me/set-password")
    @Operation(summary = "Yerel Parola Belirle", description = "Sosyal oturum açmayla gelen veya şifresini güncellemek isteyen kullanıcının yerel parola atamasını sağlar.")
    public ApiResponse<Void> setPassword(Authentication authentication,
                                         @Valid @RequestBody SetPasswordRequest request) {
        UUID publicId = resolveUserPublicId(authentication);
        userService.setPassword(publicId, request);
        String traceId = MDC.get("traceId");
        return ApiResponse.success(null, "Parolanız başarıyla güncellendi.", traceId);
    }

    @DeleteMapping("/me/social/{provider}")
    @Operation(summary = "Sosyal Hesap Bağlantısını Kopar", description = "Kullanıcının hesabına bağlı Google veya GitHub federasyonunu kaldırır. Tek giriş yöntemi bu hesap ise silme engellenir.")
    public ApiResponse<Void> unlinkSocial(Authentication authentication,
                                          @PathVariable String provider) {
        UUID publicId = resolveUserPublicId(authentication);
        userService.unlinkSocialAccount(publicId, provider);
        String traceId = MDC.get("traceId");
        return ApiResponse.success(null, provider + " sosyal hesap bağlantısı başarıyla kaldırıldı.", traceId);
    }

    private UUID resolveUserPublicId(Authentication authentication) {
        if (authentication == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_ACCESS, "Yetkisiz istek.");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String userIdClaim = jwt.getClaimAsString("user_id");
            if (userIdClaim != null) {
                return UUID.fromString(userIdClaim);
            }
            return userService.getProfileByEmail(jwt.getSubject()).publicId();
        }

        return userService.getProfileByEmail(authentication.getName()).publicId();
    }
}
