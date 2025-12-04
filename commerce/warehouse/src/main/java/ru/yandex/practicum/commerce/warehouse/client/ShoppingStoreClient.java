package ru.yandex.practicum.commerce.warehouse.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.yandex.practicum.commerce.interaction.api.api.ShoppingStoreApi;

@FeignClient(name = "shopping-store", contextId = "warehouseShoppingStoreClient")
public interface ShoppingStoreClient extends ShoppingStoreApi {
}

