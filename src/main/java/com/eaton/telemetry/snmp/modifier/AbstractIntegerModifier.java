package com.eaton.telemetry.snmp.modifier;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedInteger;
import lombok.Getter;
import org.snmp4j.smi.UnsignedInteger32;

/**
 * This modifier has all utility methods to construct for unsigned integer variable modifiers.
 */
abstract class AbstractIntegerModifier<T extends UnsignedInteger32> implements VariableModifier<T> {

    /** The minimum allowed number for the resulting modified variable. */
    @Getter private final long minimum;

    /** The maximum allowed number for the resulting modified variable. */
    @Getter private final long maximum;

    /** The minimal step by which a variable will be incremented. */
    @Getter private final long minimumStep;

    /** The maximal step by which a variable will be incremented. */
    @Getter private final long maximumStep;

    public AbstractIntegerModifier(long minimum, long maximum, long minimumStep, long maximumStep) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.minimumStep = minimumStep;
        this.maximumStep = maximumStep;
        Preconditions.checkArgument(minimum >= 0, "minimum should not be negative");
        Preconditions.checkArgument(maximum >= 0, "maximum should not be negative");

        Preconditions.checkArgument(minimum <= UnsignedInteger.MAX_VALUE.longValue(), "minimum should not exceed 2^32-1 (4294967295 decimal)");
        Preconditions.checkArgument(maximum <= UnsignedInteger.MAX_VALUE.longValue(), "maximum should not exceed 2^32-1 (4294967295 decimal)");
    }

    /**
     * Casts the long value to the specified output type of the implementing modifier.
     *
     * @param value the value to cast
     * @return the casted value
     */
    protected abstract T cast(long value);

    @Override
    public final T modify(T variable) {
        long currentValue = variable.getValue();
        if (currentValue < minimum || currentValue > maximum) {
            currentValue = minimum;
        }
        long step = (Math.round(Math.random() * (maximumStep - minimumStep)) + minimumStep);

        long stepUntilMaximum = maximum - currentValue;
        long newValue;
        if (Math.abs(step) > Math.abs(stepUntilMaximum)) {
            newValue = minimum + (step - stepUntilMaximum - 1);
        } else {
            newValue = currentValue + step;
        }

        if (newValue < minimum) {
            newValue = minimum;
        } else if (newValue > maximum) {
            newValue = maximum;
        }

        return cast(newValue);
    }
}
