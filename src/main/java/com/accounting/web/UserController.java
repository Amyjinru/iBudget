package com.accounting.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @GetMapping("/me")
    public String me(Authentication auth) {
        return auth != null ? auth.getName() : "anonymous";
    }
}
