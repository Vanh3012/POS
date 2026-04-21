package com.restaurant.dto.response;

import lombok.*;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long userId;
    private String name;
    private String avatarUrl;
    private String email;
    private String role;
    private LocalTime startTime;
    private LocalTime endTime;
    private String token;
}
