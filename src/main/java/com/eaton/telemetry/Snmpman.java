package com.eaton.telemetry;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.agent.BaseAgent;

/**
 * This is the library interface for Snmpman.
 * <br>
 * Each configuration list item represents an instance of the {@link AgentConfiguration}.
 * The constructor {@link AgentConfiguration#AgentConfiguration(String, java.io.File, java.io.File, String, int, String)}
 * lists all available properties, which may or may not be required.
 * <br>
 * An entry may look like the following:
 * <pre>
 * {@code
 *     - name: "example1"
 *       device: "src/test/resources/configuration/cisco.yaml"
 *       walk: "src/test/resources/configuration/example.txt"
 *       ip: "127.0.0.1"
 *       port: 10000
 * }
 * </pre>
 * You can find more example within the test resources of this project.
 * <br>
 * The configuration {@code YAML} file defines a list of all agents that should be simulated by the {@code Snmpman}.
 */
@Slf4j
public class Snmpman {

    /**
     * Creates an {@code Snmpman} instance by the specified configuration in the {@code configurationFile} and starts all agents.
     *
     * @param configurationFile the configuration
     * @return the {@code Snmpman} instance
     * @throws InitializationException thrown if any agent, as specified in the configuration, could not be started
     */
    public static Snmpman start(File configurationFile) {
        Preconditions.checkNotNull(configurationFile, "the configuration file may not be null");
        Preconditions.checkArgument(configurationFile.exists() && configurationFile.isFile(), "configuration does not exist or is not a file");

        log.debug("started with configuration in path {}", configurationFile.getAbsolutePath());
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            AgentConfiguration[] configurations = mapper.readValue(configurationFile, AgentConfiguration[].class);

            return start(configurations);
        } catch (IOException e) {
            throw new InitializationException("could not parse configuration at path: " + configurationFile.getAbsolutePath(), e);
        }
    }

    /**
     * Creates an {@code Snmpman} instance by the specified configuration in the {@code configurationFile} and starts all agents.
     *
     * @param configurations the configurations
     * @return the {@code Snmpman} instance
     * @throws InitializationException thrown if any agent, as specified in the configuration, could not be started
     */
    public static Snmpman start(AgentConfiguration ... configurations) {
        return Snmpman.start(Arrays.stream(configurations).map(SnmpmanAgent::new).collect(Collectors.toList()));
    }

    /**
     * Creates a {@code Snmpman} instance with the specified list of agents and starts all agents.
     *
     * @param agents the list of agents
     * @return the {@code Snmpman} instance
     * @throws InitializationException thrown if any agent, as specified in the configuration, could not be started
     */
    public static Snmpman start(List<SnmpmanAgent> agents) {
        Snmpman snmpman = new Snmpman(Collections.unmodifiableList(agents));
        snmpman.start();
        return snmpman;
    }

    /**
     * Returns the list of SNMP agents for {@code this} instance.
     *
     * @return the list of SNMP agents
     */
    @Getter private final List<SnmpmanAgent> agents;

    /**
     * Constructs an instance by the specified list of agents.
     *
     * @param agents the agents for {@code this} instance
     */
    private Snmpman(List<SnmpmanAgent> agents) {
        this.agents = agents;
    }

    public SnmpmanAgent getAgent(String name) {
        return agents.stream().filter(agent -> agent.getName().equals(name)).findFirst().orElse(null);
    }

    /**
     * Starts all agents as defined in {@link #agents}.
     *
     * @throws InitializationException thrown if any agent could not be started
     */
    private void start() {
        log.debug("starting to load agents");
        for (SnmpmanAgent agent : agents) {
            try {
                agent.execute();
            } catch (IOException e) {
                throw new InitializationException("failed to start agent \"" + agent.getName() + "\"", e);
            }
        }
        log.debug("all agents initialized");
        agents.forEach(this::checkStatus);
        log.info("all agents are running");
    }

    /**
     * Wait until specified agent is started.
     * <br>
     * A call of this method is blocking.
     *
     * @param agent the agent to wait for
     * @throws InitializationException if the specified agent is already stopped
     */
    private void checkStatus(SnmpmanAgent agent) {
        if (agent.getAgentState() == BaseAgent.STATE_STOPPED) {
            throw new InitializationException("agent " + agent.getName() + " already stopped while initialization was running");
        } else if (agent.getAgentState() != BaseAgent.STATE_RUNNING) {
            try {
                Thread.sleep(100L);
                checkStatus(agent);
            } catch (InterruptedException e) {
                log.warn("wait was interrupted", e);
            }
        }
    }

    /** Stops all agents as defined in {@link #agents}. */
    public void stop() {
        agents.forEach(SnmpmanAgent::stop);
    }
}
