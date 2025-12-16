package ru.yandex.practicum.commerce.warehouse.exception;

public class ProductInShoppingCartLowQuantityInWarehouseException extends RuntimeException {

    public ProductInShoppingCartLowQuantityInWarehouseException(String message) {
        super(message);
    }
}

