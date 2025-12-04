package ru.yandex.practicum.commerce.shoppingcart.exception;

import org.springframework.http.HttpStatus;

public record ApiError(String message, String userMessage, HttpStatus httpStatus) {
}

