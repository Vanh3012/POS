package com.restaurant.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.restaurant.repository.AdminRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import com.restaurant.dto.response.AdminUserDetailDTO;
import com.restaurant.dto.response.CategoryDTO;
import com.restaurant.models.entity.User;
import com.restaurant.dto.request.CreateUserRequest;
import com.restaurant.dto.request.UpdateUserRequest;
import com.restaurant.exception.ApiException;
import com.restaurant.models.enums.UserRole;
import com.restaurant.repository.CategoryRepository;
import com.restaurant.models.entity.Category;
import com.restaurant.repository.MenuItemRepository;
import com.restaurant.models.entity.MenuItem;
import com.restaurant.dto.request.MenuItemRequest;
import com.restaurant.dto.response.MenuItemDTO;
import com.restaurant.dto.request.CategoryRequest;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    // CRUD USER
    // --------------------------------------------------------------------------
    private final AdminRepository adminRepo;

    public List<AdminUserDetailDTO> viewAllUsers() {
        return adminRepo.findAll()
                .stream()
                .map(this::toUserDetailDTO)
                .toList();
    }

    public List<AdminUserDetailDTO> viewByKeyWord(String keyword) {
        return adminRepo.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword)
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

    @Transactional
    public void deleteUser(Long userId) {
        User user = adminRepo.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Khong tim thay User"));
        user.setActive(false);
        adminRepo.save(user);
    }

    @Transactional
    public AdminUserDetailDTO createUser(CreateUserRequest request) {
        if (adminRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiException("EMAIL_ALREADY_EXISTS", HttpStatus.BAD_REQUEST, "Email da ton tai");
        }

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

    // Catagory--------------------------------------------------------------------------------------------------
    private final CategoryRepository cateRepo;

    @Transactional
    public List<CategoryDTO> viewAllCategories() {
        return cateRepo.findAll()
                .stream()
                .map(this::toCategoryDTO)
                .toList();
    }

    @Transactional
    public CategoryDTO viewCategoryById(Long categoryId) {
        Category category = cateRepo.findById(categoryId)
                .orElseThrow(
                        () -> new ApiException("CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND, "Khong tim thay Category"));
        return toCategoryDTO(category);
    }

    public CategoryDTO toCategoryDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .menuItemIds(category.getMenuItems()
                        .stream()
                        .map(MenuItem::getId)
                        .toList())
                .build();
    }

    @Transactional
    public CategoryDTO createCategory(CategoryRequest request) {
        if (cateRepo.findByName(request.getName()).isPresent()) {
            throw new ApiException("CATEGORY_ALREADY_EXISTS", HttpStatus.BAD_REQUEST, "Category da ton tai");
        }
        Category category = new Category();

        category.setName(request.getName());

        if (request.getMenuItemIds() != null && !request.getMenuItemIds().isEmpty()) {

            List<MenuItem> items = menuItemRepo.findAllById(request.getMenuItemIds());

            for (MenuItem item : items) {
                item.setCategory(category);
            }

            category.setMenuItems(items);
        }
        return toCategoryDTO(cateRepo.save(category));
    }

    @Transactional
    public CategoryDTO updateCategory(Long categoryId, CategoryRequest request) {
        Category category = cateRepo.findById(categoryId)
                .orElseThrow(
                        () -> new ApiException("CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND, "Khong tim thay Category"));

        if (request.getName() != null)
            category.setName(request.getName());
        if (request.getMenuItemIds() != null && !request.getMenuItemIds().isEmpty()) {
            List<MenuItem> items = menuItemRepo.findAllById(request.getMenuItemIds());
            for (MenuItem item : items) {
                item.setCategory(category);
            }
            category.setMenuItems(items);
        }
        return toCategoryDTO(cateRepo.save(category));
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = cateRepo.findById(categoryId)
                .orElseThrow(
                        () -> new ApiException("CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND, "Khong tim thay Category"));
        cateRepo.delete(category);
    }

    // MENUITEM---------------------------------------------------------------------------------------------------------------------
    private final MenuItemRepository menuItemRepo;

    @Transactional
    public List<MenuItemDTO> viewAllMenuItem() {
        return menuItemRepo.findAll()
                .stream()
                .map(this::toMenuItemDTO)
                .toList();
    }

    public MenuItemDTO toMenuItemDTO(MenuItem menuItem) {
        return MenuItemDTO.builder()
                .id(menuItem.getId())
                .name(menuItem.getName())
                .price(menuItem.getPrice())
                .imageUrl(menuItem.getImageUrl())
                .description(menuItem.getDescription())
                .categoryId(menuItem.getCategory() != null ? menuItem.getCategory().getId() : null)
                .categoryName(menuItem.getCategory() != null ? menuItem.getCategory().getName() : null)
                .build();
    }

    @Transactional
    public MenuItemDTO viewMenuItemById(Long menuItemId) {
        MenuItem menuItem = menuItemRepo.findById(menuItemId)
                .orElseThrow(() -> new ApiException("MENU_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Khong tim thay Menu Item"));
        return toMenuItemDTO(menuItem);
    }

    @Transactional
    public MenuItemDTO createMenuItem(MenuItemRequest request) {
        if (menuItemRepo.findByName(request.getName()).isPresent()) {
            throw new ApiException("MENU_ITEM_ALREADY_EXISTS", HttpStatus.BAD_REQUEST, "Menu Item da ton tai");
        }
        MenuItem menuItem = MenuItem.builder()
                .name(request.getName())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .build();

        if (request.getCategoryId() != null) {
            Category category = cateRepo.findById(request.getCategoryId())
                    .orElseThrow(() -> new ApiException("CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND,
                            "Khong tim thay Category"));
            menuItem.setCategory(category);
        }

        MenuItem savedMenuItem = menuItemRepo.save(menuItem);
        return toMenuItemDTO(savedMenuItem);
    }

    @Transactional
    public MenuItemDTO updateMenuItem(Long menuItemId, MenuItemRequest request) {
        MenuItem menuItem = menuItemRepo.findById(menuItemId)
                .orElseThrow(() -> new ApiException("MENU_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Khong tim thay Menu Item"));

        if (request.getName() != null)
            menuItem.setName(request.getName());
        if (request.getPrice() != null)
            menuItem.setPrice(request.getPrice());
        if (request.getImageUrl() != null)
            menuItem.setImageUrl(request.getImageUrl());
        if (request.getDescription() != null)
            menuItem.setDescription(request.getDescription());
        if (request.getCategoryId() != null) {
            Category category = cateRepo.findById(request.getCategoryId())
                    .orElseThrow(() -> new ApiException("CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND,
                            "Khong tim thay Category"));
            menuItem.setCategory(category);
        }

        MenuItem savedMenuItem = menuItemRepo.save(menuItem);
        return toMenuItemDTO(savedMenuItem);
    }

    @Transactional
    public void deleteMenuItem(Long menuItemId) {
        MenuItem menuItem = menuItemRepo.findById(menuItemId)
                .orElseThrow(() -> new ApiException("MENU_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Khong tim thay Menu Item"));
        menuItemRepo.delete(menuItem);
    }
}
