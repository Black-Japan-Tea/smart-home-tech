package ru.yandex.practicum.commerce.shoppingstore.service.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.commerce.interaction.api.dto.ProductDto;
import ru.yandex.practicum.commerce.interaction.api.dto.ProductState;
import ru.yandex.practicum.commerce.shoppingstore.model.ProductEntity;

@Component
public class ProductMapper {

    public ProductDto toDto(ProductEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProductDto.builder()
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .description(entity.getDescription())
                .imageSrc(entity.getImageSrc())
                .quantityState(entity.getQuantityState())
                .productState(entity.getProductState())
                .productCategory(entity.getProductCategory())
                .price(entity.getPrice())
                .build();
    }

    public ProductEntity toEntity(ProductDto dto) {
        ProductEntity entity = new ProductEntity();
        entity.setProductName(dto.getProductName());
        entity.setDescription(dto.getDescription());
        entity.setImageSrc(dto.getImageSrc());
        entity.setQuantityState(dto.getQuantityState());
        entity.setProductState(dto.getProductState() == null ? ProductState.ACTIVE : dto.getProductState());
        entity.setProductCategory(dto.getProductCategory());
        entity.setPrice(dto.getPrice());
        return entity;
    }

    public void updateEntity(ProductEntity entity, ProductDto dto) {
        entity.setProductName(dto.getProductName());
        entity.setDescription(dto.getDescription());
        entity.setImageSrc(dto.getImageSrc());
        entity.setQuantityState(dto.getQuantityState());
        entity.setProductState(dto.getProductState());
        entity.setProductCategory(dto.getProductCategory());
        entity.setPrice(dto.getPrice());
    }
}

