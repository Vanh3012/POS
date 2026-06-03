package com.restaurant.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.dto.request.CreateOrderRequest;
import com.restaurant.dto.response.OrderDTO;
import com.restaurant.dto.response.OrderPageDTO;
import com.restaurant.service.OrderHistoryReportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {
    private final OrderHistoryReportService service;

    @GetMapping("/orders")
    public OrderPageDTO getOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.getHistory(date, month, year, startDate, endDate, search, page, size);
    }

    @GetMapping("/orders/{orderId}")
    public OrderDTO getOrder(@PathVariable Long orderId) {
        return service.getOrderDetail(orderId);
    }

    @PutMapping("/orders/{orderId}")
    public OrderDTO updateOrder(@PathVariable Long orderId, @Valid @RequestBody CreateOrderRequest request) {
        return service.updateOrder(orderId, request);
    }

    @DeleteMapping("/orders/{orderId}")
    public void deleteOrder(@PathVariable Long orderId) {
        service.deleteOrder(orderId);
    }
}
