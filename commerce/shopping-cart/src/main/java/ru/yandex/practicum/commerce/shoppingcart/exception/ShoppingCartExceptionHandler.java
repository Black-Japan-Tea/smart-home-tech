package ru.yandex.practicum.commerce.shoppingcart.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ShoppingCartExceptionHandler {

    @ExceptionHandler(NotAuthorizedUserException.class)
    public ResponseEntity<ApiError> handleUnauthorized(NotAuthorizedUserException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(NoProductsInShoppingCartException.class)
    public ResponseEntity<ApiError> handleNoProducts(NoProductsInShoppingCartException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(WarehouseReservationException.class)
    public ResponseEntity<ApiError> handleReservation(WarehouseReservationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(WarehouseUnavailableException.class)
    public ResponseEntity<ApiError> handleWarehouseUnavailable(WarehouseUnavailableException ex) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "Невозможно прочитать тело запроса. Проверьте корректность JSON";
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ApiError> handleServletRequestBinding(ServletRequestBindingException ex) {
        String message = "Отсутствуют обязательные параметры запроса: " + ex.getMessage();
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            ConstraintViolationException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleInternalServerError(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage() != null ? ex.getMessage() : "Internal server error");
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message) {
        ApiError apiError = new ApiError(message != null ? message : "Unknown error", "Operation cannot be completed", status);
        return ResponseEntity.status(status).body(apiError);
    }
}

