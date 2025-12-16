package ru.yandex.practicum.commerce.interaction.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddProductToWarehouseRequest {

    @NotNull
    private UUID productId;

    @Min(1)
    private long quantity;
}

