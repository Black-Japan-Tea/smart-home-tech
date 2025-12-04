package ru.yandex.practicum.commerce.shoppingcart.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.yandex.practicum.commerce.interaction.api.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.commerce.interaction.api.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.shoppingcart.client.WarehouseClient;
import ru.yandex.practicum.commerce.shoppingcart.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.commerce.shoppingcart.exception.NotAuthorizedUserException;
import ru.yandex.practicum.commerce.shoppingcart.exception.WarehouseReservationException;
import ru.yandex.practicum.commerce.shoppingcart.exception.WarehouseUnavailableException;
import ru.yandex.practicum.commerce.shoppingcart.model.ShoppingCartEntity;
import ru.yandex.practicum.commerce.shoppingcart.model.ShoppingCartItemEntity;
import ru.yandex.practicum.commerce.shoppingcart.model.ShoppingCartState;
import ru.yandex.practicum.commerce.shoppingcart.repository.ShoppingCartRepository;
import ru.yandex.practicum.commerce.shoppingcart.service.mapper.ShoppingCartMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final ShoppingCartMapper shoppingCartMapper;
    private final WarehouseClient warehouseClient;

    public ShoppingCartDto getShoppingCart(String username) {
        ShoppingCartEntity cart = findOrCreateActiveCart(username);
        return shoppingCartMapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Long> products) {
        ShoppingCartEntity cart = findOrCreateActiveCart(username);
        products.forEach((productId, quantity) -> {
            validateQuantity(quantity);
            upsertItem(cart, productId, quantity);
        });

        validateWithWarehouse(cart);
        return shoppingCartMapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto removeFromShoppingCart(String username, List<UUID> productIds) {
        ShoppingCartEntity cart = findOrCreateActiveCart(username);
        boolean removed = productIds.stream()
                .map(productId -> removeItem(cart, productId))
                .reduce(Boolean::logicalOr)
                .orElse(false);

        if (!removed) {
            throw new NoProductsInShoppingCartException();
        }
        return shoppingCartMapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto changeProductQuantity(String username, ChangeProductQuantityRequest request) {
        ShoppingCartEntity cart = findOrCreateActiveCart(username);
        ShoppingCartItemEntity item = findItem(cart, request.getProductId())
                .orElseThrow(NoProductsInShoppingCartException::new);

        if (request.getNewQuantity() <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(request.getNewQuantity());
        }

        validateWithWarehouse(cart);
        return shoppingCartMapper.toDto(cart);
    }

    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        if (!StringUtils.hasText(username)) {
            throw new NotAuthorizedUserException();
        }
        shoppingCartRepository.findByUsernameAndState(username, ShoppingCartState.ACTIVE)
                .ifPresent(cart -> cart.setState(ShoppingCartState.DEACTIVATED));
    }

    private ShoppingCartEntity findOrCreateActiveCart(String username) {
        if (!StringUtils.hasText(username)) {
            throw new NotAuthorizedUserException();
        }

        return shoppingCartRepository.findByUsernameAndState(username, ShoppingCartState.ACTIVE)
                .orElseGet(() -> shoppingCartRepository.save(createCart(username)));
    }

    private ShoppingCartEntity createCart(String username) {
        ShoppingCartEntity cart = new ShoppingCartEntity();
        cart.setUsername(username);
        cart.setState(ShoppingCartState.ACTIVE);
        return cart;
    }

    private void upsertItem(ShoppingCartEntity cart, UUID productId, long quantity) {
        ShoppingCartItemEntity item = findItem(cart, productId).orElse(null);
        if (item == null) {
            ShoppingCartItemEntity newItem = new ShoppingCartItemEntity();
            newItem.setProductId(productId);
            newItem.setQuantity(quantity);
            cart.addItem(newItem);
        } else {
            item.setQuantity(item.getQuantity() + quantity);
        }
    }

    private boolean removeItem(ShoppingCartEntity cart, UUID productId) {
        return findItem(cart, productId)
                .map(item -> cart.getItems().remove(item))
                .orElse(false);
    }

    private java.util.Optional<ShoppingCartItemEntity> findItem(ShoppingCartEntity cart, UUID productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    private void validateQuantity(Long quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Количество товара должно быть положительным");
        }
    }

    private void validateWithWarehouse(ShoppingCartEntity cart) {
        ShoppingCartDto dto = shoppingCartMapper.toDto(cart);
        if (dto.getProducts().isEmpty()) {
            return;
        }
        try {
            warehouseClient.checkProductQuantityEnoughForShoppingCart(dto);
        } catch (FeignException.BadRequest ex) {
            throw new WarehouseReservationException("На складе недостаточно товара для выбранной корзины");
        } catch (FeignException ex) {
            throw new WarehouseUnavailableException("Сервис склада временно недоступен");
        }
    }
}

