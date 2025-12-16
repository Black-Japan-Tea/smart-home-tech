package ru.yandex.practicum.commerce.shoppingcart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.yandex.practicum.commerce.shoppingcart.client.WarehouseClient;

@SpringBootApplication(scanBasePackages = "ru.yandex.practicum.commerce")
@EnableFeignClients(basePackageClasses = WarehouseClient.class)
public class ShoppingCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoppingCartApplication.class, args);
    }
}

