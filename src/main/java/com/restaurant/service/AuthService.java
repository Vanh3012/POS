package com.restaurant.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.restaurant.dto.request.LoginRequest;
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
    private final PasswordEncoder passwordEncoder;

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
