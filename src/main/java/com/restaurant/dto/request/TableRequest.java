package com.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableRequest {
    @NotBlank(message = "Table number is required")
    private String tableNumber;

    @NotNull(message = "Floor is required")
    @Min(value = 1, message = "Floor must be at least 1")
    private Integer floor;

    private Boolean isActive;

    public Boolean getIsActive() {
        return this.isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
