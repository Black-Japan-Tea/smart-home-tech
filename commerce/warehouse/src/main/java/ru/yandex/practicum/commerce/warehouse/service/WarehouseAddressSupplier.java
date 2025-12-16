package ru.yandex.practicum.commerce.warehouse.service;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.commerce.interaction.api.dto.AddressDto;

import java.security.SecureRandom;

@Component
public class WarehouseAddressSupplier {

    private static final String[] ADDRESSES = {"ADDRESS_1", "ADDRESS_2"};

    private final String currentAddress;

    public WarehouseAddressSupplier() {
        SecureRandom random = new SecureRandom();
        this.currentAddress = ADDRESSES[random.nextInt(ADDRESSES.length)];
    }

    public AddressDto currentAddress() {
        return AddressDto.builder()
                .country(currentAddress)
                .city(currentAddress)
                .street(currentAddress)
                .house(currentAddress)
                .flat(currentAddress)
                .build();
    }
}

