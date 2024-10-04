package com.eaton.telemetry.snmp;

import java.util.function.IntFunction;

import com.eaton.telemetry.Sensor;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

/**
 * {@link Sensor} dedicated to SNMP protocol. It is expected to be used with {@link SnmpAgent}, meanwhile it's also
 * compatible with {@link SnmpTrapAgent} but the {@link #setValue(Variable)} makes no sense with SNMP Trap protocol.
 * This class brings {@link OID} as identifier, and also {@link #setValue(Variable)} to allow SNMP server to set a
 * value on a set Command.
 *
 * @param <V> variable and value type
 */
public class SnmpSensor<V extends Variable> extends Sensor<V, OID> {

    /**
     * Variable manged by this sensor
     */
    private final V variable;

    /**
     * Current value of the sensor
     */
    private V value;

    /**
     * Creates a new sensor with the given OID and value generator.
     *
     * @param oid the identifier of the sensor
     * @param valueGenerator a function returning a value for every "tick" it is called, the "tick" is represented by a counter
     */
    public SnmpSensor(String oid, V variable, IntFunction<V> valueGenerator) {
        this(new OID(oid), variable, valueGenerator);
    }

    public SnmpSensor(OID oid, V variable, IntFunction<V> valueGenerator) {
        super(oid, valueGenerator);
        this.variable = variable;
    }

    public V getVariable() {
        return variable;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public V nextValue() {
        if (value != null) {
            V result = value;
            value = null;
            return result;
        } else {
            return super.nextValue();
        }
    }
}
