package com.eaton.telemetry.snmp.modifier;

import org.snmp4j.smi.Variable;

/** Abstract definition of a variable modifier. 
 * @param <T> the variable type that gets modified by this instance.
 */
public interface VariableModifier<T extends Variable> {

    /**
     * Modifies the specified {@code variable} and returns a copy of it.
     * 
     * @param variable the variable to modify
     * @return the modified variable
     */
    T modify(final T variable);
}
