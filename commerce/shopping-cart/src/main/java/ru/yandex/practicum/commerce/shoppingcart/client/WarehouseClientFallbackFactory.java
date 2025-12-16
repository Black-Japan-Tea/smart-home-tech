package ru.yandex.practicum.commerce.shoppingcart.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.commerce.interaction.api.dto.AddProductToWarehouseRequest;
import ru.yandex.practicum.commerce.interaction.api.dto.AddressDto;
import ru.yandex.practicum.commerce.interaction.api.dto.BookedProductsDto;
import ru.yandex.practicum.commerce.interaction.api.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.commerce.interaction.api.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.shoppingcart.exception.WarehouseUnavailableException;

@Component
public class WarehouseClientFallbackFactory implements FallbackFactory<WarehouseClient> {

    @Override
    public WarehouseClient create(Throwable cause) {
        return new WarehouseClient() {
            @Override
            public void newProductInWarehouse(NewProductInWarehouseRequest request) {
                throw buildException();
            }

            @Override
            public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto shoppingCartDto) {
                throw buildException();
            }

            @Override
            public void addProductToWarehouse(AddProductToWarehouseRequest request) {
                throw buildException();
            }

            @Override
            public AddressDto getWarehouseAddress() {
                throw buildException();
            }
        };
    }

    private WarehouseUnavailableException buildException() {
        return new WarehouseUnavailableException("Сервис склада временно недоступен. Попробуйте повторить запрос позже.");
    }
}
