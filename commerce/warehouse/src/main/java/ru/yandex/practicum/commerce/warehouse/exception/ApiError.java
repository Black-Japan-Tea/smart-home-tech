package ru.yandex.practicum.commerce.warehouse.exception;

import org.springframework.http.HttpStatus;

public record ApiError(String message, String userMessage, HttpStatus httpStatus) {
}

