package com.eaton.telemetry.snmp.modifier;

import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import com.google.common.primitives.UnsignedLong;
import lombok.Getter;
import org.snmp4j.smi.AbstractVariable;
import org.snmp4j.smi.AssignableFromLong;
import org.snmp4j.smi.Counter64;

/**
 * This modifier instance modifies {@link Counter64} variables.
 * @param <V> the variable type to modify, should have been inheriting from {@link org.snmp4j.smi.Variable} but it
 *          causes compilation error due to clone() method visibility conflict with Object.clone()
 */
public class Counter64Modifier<V extends AbstractVariable & AssignableFromLong> implements IntFunction<V> {

    private Long currentValue = RandomGenerator.getDefault().nextLong();

    /** The minimum allowed number for the resulting modified variable. */
    @Getter private final UnsignedLong minimum;

    /** The maximum allowed number for the resulting modified variable. */
    @Getter private final UnsignedLong maximum;

    /** The minimal step by which a variable will be incremented. */
    @Getter private final UnsignedLong minimumStep;

    /** The maximal step by which a variable will be incremented. */
    @Getter private final UnsignedLong maximumStep;

    private final Supplier<V> variableFactory;

    /**
     * Creates a default counter starting from 0 to {@link Integer#MAX_VALUE} with a step between 1 and 10.
     */
    public Counter64Modifier() {
        this(0, Long.MAX_VALUE, 1, 10, () -> (V) new Counter64());
    }

    public Counter64Modifier(long minimum, long maximum, long minimumStep, long maximumStep) {
        this(UnsignedLong.valueOf(minimum), UnsignedLong.valueOf(maximum), UnsignedLong.valueOf(minimumStep), UnsignedLong.valueOf(maximumStep), () -> (V) new Counter64());
    }

    public Counter64Modifier(long minimum, long maximum, long minimumStep, long maximumStep, Supplier<V> variableFactory) {
        this(UnsignedLong.valueOf(minimum), UnsignedLong.valueOf(maximum), UnsignedLong.valueOf(minimumStep), UnsignedLong.valueOf(maximumStep), variableFactory);
    }

    public Counter64Modifier(UnsignedLong minimum, UnsignedLong maximum, UnsignedLong minimumStep, UnsignedLong maximumStep, Supplier<V> variableFactory) {
        this.minimum = Optional.ofNullable(minimum).orElse(UnsignedLong.ZERO);
        this.maximum = Optional.ofNullable(maximum).orElse(UnsignedLong.MAX_VALUE);

        this.minimumStep = Optional.ofNullable(minimumStep).orElse(UnsignedLong.ZERO);
        this.maximumStep = Optional.ofNullable(maximumStep).orElse(UnsignedLong.ONE);
        this.variableFactory = variableFactory;
    }

    public Long modify(Long variable) {
        UnsignedLong currentValue = UnsignedLong.valueOf(variable.toString());
        if (currentValue.compareTo(minimum) < 0 || currentValue.compareTo(maximum) > 0) {
            currentValue = minimum;
        }

        UnsignedLong step = UnsignedLong.valueOf((long) (Math.random() * maximumStep.minus(minimumStep).longValue())).plus(minimumStep);
        UnsignedLong newValue = currentValue.plus(step);

        return newValue.longValue();
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
