package com.eaton.telemetry.snmp.modifier;

import java.lang.reflect.InvocationTargetException;

import com.eaton.telemetry.snmp.InitializationException;
import com.eaton.telemetry.snmp.type.WildcardOID;
import lombok.Getter;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

/**
 * Representation of a generic modifier.
 * <br>
 * See also {@link com.eaton.telemetry.snmp.modifier.VariableModifier}.
 *
 * @param <T> the variable type
 */
public class Modifier<T extends Variable> implements com.eaton.telemetry.snmp.modifier.VariableModifier<T> {

    /** The OID range {@code this} modifier should process. */
    @Getter final WildcardOID oid;

    /**
     * The wrapped variable modifier for this generic modifier.
     *
     * @return variable modifier.
     */
    @Getter private final com.eaton.telemetry.snmp.modifier.VariableModifier<T> modifier;

    /**
     * Constructs a wrapped modifier.
     *
     * @param oid the wildcard OID to define the OID range {@code this} modifier should process
     * @param modifierClass the class of the modifier
     */
    public Modifier(String oid,
                    Class<? extends com.eaton.telemetry.snmp.modifier.VariableModifier<T>> modifierClass) {
        this.oid = new WildcardOID(oid);

        try {
            this.modifier = modifierClass.getConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new InitializationException("could not construct a modifier for the oid " + oid + " and class " + modifierClass, e);
        }
    }

    public Modifier(String oid,
                    com.eaton.telemetry.snmp.modifier.VariableModifier<T> modifier) {
        this.oid = new WildcardOID(oid);
        this.modifier = modifier;
    }

    /**
     * Returns {@code true} if this modifier is applicable for the specified {@code oid}, otherwise {@code false}.
     *
     * @param oid the oid to check
     * @return {@code true} if this modifier is applicable for the specified {@code oid}, otherwise {@code false}
     */
    public boolean isApplicable(OID oid) {
        return this.oid.matches(oid);
    }

    @Override
    public T modify(T variable) {
        return modifier.modify(variable);
    }
}
