package com.eaton.telemetry;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Representation of the configuration for a {@link SnmpmanAgent}.
 */
@Slf4j
@ToString(exclude = "community")
@EqualsAndHashCode
public class AgentConfiguration {

    /**
     * Wraps {@link Files#createTempDirectory(String, FileAttribute[])} to avoid its {@link IOException} by embedding
     * it into a {@link RuntimeException}
     *
     * @param prefix the prefix string to be used in generating the directory's name; may be null
     * @return the path to the newly created directory that did not exist before this method was invoked
     */
    private static File createTempDirectory(@Nullable String prefix) {
        try {
            return Files.createTempDirectory(prefix).toFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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
    private final InetSocketAddress address; // e.g. 127.0.0.1/8080

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
     */
    public AgentConfiguration(@Nullable String name,
                              Device device,
                              String ip,
                              int port,
                              @Nullable String community) {
        this(
                Optional.ofNullable(name).orElse(ip + ":" + port),
                new InetSocketAddress(ip, port),
                device,
                Optional.ofNullable(community).orElse("public"),
                createTempDirectory(name));
    }

    public AgentConfiguration(@Nullable String name,
                              Device device,
                              InetSocketAddress address,
                              @Nullable String community) {
        this(
                Optional.ofNullable(name).orElseGet(address::toString),
                address,
                device,
                Optional.ofNullable(community).orElse("public"),
                createTempDirectory(name));
    }

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
                new InetSocketAddress(ip, port),
                device,
                Optional.ofNullable(community).orElse("public"),
                persistenceDirectory);
    }

    public AgentConfiguration(String name, InetSocketAddress address, Device device, @Nullable String community, File persistenceDirectory) {
        this.name = name;
        this.address = address;
        this.device = device;
        this.community = community;
        this.persistenceDirectory = persistenceDirectory;
    }
}
