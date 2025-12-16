package ru.yandex.practicum.commerce.shoppingcart.exception;

public class WarehouseUnavailableException extends RuntimeException {

    public WarehouseUnavailableException(String message) {
        super(message);
    }
}

