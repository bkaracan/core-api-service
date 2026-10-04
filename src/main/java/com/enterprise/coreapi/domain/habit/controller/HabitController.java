package com.enterprise.coreapi.domain.habit.controller;

import com.enterprise.coreapi.common.exception.ApiException;
import com.enterprise.coreapi.common.exception.ErrorCode;
import com.enterprise.coreapi.common.response.ApiResponse;
import com.enterprise.coreapi.domain.habit.dto.*;
import com.enterprise.coreapi.domain.habit.service.HabitService;
import com.enterprise.coreapi.domain.habit.service.IdentityService;
import com.enterprise.coreapi.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/habits")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "3. Atomik Alışkanlıklar & Kaizen (Habits)", description = "4 Davranış Değişimi Yasası, Kimlik Odaklı Takip ve Kaizen PDCA uç noktaları")
public class HabitController {

    private final HabitService habitService;
    private final IdentityService identityService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Günün Atomik Alışkanlıklarını Getir", description = "Oturum açmış kullanıcının bugüne ait 4 Yasa alışkanlıklarını ve tamamlanma durumlarını listeler.")
    public ApiResponse<List<HabitResponse>> getTodayHabits(Authentication authentication) {
        UUID userPublicId = resolveUserPublicId(authentication);
        List<HabitResponse> habits = habitService.getHabitsForToday(userPublicId);
        String traceId = MDC.get("traceId");
        return ApiResponse.success(habits, traceId);
    }

    @PostMapping
    @Operation(summary = "Yeni Atomik Alışkanlık Tanımla", description = "4 Davranış Değişimi Yasasına uygun yeni bir alışkanlık oluşturur.")
    public ApiResponse<HabitResponse> createHabit(Authentication authentication,
                                                 @Valid @RequestBody CreateHabitRequest request) {
        UUID userPublicId = resolveUserPublicId(authentication);
        HabitResponse response = habitService.createHabit(userPublicId, request);
        String traceId = MDC.get("traceId");
        return ApiResponse.success(response, "Yeni atomik alışkanlık başarıyla tanımlandı.", traceId);
    }

    @PostMapping("/{publicId}/toggle")
    @Operation(summary = "Alışkanlığı Tamamla / Geri Al", description = "Alışkanlığın bugünkü tamamlanma durumunu değiştirir. Tamamlandığında streak ve kimlik oyu artırılır.")
    public ApiResponse<HabitResponse> toggleHabit(Authentication authentication,
                                                  @PathVariable UUID publicId,
                                                  @RequestParam(defaultValue = "false") boolean usedTwoMinuteRule) {
        UUID userPublicId = resolveUserPublicId(authentication);
        HabitResponse response = habitService.toggleHabitCompletion(publicId, userPublicId, usedTwoMinuteRule);
        String message = response.completedToday() ? "Alışkanlık tamamlandı (+XP)!" : "Alışkanlık tamamlama geri alındı.";
        String traceId = MDC.get("traceId");
        return ApiResponse.success(response, message, traceId);
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Alışkanlığı Sil", description = "Alışkanlığı mantıksal olarak (soft-delete) siler.")
    public ApiResponse<Void> deleteHabit(Authentication authentication, @PathVariable UUID publicId) {
        UUID userPublicId = resolveUserPublicId(authentication);
        habitService.deleteHabit(userPublicId, publicId);
        String traceId = MDC.get("traceId");
        return ApiResponse.success(null, "Alışkanlık başarıyla silindi.", traceId);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Kullanıcı Paneli Özetini Getir", description = "Bileşik büyüme zinciri, kimlikler, 4 yasa alışkanlıkları ve bugünün Kaizen skorunu tek seferde döner.")
    public ApiResponse<DashboardSummaryResponse> getDashboardSummary(Authentication authentication) {
        UUID userPublicId = resolveUserPublicId(authentication);
        DashboardSummaryResponse summary = habitService.getDashboardSummary(userPublicId);
        String traceId = MDC.get("traceId");
        return ApiResponse.success(summary, traceId);
    }

    @GetMapping("/identities")
    @Operation(summary = "Hedef Kimlikleri Listele", description = "James Clear felsefesine göre kullanıcının benimsediği hedef kimlikleri ve toplanan oyları listeler.")
    public ApiResponse<List<IdentityResponse>> getIdentities(Authentication authentication) {
        UUID userPublicId = resolveUserPublicId(authentication);
        List<IdentityResponse> identities = identityService.getIdentitiesForUser(userPublicId);
        String traceId = MDC.get("traceId");
        return ApiResponse.success(identities, traceId);
    }

    @PostMapping("/identities")
    @Operation(summary = "Yeni Hedef Kimlik Oluştur", description = "Kullanıcı için yeni bir davranışsal hedef kimlik tanımlar.")
    public ApiResponse<IdentityResponse> createIdentity(Authentication authentication,
                                                       @Valid @RequestBody CreateIdentityRequest request) {
        UUID userPublicId = resolveUserPublicId(authentication);
        IdentityResponse response = identityService.createIdentity(userPublicId, request);
        String traceId = MDC.get("traceId");
        return ApiResponse.success(response, "Hedef kimlik başarıyla oluşturuldu.", traceId);
    }

    @PostMapping("/identities/{publicId}/vote")
    @Operation(summary = "Hedef Kimliğe Oy Ver", description = "Kullanıcının eylemi sonucunda kimliğe +1 oy ekler.")
    public ApiResponse<IdentityResponse> castVote(Authentication authentication,
                                                  @PathVariable UUID publicId) {
        UUID userPublicId = resolveUserPublicId(authentication);
        IdentityResponse response = identityService.castVote(publicId, userPublicId);
        String traceId = MDC.get("traceId");
        return ApiResponse.success(response, "Kimliğe oy başarıyla verildi.", traceId);
    }

    @PostMapping("/reflections")
    @Operation(summary = "Günün Kaizen Retrospektifini Kaydet", description = "PDCA döngüsüne uygun olarak günün %1 gelişim değerlendirmesini ve Muda israf analizini kaydeder.")
    public ApiResponse<KaizenReflectionResponse> saveReflection(Authentication authentication,
                                                               @RequestBody KaizenReflectionRequest request) {
        UUID userPublicId = resolveUserPublicId(authentication);
        KaizenReflectionResponse response = habitService.saveDailyReflection(userPublicId, request);
        String traceId = MDC.get("traceId");
        return ApiResponse.success(response, "Günün Kaizen retrospektifi kaydedildi.", traceId);
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

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String userIdAttr = (String) oauth2User.getAttributes().get("user_id");
            if (userIdAttr != null) {
                return UUID.fromString(userIdAttr);
            }
            String email = (String) oauth2User.getAttributes().get("email");
            if (email != null && !email.isBlank()) {
                return userService.getProfileByEmail(email).publicId();
            }
        }

        return userService.getProfileByEmail(authentication.getName()).publicId();
    }
}
