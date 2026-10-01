package com.enterprise.coreapi.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetPasswordRequest(
        @NotBlank(message = "Yeni şifre boş bırakılamaz")
        @Size(min = 8, max = 100, message = "Şifre en az 8 karakter olmalıdır")
        String newPassword,

        @NotBlank(message = "Şifre tekrarı boş bırakılamaz")
        String confirmPassword
) {}
