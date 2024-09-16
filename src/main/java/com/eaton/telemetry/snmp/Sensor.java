package com.eaton.telemetry.snmp;

import java.time.Duration;
import java.util.function.Supplier;

import com.eaton.telemetry.snmp.modifier.VariableModifier;
import lombok.Getter;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

public class Sensor<V extends Variable> {

    @Getter
    private final OID oid;

    private final Supplier<V> valueGenerator;

    @Getter
    private final Duration period;

    @Getter
    private final Duration initialDelay;

    public Sensor(OID oid, V initialValue, VariableModifier<V> variableModifier, Duration period, Duration initialDelay) {
        this(oid, () -> {
            V value = initialValue;
            if (variableModifier != null) {
                value = variableModifier.modify(value);
            }
            return value;
        }, period, initialDelay);
    }

    public Sensor(OID oid, Supplier<V> valueGenerator, Duration period, Duration initialDelay) {
        this.oid = oid;
        this.valueGenerator = valueGenerator;
        this.period = period;
        this.initialDelay = initialDelay;
    }

    public V getValue() {
        return valueGenerator.get();
    }
}
