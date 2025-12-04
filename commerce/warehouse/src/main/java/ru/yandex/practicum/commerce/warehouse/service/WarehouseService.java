package ru.yandex.practicum.commerce.warehouse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.interaction.api.dto.AddProductToWarehouseRequest;
import ru.yandex.practicum.commerce.interaction.api.dto.BookedProductsDto;
import ru.yandex.practicum.commerce.interaction.api.dto.DimensionDto;
import ru.yandex.practicum.commerce.interaction.api.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.commerce.interaction.api.dto.QuantityState;
import ru.yandex.practicum.commerce.interaction.api.dto.SetProductQuantityStateRequest;
import ru.yandex.practicum.commerce.interaction.api.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.warehouse.client.ShoppingStoreClient;
import ru.yandex.practicum.commerce.warehouse.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.commerce.warehouse.exception.ProductInShoppingCartLowQuantityInWarehouseException;
import ru.yandex.practicum.commerce.warehouse.exception.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.commerce.warehouse.model.DimensionEmbeddable;
import ru.yandex.practicum.commerce.warehouse.model.WarehouseProductEntity;
import ru.yandex.practicum.commerce.warehouse.repository.WarehouseProductRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseProductRepository warehouseProductRepository;
    private final ShoppingStoreClient shoppingStoreClient;
    private final WarehouseAddressSupplier warehouseAddressSupplier;

    @Transactional
    public void registerNewProduct(NewProductInWarehouseRequest request) {
        if (warehouseProductRepository.existsById(request.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseException();
        }

        WarehouseProductEntity entity = new WarehouseProductEntity();
        entity.setProductId(request.getProductId());
        entity.setQuantity(0);
        entity.setWeight(request.getWeight());
        entity.setFragile(request.isFragile());
        entity.setDimension(toEmbeddable(request.getDimension()));
        warehouseProductRepository.save(entity);
        notifyStore(entity);
    }

    @Transactional
    public void addProduct(AddProductToWarehouseRequest request) {
        WarehouseProductEntity entity = warehouseProductRepository.findById(request.getProductId())
                .orElseThrow(NoSpecifiedProductInWarehouseException::new);
        entity.setQuantity(entity.getQuantity() + request.getQuantity());
        notifyStore(entity);
    }

    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto shoppingCartDto) {
        if (shoppingCartDto == null || shoppingCartDto.getProducts().isEmpty()) {
            return BookedProductsDto.builder()
                    .deliveryVolume(BigDecimal.ZERO)
                    .deliveryWeight(BigDecimal.ZERO)
                    .fragile(false)
                    .build();
        }

        BigDecimal deliveryWeight = BigDecimal.ZERO;
        BigDecimal deliveryVolume = BigDecimal.ZERO;
        boolean fragile = false;

        for (Map.Entry<UUID, Long> entry : shoppingCartDto.getProducts().entrySet()) {
            WarehouseProductEntity product = warehouseProductRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ProductInShoppingCartLowQuantityInWarehouseException(
                            "Товар %s отсутствует на складе".formatted(entry.getKey())));
            long requestedQuantity = entry.getValue() == null ? 0 : entry.getValue();
            if (requestedQuantity <= 0 || product.getQuantity() < requestedQuantity) {
                throw new ProductInShoppingCartLowQuantityInWarehouseException(
                        "Товара %s недостаточно на складе".formatted(entry.getKey()));
            }

            BigDecimal requested = BigDecimal.valueOf(requestedQuantity);
            deliveryWeight = deliveryWeight.add(product.getWeight().multiply(requested));
            deliveryVolume = deliveryVolume.add(product.getDimension().volume().multiply(requested));
            fragile = fragile || product.isFragile();
        }

        return BookedProductsDto.builder()
                .deliveryWeight(deliveryWeight)
                .deliveryVolume(deliveryVolume)
                .fragile(fragile)
                .build();
    }

    public ru.yandex.practicum.commerce.interaction.api.dto.AddressDto getWarehouseAddress() {
        return warehouseAddressSupplier.currentAddress();
    }

    private DimensionEmbeddable toEmbeddable(DimensionDto dimension) {
        DimensionEmbeddable embeddable = new DimensionEmbeddable();
        embeddable.setWidth(dimension.getWidth());
        embeddable.setHeight(dimension.getHeight());
        embeddable.setDepth(dimension.getDepth());
        return embeddable;
    }

    private void notifyStore(WarehouseProductEntity entity) {
        try {
            shoppingStoreClient.setProductQuantityState(SetProductQuantityStateRequest.builder()
                    .productId(entity.getProductId())
                    .quantityState(resolveQuantityState(entity.getQuantity()))
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to synchronize quantity state with shopping-store for product {}", entity.getProductId(), ex);
        }
    }

    private QuantityState resolveQuantityState(long quantity) {
        if (quantity <= 0) {
            return QuantityState.ENDED;
        }
        if (quantity < 10) {
            return QuantityState.FEW;
        }
        if (quantity <= 100) {
            return QuantityState.ENOUGH;
        }
        return QuantityState.MANY;
    }
}

