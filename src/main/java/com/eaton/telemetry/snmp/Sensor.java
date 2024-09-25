package com.eaton.telemetry.snmp;

import java.util.function.IntFunction;

import lombok.Getter;
import org.snmp4j.smi.OID;

public class Sensor<V> {

    @Getter
    private final OID oid;

    private final IntFunction<V> valueGenerator;

    /**
     * Creates a new sensor with the given OID and value generator.
     *
     * @param oid the identifier of the sensor
     * @param valueGenerator a function returning a value for every "tick" it is called, the "tick" is represented by a counter
     */
    public Sensor(String oid, IntFunction<V> valueGenerator) {
        this(new OID(oid), valueGenerator);
    }

    /**
     * Creates a new sensor with the given OID and value generator.
     *
     * @param oid the identifier of the sensor
     * @param valueGenerator a function returning a value for every "tick" it is called, the "tick" is represented by a counter
     */
    public Sensor(OID oid, IntFunction<V> valueGenerator) {
        this.oid = oid;
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
}
