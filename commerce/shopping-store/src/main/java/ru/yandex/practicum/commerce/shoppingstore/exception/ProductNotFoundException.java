package ru.yandex.practicum.commerce.shoppingstore.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID productId) {
        super("Product with id %s not found".formatted(productId));
    }
}

