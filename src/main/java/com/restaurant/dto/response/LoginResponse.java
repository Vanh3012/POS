package com.restaurant.dto.response;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// response hien thi dropdown danh sach nhan vien
public class LoginResponse {
    private Long userId;
    private String name;
    private String avatarUrl;
    private String role;
    private LocalTime startTime;
    private LocalTime endTime;
}
