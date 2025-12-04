package ru.yandex.practicum.commerce.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.commerce.warehouse.model.WarehouseProductEntity;

import java.util.UUID;

public interface WarehouseProductRepository extends JpaRepository<WarehouseProductEntity, UUID> {
}

