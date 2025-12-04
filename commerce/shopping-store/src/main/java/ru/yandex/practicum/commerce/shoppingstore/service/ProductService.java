package ru.yandex.practicum.commerce.shoppingstore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.interaction.api.dto.ProductCategory;
import ru.yandex.practicum.commerce.interaction.api.dto.ProductDto;
import ru.yandex.practicum.commerce.interaction.api.dto.ProductState;
import ru.yandex.practicum.commerce.interaction.api.dto.SetProductQuantityStateRequest;
import ru.yandex.practicum.commerce.shoppingstore.exception.ProductNotFoundException;
import ru.yandex.practicum.commerce.shoppingstore.model.ProductEntity;
import ru.yandex.practicum.commerce.shoppingstore.repository.ProductRepository;
import ru.yandex.practicum.commerce.shoppingstore.service.mapper.ProductMapper;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public List<ProductDto> getProducts(ProductCategory category, int page, int size, List<String> sort) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), calculateSize(size), parseSort(sort));
        return productRepository.findAllByProductCategoryAndProductState(category, ProductState.ACTIVE, pageable)
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    public ProductDto getProduct(UUID productId) {
        return productRepository.findById(productId)
                .map(productMapper::toDto)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Transactional
    public ProductDto createProduct(ProductDto productDto) {
        if (productDto.getProductId() != null) {
            throw new IllegalArgumentException("Product identifier must be empty for new products");
        }

        ProductEntity entity = productMapper.toEntity(productDto);
        return productMapper.toDto(productRepository.save(entity));
    }

    @Transactional
    public ProductDto updateProduct(ProductDto productDto) {
        if (productDto.getProductId() == null) {
            throw new IllegalArgumentException("Product identifier must be provided for update");
        }

        ProductEntity entity = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(productDto.getProductId()));

        productMapper.updateEntity(entity, productDto);
        return productMapper.toDto(entity);
    }

    @Transactional
    public boolean removeProduct(UUID productId) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        entity.setProductState(ProductState.DEACTIVATE);
        return true;
    }

    @Transactional
    public boolean setProductQuantityState(SetProductQuantityStateRequest request) {
        ProductEntity entity = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(request.getProductId()));
        entity.setQuantityState(request.getQuantityState());
        return true;
    }

    private int calculateSize(int size) {
        return size > 0 ? Math.min(size, 100) : DEFAULT_PAGE_SIZE;
    }

    private Sort parseSort(List<String> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            return Sort.unsorted();
        }

        return sortParams.stream()
                .map(this::createOrder)
                .reduce(Sort::and)
                .orElse(Sort.unsorted());
    }

    private Sort createOrder(String parameter) {
        if (parameter == null || parameter.isBlank()) {
            return Sort.unsorted();
        }

        String[] parts = parameter.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }
}

