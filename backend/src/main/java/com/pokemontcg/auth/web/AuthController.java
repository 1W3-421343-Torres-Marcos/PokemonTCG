package com.pokemontcg.auth.web;

import com.pokemontcg.auth.dto.AuthResponseDTO;
import com.pokemontcg.auth.dto.LoginRequest;
import com.pokemontcg.auth.dto.RegisterRequest;
import com.pokemontcg.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponseDTO login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
