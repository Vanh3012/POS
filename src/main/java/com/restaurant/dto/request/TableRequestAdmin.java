package com.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.restaurant.models.enums.TableStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableRequestAdmin {
    @NotBlank(message = "Table number is required")
    private String tableNumber;

    @NotNull(message = "Floor is required")
    @Min(value = 1, message = "Floor must be at least 1")
    private Integer floor;

    @NotNull(message = "Status is required")
    private TableStatus status;

    @NotNull(message = "Active is required")
    private Boolean isActive;
}
