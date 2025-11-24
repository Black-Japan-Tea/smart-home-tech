# Интеграционные тесты для воссоздания проблем с UNSPECIFIED

Эти тесты воссоздают проблему, когда Hub Router отправляет события с `UNSPECIFIED` значениями, и мы можем увидеть логи ошибок для анализа.

## Проблема

1. **Hub Router отправляет события с UNSPECIFIED** - enum значения не установлены явно
2. **Collector пропускает эти события** - они не сохраняются в Kafka
3. **Analyzer не получает сценарии** - события SCENARIO_ADDED не доходят до БД
4. **Команды не выполняются** - сценарии не найдены в БД

## Как использовать тесты

### 1. Запуск тестов Collector

```bash
cd telemetry
mvn test -pl collector -Dtest=CollectorIntegrationTest
```

Эти тесты проверяют:
- `shouldSkipDeviceAddedWithUnspecifiedType` - DEVICE_ADDED с UNSPECIFIED пропускается
- `shouldSkipScenarioAddedWithUnspecifiedConditionType` - SCENARIO_ADDED с UNSPECIFIED в условии пропускается
- `shouldSkipScenarioAddedWithUnspecifiedActionType` - SCENARIO_ADDED с UNSPECIFIED в действии пропускается
- `shouldProcessValidDeviceAddedEvent` - валидные события обрабатываются нормально

**Ожидаемые логи:**
```
ERROR ru.yandex.practicum.kafka.telemetry.mapper.ProtobufMapper -- Received device with unspecified or unrecognized type: DEVICE_TYPE_UNSPECIFIED
ERROR ru.yandex.practicum.kafka.telemetry.mapper.ProtobufMapper -- Received condition with unspecified or unrecognized type: CONDITION_TYPE_UNSPECIFIED
```

### 2. Запуск тестов Analyzer

```bash
cd telemetry
mvn test -pl analyzer -Dtest=AnalyzerIntegrationTest
```

Эти тесты проверяют:
- `shouldFailWhenSensorNotFoundForScenario` - ошибка, когда сенсор не найден (DEVICE_ADDED был пропущен)
- `shouldReturnEmptyListWhenNoScenariosInDatabase` - сценариев нет в БД (SCENARIO_ADDED были пропущены)
- `shouldLogActionWhenHubRouterUnavailable` - логирование действий, когда Hub Router недоступен
- `shouldDemonstrateFullProblemScenario` - полный сценарий проблемы

**Ожидаемые логи:**
```
WARN ru.yandex.practicum.kafka.telemetry.processor.SnapshotProcessor -- Нет сценариев для хаба: hub-1
```

## Воссоздание проблемы в реальном приложении

### Шаг 1: Запустите сервисы

```bash
# Terminal 1: Collector
cd telemetry/collector
mvn spring-boot:run

# Terminal 2: Aggregator
cd telemetry/aggregator
mvn spring-boot:run

# Terminal 3: Analyzer
cd telemetry/analyzer
mvn spring-boot:run
```

### Шаг 2: Запустите Hub Router

```bash
java -jar .github/workflows/stuff/hub-router.jar \
  --hub-router.execution.collector.port=59091 \
  --hub-router.execution.mode=ANALYZE
```

### Шаг 3: Проверьте логи

В логах Collector вы увидите:
```
ERROR ru.yandex.practicum.kafka.telemetry.controller.GrpcEventController -- Получено событие DEVICE_ADDED с UNSPECIFIED типом: hubId=hub-1, deviceId=...
ERROR ru.yandex.practicum.kafka.telemetry.controller.GrpcEventController -- Пропускаем событие SCENARIO_ADDED с UNSPECIFIED значениями: hubId=hub-1, name=...
```

В логах Analyzer вы увидите:
```
WARN ru.yandex.practicum.kafka.telemetry.processor.SnapshotProcessor -- Нет сценариев для хаба: hub-1. Проверьте, что события SCENARIO_ADDED были успешно обработаны HubEventProcessor.
```

## Анализ проблемы

### Причина

Hub Router отправляет события с `UNSPECIFIED` значениями enum, потому что:
1. Protobuf enum по умолчанию имеет значение 0 (UNSPECIFIED)
2. Hub Router не устанавливает enum значения явно
3. Наша система правильно отклоняет такие события

### Решение

Проблема в Hub Router - он должен устанавливать enum значения явно. Но так как мы не можем изменить Hub Router, мы:
1. Логируем ошибки для диагностики
2. Пропускаем события с UNSPECIFIED (возвращаем успешный ответ, но не сохраняем)
3. Добавили детальное логирование для отслеживания проблемы

## Следующие шаги

1. Проверьте логи Collector - найдите события с UNSPECIFIED
2. Проверьте логи Analyzer - убедитесь, что сценарии не сохраняются
3. Проверьте Kafka топики - убедитесь, что события не сохраняются
4. Передайте логи для анализа

