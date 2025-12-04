package ru.yandex.practicum.commerce.warehouse.exception;

public class NoSpecifiedProductInWarehouseException extends RuntimeException {

    public NoSpecifiedProductInWarehouseException() {
        super("Нет информации о товаре на складе");
    }
}

