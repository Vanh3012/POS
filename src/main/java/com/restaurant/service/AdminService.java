package com.restaurant.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.restaurant.repository.AdminRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import com.restaurant.dto.response.AdminUserDetailDTO;
import com.restaurant.models.entity.User;
import com.restaurant.dto.request.CreateUserRequest;
import com.restaurant.dto.request.UpdateUserRequest;
import com.restaurant.exception.ApiException;
import com.restaurant.models.enums.UserRole;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminRepository adminRepo;

    public List<AdminUserDetailDTO> viewAllUsers() {
        return adminRepo.findAllByOrderByRoleAsc()
                .stream()
                .map(this::toUserDetailDTO)
                .toList();
    }

    public AdminUserDetailDTO viewUserById(Long UserId) {
        User user = adminRepo.findById(UserId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Khong tim thay User"));
        return toUserDetailDTO(user);
    }

    @Transactional
    public AdminUserDetailDTO updateUser(Long userId, UpdateUserRequest request) {
        User user = adminRepo.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Khong tim thay User"));

        if (request.getName() != null)
            user.setName(request.getName());
        if (request.getAvatarUrl() != null)
            user.setAvatarUrl(request.getAvatarUrl());
        if (request.getEmail() != null)
            user.setEmail(request.getEmail());
        if (request.getPassword() != null)
            user.setPassword(request.getPassword());
        if (request.getRole() != null)
            user.setRole(toRole(request.getRole()));
        if (request.getStartTime() != null)
            user.setStartTime(request.getStartTime());
        if (request.getEndTime() != null)
            user.setEndTime(request.getEndTime());
        if (request.getActive() != null)
            user.setActive(request.getActive());

        User savedUser = adminRepo.save(user);
        return toUserDetailDTO(savedUser);
    }

    public UserRole toRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException("ROLE_INVALID", HttpStatus.BAD_REQUEST, "Role khong hop le");
        }
    }

    public void deleteUser(Long userId) {
        User user = adminRepo.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Khong tim thay User"));
        adminRepo.delete(user);
    }

    @Transactional
    public AdminUserDetailDTO createUser(CreateUserRequest request) {
        User user = User.builder()
                .name(request.getName())
                .avatarUrl(request.getAvatarUrl())
                .email(request.getEmail())
                .password(request.getPassword())
                .role(toRole(request.getRole()))
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        User savedUser = adminRepo.save(user);
        return toUserDetailDTO(savedUser);
    }

    public AdminUserDetailDTO toUserDetailDTO(User user) {
        return AdminUserDetailDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .email(user.getEmail())
                .role(user.getRole().name())
                .startTime(user.getStartTime())
                .endTime(user.getEndTime())
                .active(user.getActive())
                .build();
    }
}
