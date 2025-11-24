package ru.yandex.practicum.kafka.telemetry.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "scenario_conditions")
@IdClass(ScenarioCondition.ScenarioConditionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioCondition {
    @Id
    @ManyToOne
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Id
    @ManyToOne
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @Id
    @ManyToOne
    @JoinColumn(name = "condition_id", nullable = false)
    private Condition condition;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioConditionId implements Serializable {
        private Long scenario;
        private String sensor;
        private Long condition;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScenarioConditionId that = (ScenarioConditionId) o;
            return java.util.Objects.equals(scenario, that.scenario) &&
                   java.util.Objects.equals(sensor, that.sensor) &&
                   java.util.Objects.equals(condition, that.condition);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(scenario, sensor, condition);
        }
    }
}

