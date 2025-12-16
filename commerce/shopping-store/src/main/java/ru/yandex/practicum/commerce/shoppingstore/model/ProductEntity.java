package ru.yandex.practicum.commerce.shoppingstore.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ru.yandex.practicum.commerce.interaction.api.dto.ProductCategory;
import ru.yandex.practicum.commerce.interaction.api.dto.ProductState;
import ru.yandex.practicum.commerce.interaction.api.dto.QuantityState;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "store_products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "description", nullable = false, length = 2048)
    private String description;

    @Column(name = "image_src", length = 1024)
    private String imageSrc;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_state", nullable = false, length = 32)
    private QuantityState quantityState;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_state", nullable = false, length = 32)
    private ProductState productState;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_category", nullable = false, length = 32)
    private ProductCategory productCategory;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
}

