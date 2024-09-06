package com.eaton.telemetry.modifier;

import java.lang.reflect.InvocationTargetException;

import com.eaton.telemetry.type.ModifierProperties;
import com.eaton.telemetry.type.WildcardOID;
import com.eaton.telemetry.InitializationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

/**
 * Representation of a generic modifier.
 * <br>
 * See also {@link VariableModifier}.
 *
 * @param <T> the variable type
 */
public class Modifier<T extends Variable> implements VariableModifier<T> {

    /** The OID range {@code this} modifier should process. */
    private final WildcardOID oid;

    /**
     * The wrapped variable modifier for this generic modifier.
     *
     * @return variable modifier.
     */
    @Getter private final VariableModifier<T> modifier;

    /**
     * Constructs a wrapped modifier.
     *
     * @param oid the wildcard OID to define the OID range {@code this} modifier should process
     * @param modifierClass the class of the modifier
     * @param properties the initialization properties
     */
    @SuppressWarnings("unchecked")
    public Modifier(@JsonProperty("oid") final String oid,
                    @JsonProperty("class") final String modifierClass,
                    @JsonProperty("properties") final ModifierProperties properties) {
        this.oid = new WildcardOID(oid);

        try {
            this.modifier = (VariableModifier) Class.forName(modifierClass).getConstructor().newInstance();
            this.modifier.init(properties);
        } catch (final ClassNotFoundException | NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new InitializationException("could not construct a modifier for the oid " + oid + " and class " + modifierClass, e);
        }
    }

    /**
     * Returns {@code true} if this modifier is applicable for the specified {@code oid}, otherwise {@code false}.
     *
     * @param oid the oid to check
     * @return {@code true} if this modifier is applicable for the specified {@code oid}, otherwise {@code false}
     */
    public boolean isApplicable(final OID oid) {
        return this.oid.matches(oid);
    }

    @Override
    public T modify(T variable) {
        return modifier.modify(variable);
    }
}
