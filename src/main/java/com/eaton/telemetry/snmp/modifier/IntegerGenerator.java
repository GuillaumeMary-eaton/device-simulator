package com.eaton.telemetry.snmp.modifier;

import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedInteger;
import lombok.Getter;
import org.snmp4j.smi.AbstractVariable;
import org.snmp4j.smi.AssignableFromLong;

/**
 * This modifier has all utility methods to construct for unsigned integer variable modifiers.
 * @param <V> the variable type to modify, should have been inheriting from {@link org.snmp4j.smi.Variable} but it
 *          causes compilation error due to clone() method visibility conflict with Object.clone()
 */
public class IntegerGenerator<V extends AbstractVariable & AssignableFromLong>
        implements IntFunction<V>, VariableGenerator<V> {

    private Integer currentValue = RandomGenerator.getDefault().nextInt();

    /** The minimum allowed number for the resulting modified variable. */
    @Getter private final int minimum;

    /** The maximum allowed number for the resulting modified variable. */
    @Getter private final int maximum;

    /** The minimal step by which a variable will be incremented. */
    @Getter private final int minimumStep;

    /** The maximal step by which a variable will be incremented. */
    @Getter private final int maximumStep;

    private final Supplier<V> variableFactory;

    public IntegerGenerator(int minimum, int maximum, int minimumStep, int maximumStep, Supplier<V> variableFactory) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.minimumStep = minimumStep;
        this.maximumStep = maximumStep;
        this.variableFactory = variableFactory;
        Preconditions.checkArgument(minimum >= 0, "minimum should not be negative");
        Preconditions.checkArgument(maximum >= 0, "maximum should not be negative");

        Preconditions.checkArgument(minimum <= UnsignedInteger.MAX_VALUE.longValue(), "minimum should not exceed 2^32-1 (4294967295 decimal)");
        Preconditions.checkArgument(maximum <= UnsignedInteger.MAX_VALUE.longValue(), "maximum should not exceed 2^32-1 (4294967295 decimal)");
    }

    public Integer modify(Integer variable) {
        long currentValue = variable;
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

        return (int) newValue;
    }

    @Override
    public V apply(int tick) {
        if (tick > 0) {
            this.currentValue = modify(currentValue);
        }
        V newVariable = this.variableFactory.get();
        newVariable.setValue(currentValue);
        return newVariable;
    }
}
