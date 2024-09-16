package com.eaton.telemetry.snmp;

import java.util.List;

import com.eaton.telemetry.snmp.modifier.Modifier;
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
     * See {@link Modifier} and {@link com.eaton.telemetry.snmp.modifier.VariableModifier}.
     *
     * @return list of modifier definitions
     */
    @Getter private final List<Modifier> modifiers;

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
    public Device(String name, List<Modifier> modifiers, List<Long> vlans) {
        this.name = name;
        this.modifiers = modifiers;
        this.vlans = vlans;
    }
}
