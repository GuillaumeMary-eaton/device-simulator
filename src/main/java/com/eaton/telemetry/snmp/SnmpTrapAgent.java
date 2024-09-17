package com.eaton.telemetry.snmp;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

@Slf4j
public class SnmpTrapAgent {

    private Snmp snmpSession;

    private static InetAddress getLocalHost() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private final ScheduledExecutorService executorService;

    private final ExecutorService senderService = Executors.newFixedThreadPool(3);
    private final Set<Sensor> sensors;
    private final Address destination;
    private final InetAddress sourceAddress;

    private final String community;

    /**
     * Basic constructor that creates a Snmp Trap sender Agent without sensor.
     * Sensors can be added later with {@link #addSensor(OID, Supplier, Duration, Duration)}
     *
     * @param configuration
     */
    public SnmpTrapAgent(AgentConfiguration configuration) {
        this(configuration, new LinkedHashSet<>());
    }

    public SnmpTrapAgent(AgentConfiguration configuration, Set<Sensor> sensors) {
        this(GenericAddress.parse("udp:" + configuration.getAddress().getHostName() + "/" + configuration.getAddress().getPort()), configuration.getCommunity(), sensors);
    }

    public SnmpTrapAgent(Address destination, @Nullable String community, Set<Sensor> sensors) {
        this(destination, getLocalHost(), community, sensors);
    }

    public SnmpTrapAgent(Address destination, InetAddress sourceAddress, @Nullable String community, Set<Sensor> sensors) {
        this.executorService = Executors.newScheduledThreadPool(1);
        this.sensors = sensors;
        this.destination = destination;
        this.community = community;
        this.sourceAddress = sourceAddress;
        try {
            this.snmpSession = new Snmp(new DefaultUdpTransportMapping());
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a {@link Sensor} to this agent.
     * This action must be made before calling {@link #start()}
     *
     * @param oid the Oid of the sensor
     * @param valueGenerator the generator of the values
     * @param period the rhythm at which the values must be sent
     * @param initialDelay the delay before sending the first data, starting from {@link #start()} call
     * @return this to eventually chain sensor addition
     */
    public SnmpTrapAgent addSensor(OID oid, Supplier<Variable> valueGenerator, Duration period, Duration initialDelay) {
        sensors.add(new Sensor<>(oid, valueGenerator, period, initialDelay));
        return this;
    }

    /**
     * Starts this agent and sends trap values (coming from sensors) to the destination.
     */
    public void start() {
        sensors.forEach(sensor -> {
            executorService.scheduleAtFixedRate(() -> {
                CommunityTarget target = new CommunityTarget();
                target.setCommunity(new OctetString(community));
                target.setAddress(destination);
                log.debug("Sending traps to " + target.getAddress());
                target.setVersion(SnmpConstants.version2c);
                target.setTimeout(100); // milliseconds
                target.setRetries(2);

                PDU pdu = new PDU();
                // need to specify the system up time
                pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new OctetString(new Date().toString())));
                pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, sensor.getOid()));
                pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(sourceAddress)));
                pdu.add(new VariableBinding(sensor.getOid(), sensor.getValue()));
                pdu.setType(PDU.NOTIFICATION);

                senderService.submit(() -> {
                    try {
                        return sendTrap(pdu, target);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }, sensor.getInitialDelay().toMillis(), sensor.getPeriod().toMillis(), TimeUnit.MILLISECONDS);
        });
    }

    private ResponseEvent<?> sendTrap(PDU pdu, CommunityTarget target) throws IOException {
        return this.snmpSession.send(pdu, target);
    }
}
