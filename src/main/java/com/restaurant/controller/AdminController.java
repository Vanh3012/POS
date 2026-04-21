package com.restaurant.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.restaurant.service.AdminService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

import java.util.List;
import com.restaurant.dto.response.AdminUserDetailDTO;
import com.restaurant.dto.request.CreateUserRequest;
import com.restaurant.dto.request.UpdateUserRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/users")
    public List<AdminUserDetailDTO> viewAllUsers() {
        return adminService.viewAllUsers();
    }

    @GetMapping("/users/{userId}")
    public AdminUserDetailDTO viewUserById(@PathVariable Long userId) {
        return adminService.viewUserById(userId);
    }

    @PostMapping("/create-user")
    public AdminUserDetailDTO createUser(@Valid @RequestBody CreateUserRequest request) {
        return adminService.createUser(request);
    }

    @PutMapping("update-user/{userId}")
    public AdminUserDetailDTO updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
        return adminService.updateUser(userId, request);
    }

    @DeleteMapping("delete-user/{userId}")
    public void deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
    }
}
