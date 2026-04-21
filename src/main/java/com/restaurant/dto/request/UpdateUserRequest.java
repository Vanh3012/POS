package com.restaurant.dto.request;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateUserRequest {
    private String name;
    private String avatarUrl;

    @Email(message = "EMAIL_INVALID")
    private String email;

    private String password;
    private String role;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean active;
}
