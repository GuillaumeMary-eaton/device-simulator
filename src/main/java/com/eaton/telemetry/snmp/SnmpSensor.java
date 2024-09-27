package com.eaton.telemetry.snmp;

import java.util.function.IntFunction;

import com.eaton.telemetry.Sensor;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

public class SnmpSensor<V extends Variable> extends Sensor<V> {

    private final V variable;

    private V value;

    public SnmpSensor(String oid, V variable, IntFunction<V> valueGenerator) {
        super(oid, valueGenerator);
        this.variable = variable;
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
