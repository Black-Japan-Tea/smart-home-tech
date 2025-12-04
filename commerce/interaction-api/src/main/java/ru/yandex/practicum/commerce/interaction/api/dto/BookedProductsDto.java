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
public class BookedProductsDto {

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal deliveryWeight;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal deliveryVolume;

    private boolean fragile;
}

