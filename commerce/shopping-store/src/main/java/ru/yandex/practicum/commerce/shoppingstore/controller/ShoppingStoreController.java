package ru.yandex.practicum.commerce.shoppingstore.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.commerce.interaction.api.api.ShoppingStoreApi;
import ru.yandex.practicum.commerce.interaction.api.dto.PageResponse;
import ru.yandex.practicum.commerce.interaction.api.dto.ProductCategory;
import ru.yandex.practicum.commerce.interaction.api.dto.ProductDto;
import ru.yandex.practicum.commerce.interaction.api.dto.SetProductQuantityStateRequest;
import ru.yandex.practicum.commerce.shoppingstore.service.ProductService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@Validated
@RequestMapping(ShoppingStoreApi.API_PATH)
@RequiredArgsConstructor
public class ShoppingStoreController implements ShoppingStoreApi {

    private final ProductService productService;

    @Override
    @GetMapping
    public PageResponse<ProductDto> getProducts(@RequestParam("category") ProductCategory category,
                                                @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
                                                @RequestParam(value = "size", defaultValue = "20") @Min(1) int size,
                                                @RequestParam(value = "sort", required = false) List<String> sort) {
        log.info("getProducts called: category={}, page={}, size={}, sort={}", category, page, size, sort);
        PageResponse<ProductDto> response = PageResponse.from(productService.getProducts(category, page, size, sort));
        log.info("getProducts result: requestedSize={}, actualPageSize={}, totalElements={}",
                size, response.getSize(), response.getTotalElements());
        return response;
    }

    @Override
    @PutMapping
    public ProductDto createNewProduct(@Valid @RequestBody ProductDto productDto) {
        log.info("createNewProduct called: productName={}, category={}, productState={}", 
                productDto.getProductName(), productDto.getProductCategory(), productDto.getProductState());
        ProductDto result = productService.createProduct(productDto);
        log.info("createNewProduct result: productId={}, productName={}", 
                result.getProductId(), result.getProductName());
        return result;
    }

    @Override
    @PostMapping
    public ProductDto updateProduct(@Valid @RequestBody ProductDto productDto) {
        return productService.updateProduct(productDto);
    }

    @Override
    @PostMapping("/removeProductFromStore")
    public boolean removeProductFromStore(@RequestBody @NotNull UUID productId) {
        return productService.removeProduct(productId);
    }

    @Override
    @PostMapping("/quantityState")
    public boolean setProductQuantityState(@RequestParam(value = "productId", required = false) UUID productId,
                                          @RequestParam(value = "quantityState", required = false) ru.yandex.practicum.commerce.interaction.api.dto.QuantityState quantityState,
                                          @Valid @RequestBody(required = false) SetProductQuantityStateRequest request) {
        SetProductQuantityStateRequest effectiveRequest = request;
        if (effectiveRequest == null) {
            if (productId == null || quantityState == null) {
                throw new IllegalArgumentException("productId и quantityState должны быть заданы либо в теле, либо в параметрах запроса");
            }
            effectiveRequest = SetProductQuantityStateRequest.builder()
                .productId(productId)
                .quantityState(quantityState)
                .build();
        }
        return productService.setProductQuantityState(effectiveRequest);
    }

    @Override
    @GetMapping("/{productId}")
    public ProductDto getProduct(@PathVariable("productId") UUID productId) {
        return productService.getProduct(productId);
    }
}

