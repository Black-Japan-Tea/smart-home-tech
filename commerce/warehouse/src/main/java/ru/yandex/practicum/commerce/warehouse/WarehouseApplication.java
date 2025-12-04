package ru.yandex.practicum.commerce.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.yandex.practicum.commerce.warehouse.client.ShoppingStoreClient;

@SpringBootApplication(scanBasePackages = "ru.yandex.practicum.commerce")
@EnableFeignClients(basePackageClasses = ShoppingStoreClient.class)
public class WarehouseApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarehouseApplication.class, args);
    }
}

