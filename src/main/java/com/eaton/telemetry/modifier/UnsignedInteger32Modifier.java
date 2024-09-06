package com.eaton.telemetry.modifier;

import org.snmp4j.smi.UnsignedInteger32;

/** This modifier instance modifies {@link org.snmp4j.smi.UnsignedInteger32} variables. */
public class UnsignedInteger32Modifier extends AbstractIntegerModifier<UnsignedInteger32> {

    public UnsignedInteger32Modifier(long minimum, long maximum, long minimumStep, long maximumStep) {
        super(minimum, maximum, minimumStep, maximumStep);
    }

    @Override
    protected UnsignedInteger32 cast(long value) {
        return new UnsignedInteger32(value);
    }
}
