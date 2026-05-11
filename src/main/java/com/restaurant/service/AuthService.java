package com.restaurant.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.restaurant.dto.request.ForgotPasswordRequest;
import com.restaurant.dto.request.LoginRequest;
import com.restaurant.dto.request.ResetPasswordRequest;
import com.restaurant.dto.request.VerifyOtpRequest;
import com.restaurant.dto.response.LoginResponse;
import com.restaurant.dto.response.UserDTO;
import com.restaurant.exception.ApiException;
import com.restaurant.models.entity.User;
import com.restaurant.repository.UserRepository;
import com.restaurant.security.JwtService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EmailService emailService;

    public List<LoginResponse> getAllUserActive() {
        return userRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toLoginResponse)
                .toList();
    }

    @Transactional
    public UserDTO login(LoginRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Khong tim thay User"));

        if (!user.getPassword().equals(request.getPin())) {
            throw new ApiException("PIN_INCORECT", HttpStatus.UNAUTHORIZED, "Sai PIN");
        }

        return toUserDTO(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy User với email này"));

        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtp(otp);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        emailService.sendOtpEmail(user.getEmail(), otp);
    }

    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy User với email này"));

        if (user.getOtp() == null || !user.getOtp().equals(request.getOtp())) {
            throw new ApiException("INVALID_OTP", HttpStatus.BAD_REQUEST, "Mã OTP không hợp lệ");
        }

        if (user.getOtpExpiryTime() == null || user.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
            throw new ApiException("EXPIRED_OTP", HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn");
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        verifyOtp(new VerifyOtpRequest(request.getEmail(), request.getOtp()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy User với email này"));

        user.setPassword(request.getNewPassword());
        user.setOtp(null);
        user.setOtpExpiryTime(null);
        userRepository.save(user);
    }

    private LoginResponse toLoginResponse(User user) {
        return LoginResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .startTime(user.getStartTime())
                .endTime(user.getEndTime())
                .build();
    }

    private UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .email(user.getEmail())
                .role(user.getRole().name())
                .startTime(user.getStartTime())
                .endTime(user.getEndTime())
                .token(jwtService.generationToken(user.getId()))
                .build();
    }
}
