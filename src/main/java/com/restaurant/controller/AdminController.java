package com.restaurant.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import com.restaurant.dto.response.MenuItemDTO;
import com.restaurant.dto.response.CategoryDTO;
import com.restaurant.dto.request.CategoryRequest;
import com.restaurant.dto.request.CreateUserRequest;
import com.restaurant.dto.request.MenuItemRequest;
import com.restaurant.dto.request.UpdateUserRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    // CRUD USER
    private final AdminService adminService;

    @GetMapping("/users")
    public List<AdminUserDetailDTO> viewAllUsers(
            @RequestParam(required = false) String keyword) {
        if (keyword == null) {
            return adminService.viewAllUsers();
        }
        return adminService.viewByKeyWord(keyword);
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

    // MENUITEM
    @GetMapping("/menu-items")
    public List<MenuItemDTO> viewAllMenuItem() {
        return adminService.viewAllMenuItem();
    }

    @GetMapping("/menu-items/{itemId}")
    public MenuItemDTO viewMenuItemById(@PathVariable Long itemId) {
        return adminService.viewMenuItemById(itemId);
    }

    @PostMapping("/create-menu-item")
    public MenuItemDTO createMenuItem(@Valid @RequestBody MenuItemRequest request) {
        return adminService.createMenuItem(request);
    }

    @PutMapping("update-menu-item/{itemId}")
    public MenuItemDTO updateMenuItem(@PathVariable Long itemId, @Valid @RequestBody MenuItemRequest request) {
        return adminService.updateMenuItem(itemId, request);
    }

    @DeleteMapping("delete-menu-item/{itemId}")
    public void deleteMenuItem(@PathVariable Long itemId) {
        adminService.deleteMenuItem(itemId);
    }

    // CATEGORY
    @GetMapping("/categories")
    public List<CategoryDTO> viewAllCategories() {
        return adminService.viewAllCategories();
    }

    @PostMapping("/create-category")
    public CategoryDTO createCategory(@Valid @RequestBody CategoryRequest request) {
        return adminService.createCategory(request);
    }

    @PutMapping("update-category/{categoryId}")
    public CategoryDTO updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        return adminService.updateCategory(categoryId, request);
    }

    @DeleteMapping("delete-category/{categoryId}")
    public void deleteCategory(@PathVariable Long categoryId) {
        adminService.deleteCategory(categoryId);
    }
}
