package com.restaurant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vnpay")
public class VnpayProperties {
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String tmnCode;
    private String hashSecret;
    private String returnUrl;
    private String ipnUrl;
    private String version = "2.1.0";
    private String currCode = "VND";
    private String locale = "vn";
    private int expireMinutes = 15;
}
