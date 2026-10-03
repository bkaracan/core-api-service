package com.enterprise.coreapi.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Spring Boot backend (8080) üzerinde /login adresine yönlendirilen veya doğrudan erişilen
 * istekleri Angular SPA (4200) frontend login sayfasına yönlendirir.
 * Bu sayede "No static resource login." 404 hatasının önüne geçilir.
 */
@Controller
public class LoginRedirectController {

    @GetMapping("/login")
    public String redirectToFrontend(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String target = "redirect:http://localhost:4200/auth/login";
        return (queryString != null && !queryString.isBlank()) ? target + "?" + queryString : target;
    }
}
