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
public class AdminUserDetailDTO {
    private Long userId;
    private String name;
    private String avatarUrl;
    private String email;
    private String role;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean active;
}
