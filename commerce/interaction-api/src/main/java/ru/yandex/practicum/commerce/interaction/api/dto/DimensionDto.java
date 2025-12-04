package ru.yandex.practicum.commerce.interaction.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionDto {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal width;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal height;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal depth;
}

