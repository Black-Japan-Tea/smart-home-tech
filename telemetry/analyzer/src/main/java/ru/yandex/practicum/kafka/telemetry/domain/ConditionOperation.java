package ru.yandex.practicum.kafka.telemetry.domain;

public enum ConditionOperation {
    EQUALS,
    GREATER_THAN,
    LOWER_THAN;

    public boolean matches(int actual, int expected) {
        return switch (this) {
            case EQUALS -> actual == expected;
            case GREATER_THAN -> actual > expected;
            case LOWER_THAN -> actual < expected;
        };
    }
}

