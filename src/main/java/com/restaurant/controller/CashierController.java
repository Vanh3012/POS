package com.restaurant.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.dto.response.CashierViewDTO;
import com.restaurant.dto.response.CategoryDTO;
import com.restaurant.dto.response.MenuItemDTO;
import com.restaurant.service.CashierService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cashier")
@RequiredArgsConstructor
public class CashierController {
    private final CashierService cashierService;

    @GetMapping
    public CashierViewDTO getCashierView(@RequestParam(required = false) String keyword) {
        return cashierService.getCashierView(keyword);
    }

    @GetMapping("/categories")
    public List<CategoryDTO> getCategories() {
        return cashierService.getCategories();
    }

    @GetMapping("/menu-items")
    public List<MenuItemDTO> getMenuItems(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return cashierService.getMenuItems(categoryId, keyword);
    }

    @GetMapping("/menu-items/{menuItemId}")
    public MenuItemDTO getMenuItem(@PathVariable Long menuItemId) {
        return cashierService.getMenuItem(menuItemId);
    }
}
