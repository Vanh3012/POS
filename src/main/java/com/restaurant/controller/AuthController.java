package com.restaurant.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import com.restaurant.dto.response.UserDTO;
import com.restaurant.dto.request.ForgotPasswordRequest;
import com.restaurant.dto.request.LoginRequest;
import com.restaurant.dto.request.ResetPasswordRequest;
import com.restaurant.dto.request.VerifyOtpRequest;
import com.restaurant.dto.response.LoginResponse;
import com.restaurant.service.AuthService;
import jakarta.validation.Valid;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @GetMapping("/login")
    public List<LoginResponse> getAllUserActive() {
        return authService.getAllUserActive();
    }

    @PostMapping("/login")
    public UserDTO Login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
    }

    @PostMapping("/verify-otp")
    public void verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }
}
