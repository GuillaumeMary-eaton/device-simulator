import java.io.File;

import com.eaton.telemetry.AgentConfiguration;
import com.eaton.telemetry.Snmpman;

public class Server {

    public static void main(String[] args) {
        /*
         * creates a new instance of the SNMPMAN with the specified configuration file
         * and executes all agents
         */
        Snmpman snmpman = Snmpman.start(new AgentConfiguration(
                "example",
                new File(Server.class.getResource("/cisco.yaml").getPath()),
                new File(Server.class.getResource("/example.txt").getPath()),
                "127.0.0.1",
                53142,
                null
                ));
        /* ... do something with the agents */

        /* stop the SNMPMAN and all started agents */
//        snmpman.stop();
    }

}
