package com.restaurant.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.restaurant.config.VnpayProperties;
import com.restaurant.exception.ApiException;
import com.restaurant.models.entity.Order;
import com.restaurant.models.entity.Payment;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VnpayService {
    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties properties;

    public String createPaymentUrl(Order order, Payment payment, String clientIp) {
        validateConfig();

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiredAt = createdAt.plusMinutes(properties.getExpireMinutes());
        String transactionRef = payment.getTransactionRef() != null
                ? payment.getTransactionRef()
                : "ORDER" + order.getId() + "-" + System.currentTimeMillis();

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", properties.getVersion());
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put("vnp_Amount", toVnpayAmount(order.getTotalPrice()));
        params.put("vnp_CurrCode", properties.getCurrCode());
        params.put("vnp_TxnRef", transactionRef);
        params.put("vnp_OrderInfo", "Thanh toan order " + order.getId());
        params.put("vnp_OrderType", "billpayment");
        params.put("vnp_Locale", properties.getLocale());
        params.put("vnp_ReturnUrl", properties.getReturnUrl());
        params.put("vnp_IpAddr", clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", createdAt.format(VNPAY_TIME));
        params.put("vnp_ExpireDate", expiredAt.format(VNPAY_TIME));

        String hashData = buildHashData(params);
        String secureHash = hmacSha512(properties.getHashSecret(), hashData);
        String paymentUrl = properties.getPayUrl() + "?" + buildQuery(params) + "&vnp_SecureHash=" + secureHash;

        payment.setTransactionRef(transactionRef);
        payment.setPayUrl(paymentUrl);
        payment.setExpiredAt(expiredAt);
        return paymentUrl;
    }

    public boolean isValidCallback(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }

        Map<String, String> data = new TreeMap<>();
        params.forEach((key, value) -> {
            if (key != null && key.startsWith("vnp_")
                    && !"vnp_SecureHash".equals(key)
                    && !"vnp_SecureHashType".equals(key)
                    && value != null) {
                data.put(key, value);
            }
        });

        String expected = hmacSha512(properties.getHashSecret(), buildHashData(data));
        return expected.equalsIgnoreCase(secureHash);
    }

    public Map<String, String> toStringMap(Map<String, String[]> rawParams) {
        Map<String, String> params = new LinkedHashMap<>();
        rawParams.forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }

    public BigDecimal fromVnpayAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(amount).divide(BigDecimal.valueOf(100));
    }

    private void validateConfig() {
        if (isBlank(properties.getTmnCode()) || isBlank(properties.getHashSecret())
                || isBlank(properties.getReturnUrl())) {
            throw new ApiException("VNPAY_CONFIG_MISSING", HttpStatus.INTERNAL_SERVER_ERROR,
                    "Missing VNPAY config");
        }
    }

    private String toVnpayAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).toBigInteger().toString();
    }

    private String buildHashData(Map<String, String> params) {
        StringBuilder hashData = new StringBuilder();
        params.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                if (hashData.length() > 0) {
                    hashData.append('&');
                }
                hashData.append(urlEncode(key)).append('=').append(urlEncode(value));
            }
        });
        return hashData.toString();
    }

    private String buildQuery(Map<String, String> params) {
        return buildHashData(params);
    }

    private String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new ApiException("VNPAY_SIGN_FAILED", HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot sign VNPAY request");
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
