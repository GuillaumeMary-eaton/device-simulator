package snmp;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.eaton.telemetry.snmp.AgentConfiguration;
import com.eaton.telemetry.snmp.Device;
import com.eaton.telemetry.snmp.Sensor;
import com.eaton.telemetry.snmp.SnmpAgent;
import com.eaton.telemetry.snmp.Walks;
import com.eaton.telemetry.snmp.modifier.Counter32Modifier;
import com.eaton.telemetry.snmp.modifier.Counter64Modifier;
import com.eaton.telemetry.snmp.modifier.Gauge32Modifier;
import com.eaton.telemetry.snmp.modifier.Integer32Modifier;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

public class Server {

    public static void main(String[] args) throws IOException {
        SnmpAgent agent = new SnmpAgent(new AgentConfiguration(
                "example",
                new Device(
                        "Cisco",
                        Set.<Sensor<?>>of(
                                new Sensor<>(".1.3.6.1.4.1.9.2.1.56", new Integer32Modifier<>(0, 100, 0, 100, Integer32::new)),
                                new Sensor<>(".1.3.6.1.4.1.9.2.1.57", new Integer32Modifier<>(0, 100, 0, 100, Integer32::new)),
                                new Sensor<>(".1.3.6.1.4.1.9.2.1.58", new Integer32Modifier<>(0, 100, 0, 100, Integer32::new)),
                                new Sensor<>(".1.3.6.1.4.1.9.9.48.1.1.1.6", new Gauge32Modifier(1547369620, 1547374620, -30, 30)),
                                new Sensor<>(".1.3.6.1.4.1.9.9.48.1.1.1.7", new Gauge32Modifier(1547369620, 1547374620, -30, 30)),
                                new Sensor<>(".1.3.6.1.4.1.9.9.48.1.1.1.5", new Gauge32Modifier(0, 5000, -30, 30)),
                                new Sensor<>(".1.3.6.1.4.1.9.9.13.1.3.1.3", new Gauge32Modifier(25, 60, -10, 10)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.10", new Counter32Modifier(0, Integer.MAX_VALUE, 0, 3840000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.11", new Counter32Modifier(0, Integer.MAX_VALUE, 0, 2000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.12", new Counter32Modifier(0, Integer.MAX_VALUE, 0, 2000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.13", new Counter32Modifier(0, Integer.MAX_VALUE, 0, 2000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.15", new Counter32Modifier(0, Integer.MAX_VALUE, 0, 2000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.16", new Counter32Modifier(0, Integer.MAX_VALUE, 0, 3840000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.17", new Counter32Modifier(0, Integer.MAX_VALUE, 0, 20000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.18", new Counter32Modifier(0, Integer.MAX_VALUE, 0, 20000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.19", new Counter32Modifier(0, Integer.MAX_VALUE, 0, 20000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.20", new Counter32Modifier(0, Integer.MAX_VALUE, 0, 20000)),
                                new Sensor<>(".1.3.6.1.2.1.31.1.1.1.6", new Counter64Modifier<>(0, 1844674407370955161L, 0, 3840000)),
                                new Sensor<>(".1.3.6.1.2.1.31.1.1.1.7", new Counter64Modifier<>(0, 1844674407370955161L, 0, 3840000)),
                                new Sensor<>(".1.3.6.1.2.1.31.1.1.1.8", new Counter64Modifier<>(0, 1844674407370955161L, 0, 3840000)),
                                new Sensor<>(".1.3.6.1.2.1.31.1.1.1.9", new Counter64Modifier<>(0, 1844674407370955161L, 0, 3840000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.13", new Counter64Modifier<>(0, Integer.MAX_VALUE, 0, 20000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.14", new Counter64Modifier<>(0, Integer.MAX_VALUE, 0, 20000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.15", new Counter64Modifier<>(0, Integer.MAX_VALUE, 0, 20000)),
                                new Sensor<>(".1.3.6.1.2.1.31.1.1.1.10", new Counter64Modifier<>(0, 1844674407370955161L, 0, 3840000)),
                                new Sensor<>(".1.3.6.1.2.1.31.1.1.1.11", new Counter64Modifier<>(0, 1844674407370955161L, 0, 3840000)),
                                new Sensor<>(".1.3.6.1.2.1.31.1.1.1.12", new Counter64Modifier<>(0, 1844674407370955161L, 0, 3840000)),
                                new Sensor<>(".1.3.6.1.2.1.31.1.1.1.13", new Counter64Modifier<>(0, 1844674407370955161L, 0, 3840000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.19", new Counter32Modifier(0, Integer.MAX_VALUE, 0, 20000)),
                                new Sensor<>(".1.3.6.1.2.1.2.2.1.20", new Counter32Modifier(0, Integer.MAX_VALUE, 0, 20000))
                        ),
                        List.of(42L, 9L)),
                "127.0.0.1", 53142, null, new File(Server.class.getResource("/").getPath())
        ));

        Map<OID, Variable> oidVariableMap = Walks.readWalk(new File(Server.class.getResource("/example.txt").getPath()));
        agent.setBindings(oidVariableMap.entrySet().stream().map(entry -> new Sensor<>(entry.getKey(), value -> entry.getValue())).collect(Collectors.toSet()));

        agent.execute();
    }

}
