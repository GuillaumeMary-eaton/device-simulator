package com.eaton.telemetry.snmp.modifier;

import org.snmp4j.smi.Gauge32;

/** This modifier instance modifies {@link org.snmp4j.smi.Gauge32} variables. */
public class Gauge32Generator extends IntegerGenerator<Gauge32> {

    /**
     * Creates a default gauge starting from 0 to {@link Integer#MAX_VALUE} with a step between 1 and 10.
     */
    public Gauge32Generator() {
        this(0, Integer.MAX_VALUE, 1, 10);
    }

    public Gauge32Generator(int minimum, int maximum, int minimumStep, int maximumStep) {
        super(minimum, maximum, minimumStep, maximumStep, Gauge32::new);
    }
}
