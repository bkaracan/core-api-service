package com.enterprise.coreapi.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank(message = "E-posta adresi boş bırakılamaz")
        @Email(message = "Geçerli bir e-posta adresi giriniz")
        String email,

        @NotBlank(message = "Şifre boş bırakılamaz")
        @Size(min = 8, max = 100, message = "Şifre en az 8, en fazla 100 karakter olmalıdır")
        String password,

        @NotBlank(message = "İsim boş bırakılamaz")
        @Size(min = 2, max = 50, message = "İsim 2 ile 50 karakter arasında olmalıdır")
        String firstName,

        @NotBlank(message = "Soyisim boş bırakılamaz")
        @Size(min = 2, max = 50, message = "Soyisim 2 ile 50 karakter arasında olmalıdır")
        String lastName
) {}
