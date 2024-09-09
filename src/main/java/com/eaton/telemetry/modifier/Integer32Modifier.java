package com.eaton.telemetry.modifier;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.smi.Integer32;

/** This modifier instance modifies {@link Integer32} variables. */
@Slf4j
public class Integer32Modifier implements VariableModifier<Integer32> {

    /** The minimum allowed number for the resulting modified variable. */
    @Getter private final Integer minimum;

    /** The maximum allowed number for the resulting modified variable. */
    @Getter private final Integer maximum;

    /** The minimal step by which a variable will be incremented. */
    @Getter private final Integer minimumStep;

    /** The maximal step by which a variable will be incremented. */
    @Getter private final Integer maximumStep;

    /**
     * Creates a default integer starting from 0 to {@link Integer#MAX_VALUE} with a step between 1 and 10.
     */
    public Integer32Modifier() {
        this(0, Integer.MAX_VALUE, 1, 10);
    }

    public Integer32Modifier(Integer minimum, Integer maximum, Integer minimumStep, Integer maximumStep) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.minimumStep = minimumStep;
        this.maximumStep = maximumStep;
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
    public Integer32 modify(Integer32 variable) {
        int newValue = this.modify(variable.getValue(), minimum, maximum, minimumStep, maximumStep);
        log.trace("Counter32 variable {} will be tuned to {}", variable.getValue(), newValue);
        return new Integer32(newValue);
    }
}
