package com.eaton.telemetry.snmp.modifier;

import java.util.Optional;

import com.google.common.primitives.UnsignedLong;
import lombok.Getter;
import org.snmp4j.smi.Counter64;

/** This modifier instance modifies {@link Counter64} variables. */
public class Counter64Modifier implements VariableModifier<Counter64> {

    /** The minimum allowed number for the resulting modified variable. */
    @Getter private final UnsignedLong minimum;

    /** The maximum allowed number for the resulting modified variable. */
    @Getter private final UnsignedLong maximum;

    /** The minimal step by which a variable will be incremented. */
    @Getter private final UnsignedLong minimumStep;

    /** The maximal step by which a variable will be incremented. */
    @Getter private final UnsignedLong maximumStep;

    /**
     * Creates a default counter starting from 0 to {@link Integer#MAX_VALUE} with a step between 1 and 10.
     */
    public Counter64Modifier() {
        this(0, Long.MAX_VALUE, 1, 10);
    }

    public Counter64Modifier(long minimum, long maximum, long minimumStep, long maximumStep) {
        this(UnsignedLong.valueOf(minimum), UnsignedLong.valueOf(maximum), UnsignedLong.valueOf(minimumStep), UnsignedLong.valueOf(maximumStep));
    }

    public Counter64Modifier(UnsignedLong minimum, UnsignedLong maximum, UnsignedLong minimumStep, UnsignedLong maximumStep) {
        this.minimum = Optional.ofNullable(minimum).orElse(UnsignedLong.ZERO);
        this.maximum = Optional.ofNullable(maximum).orElse(UnsignedLong.MAX_VALUE);

        this.minimumStep = Optional.ofNullable(minimumStep).orElse(UnsignedLong.ZERO);
        this.maximumStep = Optional.ofNullable(maximumStep).orElse(UnsignedLong.ONE);
    }

    @Override
    public Counter64 modify(Counter64 variable) {
        UnsignedLong currentValue = UnsignedLong.valueOf(variable.toString());
        if (currentValue.compareTo(minimum) < 0 || currentValue.compareTo(maximum) > 0) {
            currentValue = minimum;
        }

        UnsignedLong step = UnsignedLong.valueOf((long) (Math.random() * maximumStep.minus(minimumStep).longValue())).plus(minimumStep);
        UnsignedLong newValue = currentValue.plus(step);

        return new Counter64(newValue.longValue());
    }

}