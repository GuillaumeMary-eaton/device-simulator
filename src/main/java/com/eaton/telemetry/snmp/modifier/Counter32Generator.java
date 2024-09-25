package com.eaton.telemetry.snmp.modifier;

import org.snmp4j.smi.Counter32;

/** This modifier instance modifies {@link org.snmp4j.smi.Counter32} variables. */
public class Counter32Generator extends IntegerGenerator<Counter32> {

    /**
     * Creates a default counter starting from 0 to {@link Integer#MAX_VALUE} with a step between 1 and 10.
     */
    public Counter32Generator() {
        this(0, Integer.MAX_VALUE, 1, 10);
    }

    public Counter32Generator(int minimum, int maximum, int minimumStep, int maximumStep) {
        super(minimum, maximum, minimumStep, maximumStep, Counter32::new);
    }
}
