package ru.yandex.practicum.commerce.shoppingcart.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.commerce.interaction.api.api.ShoppingCartApi;
import ru.yandex.practicum.commerce.interaction.api.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.commerce.interaction.api.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.shoppingcart.service.ShoppingCartService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/shopping-cart")
@RequiredArgsConstructor
public class ShoppingCartController implements ShoppingCartApi {

    private final ShoppingCartService shoppingCartService;

    @Override
    @GetMapping
    public ShoppingCartDto getShoppingCart(@RequestParam("username") @NotBlank String username) {
        return shoppingCartService.getShoppingCart(username);
    }

    @Override
    @PutMapping
    public ShoppingCartDto addProductToShoppingCart(@RequestParam("username") @NotBlank String username,
                                                    @RequestBody @NotEmpty Map<UUID, Long> products) {
        return shoppingCartService.addProductToShoppingCart(username, products);
    }

    @Override
    @DeleteMapping
    public void deactivateCurrentShoppingCart(@RequestParam("username") @NotBlank String username) {
        shoppingCartService.deactivateCurrentShoppingCart(username);
    }

    @Override
    @PostMapping("/remove")
    public ShoppingCartDto removeFromShoppingCart(@RequestParam("username") @NotBlank String username,
                                                  @RequestBody List<UUID> productIds) {
        log.info("removeFromShoppingCart called: username={}, productIds={}", username, productIds);
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Список товаров для удаления не может быть пустым");
        }
        ShoppingCartDto result = shoppingCartService.removeFromShoppingCart(username, productIds);
        log.info("removeFromShoppingCart result: {}", result);
        return result;
    }

    @Override
    @PostMapping("/change-quantity")
    public ShoppingCartDto changeProductQuantity(@RequestParam("username") @NotBlank String username,
                                                 @Valid @RequestBody ChangeProductQuantityRequest request) {
        log.info("changeProductQuantity called: username={}, request={}", username, request);
        ShoppingCartDto result = shoppingCartService.changeProductQuantity(username, request);
        log.info("changeProductQuantity result: {}", result);
        return result;
    }
}

