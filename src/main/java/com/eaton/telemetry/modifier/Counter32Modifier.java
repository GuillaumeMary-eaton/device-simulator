package com.eaton.telemetry.modifier;

import org.snmp4j.smi.Counter32;

/** This modifier instance modifies {@link org.snmp4j.smi.Counter32} variables. */
public class Counter32Modifier extends AbstractIntegerModifier<Counter32> {

    /**
     * Creates a default counter starting from 0 to {@link Integer#MAX_VALUE} with a step between 1 and 10.
     */
    public Counter32Modifier() {
        this(0, Integer.MAX_VALUE, 1, 10);
    }

    public Counter32Modifier(int minimum, int maximum, int minimumStep, int maximumStep) {
        super(minimum, maximum, minimumStep, maximumStep);
    }

    @Override
    protected Counter32 cast(long value) {
        return new Counter32(value);
    }
}
