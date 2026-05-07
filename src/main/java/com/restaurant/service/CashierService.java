package com.restaurant.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.restaurant.dto.response.CashierViewDTO;
import com.restaurant.dto.response.CategoryDTO;
import com.restaurant.dto.response.MenuItemDTO;
import com.restaurant.dto.response.OrderDTO;
import com.restaurant.exception.ApiException;
import com.restaurant.models.entity.Category;
import com.restaurant.models.entity.MenuItem;
import com.restaurant.repository.CategoryRepository;
import com.restaurant.repository.MenuItemRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashierService {
    private final CategoryRepository categoryRepo;
    private final MenuItemRepository menuItemRepo;
    private final TableService tableService;

    @Transactional
    public CashierViewDTO getCashierView(String keyword) {
        return CashierViewDTO.builder()
                .categories(getCategories())
                .menuItems(getMenuItems(null, keyword))
                .tables(tableService.getAllTables())
                .orders(getOpenOrders())
                .build();
    }

    public List<OrderDTO> getOpenOrders() {
        return List.of();
    }

    @Transactional
    public List<CategoryDTO> getCategories() {
        return categoryRepo.findAll()
                .stream()
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toCategoryDTO)
                .toList();
    }

    @Transactional
    public List<MenuItemDTO> getMenuItems(Long categoryId, String keyword) {
        String normalizedKeyword = keyword == null ? null : keyword.trim().toLowerCase();

        return menuItemRepo.findAll()
                .stream()
                .filter(item -> categoryId == null
                        || (item.getCategory() != null && categoryId.equals(item.getCategory().getId())))
                .filter(item -> normalizedKeyword == null || normalizedKeyword.isBlank()
                        || item.getName().toLowerCase().contains(normalizedKeyword))
                .sorted(Comparator.comparing(MenuItem::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toMenuItemDTO)
                .toList();
    }

    @Transactional
    public MenuItemDTO getMenuItem(Long menuItemId) {
        MenuItem menuItem = menuItemRepo.findById(menuItemId)
                .orElseThrow(() -> new ApiException("MENU_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Khong tim thay Menu Item"));
        return toMenuItemDTO(menuItem);
    }

    private CategoryDTO toCategoryDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .menuItemIds(category.getMenuItems()
                        .stream()
                        .map(MenuItem::getId)
                        .toList())
                .build();
    }

    private MenuItemDTO toMenuItemDTO(MenuItem menuItem) {
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
}
