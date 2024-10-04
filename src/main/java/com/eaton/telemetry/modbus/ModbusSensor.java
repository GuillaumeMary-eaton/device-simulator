package com.eaton.telemetry.modbus;

import java.util.function.IntFunction;

import com.eaton.telemetry.Sensor;
import lombok.Getter;

/**
 * Very basic sensor for Modbus. For now it doesn't suit to all needs and focuses on a short[] value generator.
 * @param <V>
 */
@Getter
public class ModbusSensor<V> extends Sensor<V, Integer> {

    public static ModbusSensor<short[]> holding(int sensorId, IntFunction<short[]> valueGenerator) {
        return new ModbusSensor<>(sensorId, RegisterType.H, valueGenerator);
    }

    private final RegisterType registerType;

    public ModbusSensor(int sensorId, RegisterType registerType, IntFunction<V> valueGenerator) {
        super(sensorId, valueGenerator);
        this.registerType = registerType;
    }
}
