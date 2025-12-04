package ru.yandex.practicum.commerce.shoppingcart.service.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.commerce.interaction.api.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.shoppingcart.model.ShoppingCartEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ShoppingCartMapper {

    public ShoppingCartDto toDto(ShoppingCartEntity entity) {
        if (entity == null) {
            return null;
        }
        Map<java.util.UUID, Long> products = entity.getItems().stream()
                .collect(Collectors.toMap(
                        item -> item.getProductId(),
                        item -> item.getQuantity(),
                        Long::sum,
                        LinkedHashMap::new
                ));

        return ShoppingCartDto.builder()
                .shoppingCartId(entity.getShoppingCartId())
                .products(products)
                .build();
    }
}

