package ru.yandex.practicum.kafka.telemetry.domain;

public enum ConditionType {
    MOTION,
    LUMINOSITY,
    SWITCH,
    TEMPERATURE,
    CO2LEVEL,
    HUMIDITY;

    public boolean isBoolean() {
        return this == MOTION || this == SWITCH;
    }
}

