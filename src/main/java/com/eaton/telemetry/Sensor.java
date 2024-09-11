package com.eaton.telemetry;

import com.eaton.telemetry.modifier.VariableModifier;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

public class Sensor<V extends Variable> {

    private final OID oid;

    private V variable;

    private final VariableModifier<V> modifier;

    public Sensor(OID oid, V seed,  VariableModifier<V> modifier) {
        this.oid = oid;
        this.variable = seed;
        this.modifier = modifier;
    }

    public OID getOid() {
        return oid;
    }

    public Variable getCurrentValue() {
        variable = modifier.modify(variable);
        return variable;
    }
}
