package com.eaton.telemetry;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;

/**
 * Representation of the configuration for a {@link SnmpmanAgent}.
 */
@Slf4j
@ToString(exclude = "community")
@EqualsAndHashCode
public class AgentConfiguration {

    /**
     * Returns the name of the agent.
     * <br>
     * If the {@code name} wasn't set on construction, the name will be defined by the {@link #address}.
     *
     * @return the name of the agent
     */
    @Getter
    private final String name;

    /**
     * Returns the address of the agent.
     *
     * @return the address of the agent
     */
    @Getter
    private final Address address; // e.g. 127.0.0.1/8080

    /**
     * Returns the {@link Device} representation for the agent.
     *
     * @return the device representation for the agent
     */
    @Getter
    private final Device device; // e.g. cisco

    /**
     * Returns the base walk file (e.g. dump of SNMP walks) for the agent.
     *
     * @return the base walk file for the agent
     */
    @Getter
    private final File persistenceDirectory; // real walk: /opt/snmpman/...

    /**
     * Returns the community for the agent.
     * <br>
     * The community is {@code public} by default.
     *
     * @return the community for the agent
     */
    @Getter
    private final String community; // e.g. 'public'

    /**
     * Constructs a new agent configuration.
     * <br>
     * The list of agent configurations will be parsed from within {@link Snmpman}.
     *
     * @param name                 the name of the agent or {@code null} to set the address as the name
     * @param device               the device
     * @param ip                   the IP the agent should bind to
     * @param port                 the port of the agent
     * @param community            the community of the agent or {@code null} will set it to {@code public}
     * @param persistenceDirectory the base directory to store Agent boot counter
     */
    public AgentConfiguration(@Nullable String name,
                              Device device,
                              String ip,
                              int port,
                              @Nullable String community,
                              File persistenceDirectory) {
        this(
                Optional.ofNullable(name).orElse(ip + ":" + port),
                GenericAddress.parse(ip + "/" + port),
                device,
                Optional.ofNullable(community).orElse("public"),
                persistenceDirectory);
    }

    public AgentConfiguration(String name, Address address, Device device, @Nullable String community, File persistenceDirectory) {
        this.name = name;
        this.address = address;
        this.device = device;
        this.community = community;
        this.persistenceDirectory = persistenceDirectory;
    }
}
