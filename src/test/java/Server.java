import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.eaton.telemetry.AgentConfiguration;
import com.eaton.telemetry.Device;
import com.eaton.telemetry.SnmpmanAgent;
import com.eaton.telemetry.Walks;
import com.eaton.telemetry.modifier.CommunityIndexCounter32Modifier;
import com.eaton.telemetry.modifier.Counter32Modifier;
import com.eaton.telemetry.modifier.Counter64Modifier;
import com.eaton.telemetry.modifier.Gauge32Modifier;
import com.eaton.telemetry.modifier.Integer32Modifier;
import com.eaton.telemetry.modifier.Modifier;

public class Server {

    public static void main(String[] args) throws IOException {

        SnmpmanAgent agent = new SnmpmanAgent(new AgentConfiguration(
                "example",
                new Device(
                        "Cisco",
                        List.of(
                                new Modifier<>(".1.3.6.1.4.1.9.2.1.56", new Integer32Modifier(0, 100, 0, 100)),
                                new Modifier<>(".1.3.6.1.4.1.9.2.1.57", new Integer32Modifier(0, 100, 0, 100)),
                                new Modifier<>(".1.3.6.1.4.1.9.2.1.58", new Integer32Modifier(0, 100, 0, 100)),
                                new Modifier<>(".1.3.6.1.4.1.9.9.48.1.1.1.6", new Gauge32Modifier(1547369620, 1547374620, -30, 30)),
                                new Modifier<>(".1.3.6.1.4.1.9.9.48.1.1.1.7", new Gauge32Modifier(1547369620, 1547374620, -30, 30)),
                                new Modifier<>(".1.3.6.1.4.1.9.9.48.1.1.1.5", new Gauge32Modifier(0, 5000, -30, 30)),
                                new Modifier<>(".1.3.6.1.4.1.9.9.13.1.3.1.3", new Gauge32Modifier(25, 60, -10, 10)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.10", new Counter32Modifier(0, 4294967295L, 0, 3840000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.11", new Counter32Modifier(0, 4294967295L, 0, 2000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.12", new Counter32Modifier(0, 4294967295L, 0, 2000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.13", new Counter32Modifier(0, 4294967295L, 0, 2000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.15", new Counter32Modifier(0, 4294967295L, 0, 2000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.16", new Counter32Modifier(0, 4294967295L, 0, 3840000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.17", new Counter32Modifier(0, 4294967295L, 0, 20000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.18", new Counter32Modifier(0, 4294967295L, 0, 20000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.19", new Counter32Modifier(0, 4294967295L, 0, 20000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.20", new Counter32Modifier(0, 4294967295L, 0, 20000)),
                                new Modifier<>(".1.3.6.1.2.1.31.1.1.1.6", new Counter64Modifier(0, 1844674407370955161L, 0, 3840000)),
                                new Modifier<>(".1.3.6.1.2.1.31.1.1.1.7", new Counter64Modifier(0, 1844674407370955161L, 0, 3840000)),
                                new Modifier<>(".1.3.6.1.2.1.31.1.1.1.8", new Counter64Modifier(0, 1844674407370955161L, 0, 3840000)),
                                new Modifier<>(".1.3.6.1.2.1.31.1.1.1.9", new Counter64Modifier(0, 1844674407370955161L, 0, 3840000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.13", new Counter32Modifier(0, 4294967295L, 0, 20000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.14", new Counter32Modifier(0, 4294967295L, 0, 20000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.15", new Counter32Modifier(0, 4294967295L, 0, 20000)),
                                new Modifier<>(".1.3.6.1.2.1.31.1.1.1.10", new Counter64Modifier(0, 1844674407370955161L, 0, 3840000)),
                                new Modifier<>(".1.3.6.1.2.1.31.1.1.1.11", new Counter64Modifier(0, 1844674407370955161L, 0, 3840000)),
                                new Modifier<>(".1.3.6.1.2.1.31.1.1.1.12", new Counter64Modifier(0, 1844674407370955161L, 0, 3840000)),
                                new Modifier<>(".1.3.6.1.2.1.31.1.1.1.13", new Counter64Modifier(0, 1844674407370955161L, 0, 3840000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.19", new Counter32Modifier(0, 4294967295L, 0, 20000)),
                                new Modifier<>(".1.3.6.1.2.1.2.2.1.20", new Counter32Modifier(0, 4294967295L, 0, 20000)),
                                new Modifier<>(".1.3.6.1.2.1.17.2.4.0", new CommunityIndexCounter32Modifier(Map.of(42L, 150L, 9L, 120L)))
                        ),
                        List.of(42L, 9L)),
                "127.0.0.1", 53142, null, new File(Server.class.getResource("/").getPath())
        ));

        agent.setBindings(Walks.readWalk(new File(Server.class.getResource("/example.txt").getPath())));

        agent.execute();

        /*
         * creates a new instance of the SNMPMAN with the specified configuration file
         * and executes all agents
         */
//        Snmpman snmpman = Snmpman.start(new AgentConfiguration(
//                "example",
//                new File(Server.class.getResource("/cisco.yaml").getPath()),
//                new File(Server.class.getResource("/example.txt").getPath()),
//                "127.0.0.1",
//                53142,
//                null
//        ));
        /* ... do something with the agents */

        /* stop the SNMPMAN and all started agents */
//        snmpman.stop();
    }

}
