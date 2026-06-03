package com.restaurant.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.restaurant.dto.response.OrderItemDTO;
import com.restaurant.dto.response.OrderPrintDTO;
import com.restaurant.dto.response.PaymentStatusDTO;
import com.restaurant.dto.response.VnpayPaymentDTO;
import com.restaurant.exception.ApiException;
import com.restaurant.models.entity.Order;
import com.restaurant.models.entity.OrderItem;
import com.restaurant.models.entity.Payment;
import com.restaurant.models.enums.PaymentMethod;
import com.restaurant.models.enums.PaymentStatus;
import com.restaurant.repository.OrderRepository;
import com.restaurant.repository.PaymentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final String PROVIDER_VNPAY = "VNPAY";

    private final OrderRepository orderRepo;
    private final PaymentRepository paymentRepo;
    private final VnpayService vnpayService;

    @Transactional
    public VnpayPaymentDTO createVnpayQr(Long orderId, String clientIp) {
        Order order = findOrder(orderId);
        Payment payment = order.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
            order.setPayment(payment);
        }

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new ApiException("PAYMENT_ALREADY_COMPLETED", HttpStatus.BAD_REQUEST,
                    "Order already paid");
        }

        payment.setPaymentMethod(PaymentMethod.QRIS);
        payment.setProvider(PROVIDER_VNPAY);
        payment.setAmount(money(order.getTotalPrice()));
        payment.setReceived(BigDecimal.ZERO);
        payment.setChangeAmount(BigDecimal.ZERO);
        payment.setStatus(PaymentStatus.PENDING);

        String payUrl = vnpayService.createPaymentUrl(order, payment, clientIp);
        paymentRepo.save(payment);

        return toVnpayPaymentDTO(order, payment, payUrl);
    }

    @Transactional
    public PaymentStatusDTO getPaymentStatus(Long orderId) {
        Order order = findOrder(orderId);
        Payment payment = requirePayment(order);
        return toPaymentStatusDTO(order, payment);
    }

    @Transactional
    public OrderPrintDTO getPrintData(Long orderId) {
        Order order = findOrder(orderId);
        Payment payment = requirePayment(order);

        return OrderPrintDTO.builder()
                .orderId(order.getId())
                .paymentId(payment.getId())
                .orderCode(orderCode(order.getId()))
                .customerName(order.getCustomerName())
                .tableNumber(order.getTable() != null ? order.getTable().getTableNumber() : null)
                .orderType(order.getOrderType() != null ? order.getOrderType().name() : null)
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                .paymentStatus(payment.getStatus() != null ? payment.getStatus().name() : null)
                .transactionRef(payment.getTransactionRef())
                .paymentUrl(payment.getPayUrl())
                .qrContent(payment.getPayUrl())
                .createdAt(order.getCreatedAt())
                .subTotal(order.getSubTotal())
                .tax(order.getTax())
                .totalPrice(order.getTotalPrice())
                .items(order.getOrderItems().stream().map(this::toOrderItemDTO).toList())
                .build();
    }

    @Transactional
    public Map<String, String> handleVnpayIpn(Map<String, String> params) {
        Map<String, String> response = new LinkedHashMap<>();
        if (!vnpayService.isValidCallback(params)) {
            return ipnResponse("97", "Invalid checksum");
        }

        String txnRef = params.get("vnp_TxnRef");
        Payment payment = paymentRepo.findByTransactionRef(txnRef).orElse(null);
        if (payment == null) {
            return ipnResponse("01", "Order not found");
        }

        BigDecimal paidAmount = vnpayService.fromVnpayAmount(params.get("vnp_Amount"));
        if (money(payment.getAmount()).compareTo(money(paidAmount)) != 0) {
            return ipnResponse("04", "Invalid amount");
        }

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return ipnResponse("02", "Order already confirmed");
        }

        applyVnpayResult(payment, params);
        paymentRepo.save(payment);

        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return response;
    }

    @Transactional
    public PaymentStatusDTO handleVnpayReturn(Map<String, String> params) {
        if (!vnpayService.isValidCallback(params)) {
            throw new ApiException("VNPAY_INVALID_CHECKSUM", HttpStatus.BAD_REQUEST,
                    "Invalid VNPAY checksum");
        }

        String txnRef = params.get("vnp_TxnRef");
        Payment payment = paymentRepo.findByTransactionRef(txnRef)
                .orElseThrow(() -> new ApiException("PAYMENT_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Payment not found"));

        BigDecimal paidAmount = vnpayService.fromVnpayAmount(params.get("vnp_Amount"));
        if (money(payment.getAmount()).compareTo(money(paidAmount)) != 0) {
            throw new ApiException("VNPAY_INVALID_AMOUNT", HttpStatus.BAD_REQUEST,
                    "Invalid VNPAY amount");
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            applyVnpayResult(payment, params);
            paymentRepo.save(payment);
        }

        return toPaymentStatusDTO(payment.getOrder(), payment);
    }

    private void applyVnpayResult(Payment payment, Map<String, String> params) {
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

        payment.setProvider(PROVIDER_VNPAY);
        payment.setProviderTransactionNo(params.get("vnp_TransactionNo"));
        payment.setProviderResponseCode(responseCode);
        payment.setBankCode(params.get("vnp_BankCode"));
        payment.setStatus(success ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
        payment.setReceived(success ? money(payment.getAmount()) : BigDecimal.ZERO);
        payment.setChangeAmount(BigDecimal.ZERO);
        if (success) {
            payment.setPaidAt(java.time.LocalDateTime.now());
        }
    }

    private PaymentStatusDTO toPaymentStatusDTO(Order order, Payment payment) {
        return PaymentStatusDTO.builder()
                .orderId(order.getId())
                .paymentId(payment.getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                .paymentStatus(payment.getStatus() != null ? payment.getStatus().name() : null)
                .transactionRef(payment.getTransactionRef())
                .providerTransactionNo(payment.getProviderTransactionNo())
                .providerResponseCode(payment.getProviderResponseCode())
                .bankCode(payment.getBankCode())
                .paidAt(payment.getPaidAt())
                .build();
    }

    private VnpayPaymentDTO toVnpayPaymentDTO(Order order, Payment payment, String payUrl) {
        return VnpayPaymentDTO.builder()
                .orderId(order.getId())
                .paymentId(payment.getId())
                .orderCode(orderCode(order.getId()))
                .amount(payment.getAmount())
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .transactionRef(payment.getTransactionRef())
                .paymentUrl(payUrl)
                .qrContent(payUrl)
                .expiredAt(payment.getExpiredAt())
                .build();
    }

    private OrderItemDTO toOrderItemDTO(OrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .menuItemId(item.getMenuItem().getId())
                .menuItemName(item.getMenuItem().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(money(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
                .note(item.getNote())
                .build();
    }

    private Payment requirePayment(Order order) {
        if (order.getPayment() == null) {
            throw new ApiException("PAYMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Payment not found");
        }
        return order.getPayment();
    }

    private Order findOrder(Long orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Khong tim thay Order"));
    }

    private Map<String, String> ipnResponse(String code, String message) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("RspCode", code);
        response.put("Message", message);
        return response;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String orderCode(Long id) {
        return "#" + String.format("%04d", id);
    }
}
