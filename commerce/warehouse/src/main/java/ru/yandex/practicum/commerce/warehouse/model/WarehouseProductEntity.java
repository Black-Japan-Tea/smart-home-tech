package ru.yandex.practicum.commerce.warehouse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "warehouse_products")
public class WarehouseProductEntity {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "quantity", nullable = false)
    private long quantity;

    @Column(name = "weight", nullable = false, precision = 19, scale = 2)
    private BigDecimal weight;

    @Embedded
    private DimensionEmbeddable dimension;

    @Column(name = "fragile", nullable = false)
    private boolean fragile;
}

