package com.eaton.telemetry.modifier;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import lombok.Getter;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;

/**
 * This modifier implementation modifies {@link Counter32} variables depending on their community context.
 */
public class CommunityIndexCounter32Modifier implements CommunityContextModifier<Counter32> {
    /**
     * Mapping of SNMP community context to SNMP OID and result.
     */
    @Getter private final Map<Long, Long> communityContextMapping;

    public CommunityIndexCounter32Modifier(Map<Long, Long> communityContextMapping) {
        this.communityContextMapping = communityContextMapping;
    }

    @Override
    public final Counter32 modify(Counter32 variable) {
        if (variable == null) {
            return new Counter32(0);
        }
        return variable;
    }

    @Override
    public Map<OID, Variable> getVariableBindings(OctetString context, OID queryOID) {
        if (queryOID != null && context != null && context.getValue().length != 0) {
            if (!queryOID.toString().isEmpty() && !context.toString().isEmpty() && communityContextMapping.containsKey(Long.parseLong(context.toString()))) {
                return Collections.singletonMap(queryOID, new Counter32(communityContextMapping.get(Long.parseLong(context.toString()))));
            }
        } else if (queryOID != null) {
            return Collections.singletonMap(queryOID, modify(null));

        }
        return new TreeMap<>();
    }
}
