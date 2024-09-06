package com.eaton.telemetry.modifier;

import org.snmp4j.smi.Counter32;

/** This modifier instance modifies {@link org.snmp4j.smi.Counter32} variables. */
public class Counter32Modifier extends AbstractIntegerModifier<Counter32> {

    public Counter32Modifier(long minimum, long maximum, long minimumStep, long maximumStep) {
        super(minimum, maximum, minimumStep, maximumStep);
    }

    @Override
    protected Counter32 cast(long value) {
        return new Counter32(value);
    }
}
