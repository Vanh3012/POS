package com.restaurant.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.restaurant.dto.request.CreateOrderRequest;
import com.restaurant.dto.response.CashierViewDTO;
import com.restaurant.dto.response.CategoryDTO;
import com.restaurant.dto.response.MenuItemDTO;
import com.restaurant.dto.response.OrderDTO;
import com.restaurant.service.CashierService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

    @GetMapping("/orders")
    public List<OrderDTO> getOrders(){
        return cashierService.getOrders();
    }
    
    @PostMapping("/orders")
    public OrderDTO createOrder(@Valid @RequestBody CreateOrderRequest request, HttpServletRequest httpRequest) {
        return cashierService.createOrder(request, clientIp(httpRequest));
    }

    @PatchMapping("/orders/{orderId}/close")
    public OrderDTO closeOrder(@PathVariable Long orderId) {
        return cashierService.closeOrder(orderId);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
