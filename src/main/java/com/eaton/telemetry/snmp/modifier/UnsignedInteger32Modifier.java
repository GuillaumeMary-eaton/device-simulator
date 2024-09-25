package com.eaton.telemetry.snmp.modifier;

import org.snmp4j.smi.UnsignedInteger32;

/** This modifier instance modifies {@link org.snmp4j.smi.UnsignedInteger32} variables. */
public class UnsignedInteger32Modifier extends AbstractIntegerModifier<UnsignedInteger32> {

    public UnsignedInteger32Modifier(int minimum, int maximum, int minimumStep, int maximumStep) {
        super(minimum, maximum, minimumStep, maximumStep, UnsignedInteger32::new);
    }
}
