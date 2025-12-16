package ru.yandex.practicum.commerce.shoppingcart.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.yandex.practicum.commerce.interaction.api.api.WarehouseApi;

@FeignClient(name = "warehouse",
        contextId = "shoppingCartWarehouseClient",
        path = ru.yandex.practicum.commerce.interaction.api.api.WarehouseApi.API_PATH)
public interface WarehouseClient extends WarehouseApi {
}

