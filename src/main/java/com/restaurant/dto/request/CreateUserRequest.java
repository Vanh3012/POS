package com.restaurant.dto.request;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateUserRequest {
    @NotBlank(message = "NAME_REQUIRED")
    private String name;

    private String avatarUrl;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    private String email;

    @NotBlank(message = "PASSWORD_REQUIRED")
    private String password;

    @NotBlank(message = "ROLE_REQUIRED")
    private String role;

    @NotNull(message = "START_TIME_REQUIRED")
    private LocalTime startTime;

    private LocalTime endTime;
    private Boolean active;
}
