package ru.yandex.practicum.commerce.warehouse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.MathContext;

@Getter
@Setter
@Embeddable
public class DimensionEmbeddable {

    @Column(name = "width", nullable = false, precision = 19, scale = 2)
    private BigDecimal width;

    @Column(name = "height", nullable = false, precision = 19, scale = 2)
    private BigDecimal height;

    @Column(name = "depth", nullable = false, precision = 19, scale = 2)
    private BigDecimal depth;

    public BigDecimal volume() {
        return width.multiply(height, MathContext.DECIMAL64)
                .multiply(depth, MathContext.DECIMAL64);
    }
}

