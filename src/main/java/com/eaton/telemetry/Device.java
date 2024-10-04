package com.eaton.telemetry;

import java.util.List;
import java.util.Set;

import com.eaton.telemetry.snmp.modifier.VariableGenerator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a device type.
 * <br>
 * You can find example configurations within the test resources of this project.
 */
@ToString(of = "name") @EqualsAndHashCode
public class Device {

    /**
     * The device name.
     *
     * @return the device name
     */
    @Getter private final String name;

    /**
     * The unmodifiable list of modifier definitions.
     * <br>
     * See {@link Sensor} and {@link VariableGenerator}.
     *
     * @return list of modifier definitions
     */
    @Getter private final Set<Sensor<Object, Object>> modifiers;

    /**
     * The unmodifiable list of vlans.
     *
     * @return list of vlans represented as {@link Long}.
     */
    @Getter private final List<Long> vlans;

    /**
     * Constructs a new device type.
     *
     * @param name the name of the device
     * @param modifiers the modifiers
     */
    public Device(String name, Set<? extends Sensor<?, ?>> modifiers, List<Long> vlans) {
        this.name = name;
        this.modifiers = (Set<Sensor<Object, Object>>) modifiers;
        this.vlans = vlans;
    }
}
