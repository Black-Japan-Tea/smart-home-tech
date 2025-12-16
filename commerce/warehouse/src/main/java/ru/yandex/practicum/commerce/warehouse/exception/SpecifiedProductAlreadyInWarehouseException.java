package ru.yandex.practicum.commerce.warehouse.exception;

public class SpecifiedProductAlreadyInWarehouseException extends RuntimeException {

    public SpecifiedProductAlreadyInWarehouseException() {
        super("Указанный товар уже зарегистрирован на складе");
    }
}

