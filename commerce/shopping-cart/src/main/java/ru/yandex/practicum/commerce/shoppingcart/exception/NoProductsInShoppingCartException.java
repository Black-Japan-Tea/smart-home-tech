package ru.yandex.practicum.commerce.shoppingcart.exception;

public class NoProductsInShoppingCartException extends RuntimeException {

    public NoProductsInShoppingCartException() {
        super("Нет искомых товаров в корзине");
    }
}

