package com.eaton.telemetry;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import lombok.Getter;

/**
 * Class representing a sensor.
 * @param <V>
 * @param <I>
 */
public class Sensor<V, I> {

    @Getter
    private final I identifier;

    private final IntFunction<V> valueGenerator;

    private final AtomicInteger tickGenerator = new AtomicInteger(0);

    /**
     * Creates a new sensor with the given OID and value generator.
     *
     * @param identifier the identifier of the sensor
     * @param valueGenerator a function returning a value for every "tick" it is called, the "tick" is represented by a counter
     */
    public Sensor(I identifier, IntFunction<V> valueGenerator) {
        this.identifier = identifier;
        this.valueGenerator = valueGenerator;
    }

    /**
     * Returns a value for given "tick" / counter
     * @param index the counter for which the value is for (starts from 0 to ... whatever value the agent runs to
     * @return the value for the given counter
     */
    public V getValue(int index) {
        return valueGenerator.apply(index);
    }

    public V nextValue() {
        return valueGenerator.apply(tickGenerator.getAndIncrement());
    }
}
