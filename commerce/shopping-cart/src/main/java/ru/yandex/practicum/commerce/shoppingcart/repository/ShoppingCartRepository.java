package ru.yandex.practicum.commerce.shoppingcart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.commerce.shoppingcart.model.ShoppingCartEntity;
import ru.yandex.practicum.commerce.shoppingcart.model.ShoppingCartState;

import java.util.Optional;
import java.util.UUID;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCartEntity, UUID> {

    Optional<ShoppingCartEntity> findByUsernameAndState(String username, ShoppingCartState state);
}

