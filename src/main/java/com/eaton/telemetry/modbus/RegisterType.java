package com.eaton.telemetry.modbus;

/**
 * Content generated thanks to Copilot.
 */
public enum RegisterType {

    /**
     * Holding Registers: These are read/write registers used to store data that can be modified by the master device.
     * They are typically used for configuration parameters and other data that needs to be read and written during operation.
     */
    H,
    /**
     * Input Registers: These are read-only registers used to store data that is read from the device.
     * They are typically used for sensor readings and other data that is updated by the device but not written by the master.
     */
    I,
    /**
     * Coils: These are read/write registers that represent binary outputs.
     * They are used to control on/off states of devices like relays or LEDs.
     */
    C,
    /**
     * Discrete Inputs: These are read-only registers that represent binary inputs.
     * They are used to read the on/off states of devices like switches or sensors.
     */
    D
}
