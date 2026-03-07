package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class LoginController {

    private static final String DEFAULT_EMAIL = "admin@cyepro.com";
    private static final String DEFAULT_PASSWORD = "password123";

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (DEFAULT_EMAIL.equals(email) && DEFAULT_PASSWORD.equals(password)) {
            return ResponseEntity.ok(Map.of(
                    "token", "cyepro-session-" + System.currentTimeMillis(),
                    "user", Map.of(
                            "email", email,
                            "name", "Admin",
                            "role", "admin"
                    )
            ));
        }

        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }
}
