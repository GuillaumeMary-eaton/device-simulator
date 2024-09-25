package com.eaton.telemetry.snmp.modifier;

import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.smi.AbstractVariable;
import org.snmp4j.smi.AssignableFromInteger;
import org.snmp4j.smi.Integer32;

/**
 * This modifier instance modifies {@link Integer32} variables.
 * @param <V> the variable type to modify, should have been inheriting from {@link org.snmp4j.smi.Variable} but it
 *          causes compilation error due to clone() method visibility conflict with Object.clone()
 */
@Slf4j
public class Integer32Generator<V extends AbstractVariable & AssignableFromInteger>
        implements IntFunction<V>, VariableGenerator<V> {

    private Integer currentValue = RandomGenerator.getDefault().nextInt();

    /** The minimum allowed number for the resulting modified variable. */
    @Getter private final Integer minimum;

    /** The maximum allowed number for the resulting modified variable. */
    @Getter private final Integer maximum;

    /** The minimal step by which a variable will be incremented. */
    @Getter private final Integer minimumStep;

    /** The maximal step by which a variable will be incremented. */
    @Getter private final Integer maximumStep;

    private final Supplier<V> variableFactory;

    /**
     * Creates a default integer starting from 0 to {@link Integer#MAX_VALUE} with a step between 1 and 10.
     */
    public Integer32Generator() {
        this(0, Integer.MAX_VALUE, 1, 10, () -> (V) new Integer32());
    }

    public Integer32Generator(Integer minimum, Integer maximum, Integer minimumStep, Integer maximumStep, Supplier<V> variableFactory) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.minimumStep = minimumStep;
        this.maximumStep = maximumStep;
        this.variableFactory = variableFactory;
    }

    /**
     * Increments the current value by a random number between the minimum and maximum step.
     * <p>
     * An overflow can occur and will be considered in the minimum and maximum interval.
     *
     * @param currentValue the current value to modify
     * @param minimum      {@link #minimum}
     * @param maximum      {@link #maximum}
     * @param minimumStep  {@link #minimumStep}
     * @param maximumStep  {@link #maximumStep}
     * @return the modified variable value
     */
    protected int modify(int currentValue, int minimum, int maximum, int minimumStep, int maximumStep) {
        int currentValidValue = currentValue;
        if (currentValue < minimum || currentValue > maximum) {
            currentValidValue = minimum;
        }
        int step = (int) (Math.round(Math.random() * (maximumStep - minimumStep)) + minimumStep);

        int stepUntilMaximum = maximum - currentValidValue;
        int newValue;
        if (Math.abs(step) > Math.abs(stepUntilMaximum)) {
            newValue = minimum + (step - stepUntilMaximum - 1);
        } else {
            newValue = currentValidValue + step;
        }

        if (newValue < minimum) {
            newValue = minimum;
        } else if (newValue > maximum) {
            newValue = maximum;
        }

        return newValue;
    }

    @Override
    public V apply(int tick) {
        if (tick > 0) {
            int newValue = this.modify(currentValue, minimum, maximum, minimumStep, maximumStep);
            log.trace("Variable {} will be tuned to {}", currentValue, newValue);
            currentValue = newValue;
        }
        V newVariable = this.variableFactory.get();
        newVariable.setValue(currentValue);
        return newVariable;
    }
}
