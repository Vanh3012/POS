package com.restaurant.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.dto.response.OrderPrintDTO;
import com.restaurant.dto.response.PaymentStatusDTO;
import com.restaurant.dto.response.VnpayPaymentDTO;
import com.restaurant.service.PaymentService;
import com.restaurant.service.VnpayService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final VnpayService vnpayService;

    @PostMapping("/cashier/orders/{orderId}/payments/vnpay-qr")
    public VnpayPaymentDTO createVnpayQr(@PathVariable Long orderId, HttpServletRequest request) {
        return paymentService.createVnpayQr(orderId, clientIp(request));
    }

    @GetMapping("/cashier/orders/{orderId}/payment-status")
    public PaymentStatusDTO getPaymentStatus(@PathVariable Long orderId) {
        return paymentService.getPaymentStatus(orderId);
    }

    @GetMapping("/cashier/orders/{orderId}/print")
    public OrderPrintDTO getPrintData(@PathVariable Long orderId) {
        return paymentService.getPrintData(orderId);
    }

    @GetMapping("/payments/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(HttpServletRequest request) {
        return ResponseEntity.ok(paymentService.handleVnpayIpn(vnpayService.toStringMap(request.getParameterMap())));
    }

    @GetMapping("/payments/vnpay/return")
    public PaymentStatusDTO vnpayReturn(HttpServletRequest request) {
        return paymentService.handleVnpayReturn(vnpayService.toStringMap(request.getParameterMap()));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
