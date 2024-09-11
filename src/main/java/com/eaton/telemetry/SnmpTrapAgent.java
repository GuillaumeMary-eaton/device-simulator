package com.eaton.telemetry;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SnmpTrapAgent {

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
    }

    public void start() {
        Snmp snmpSession;
        try {
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmpSession = new Snmp(transport);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        executorService.scheduleAtFixedRate(() -> {
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(community));
            target.setAddress(destination);
            System.out.println("Sending traps to " + target.getAddress());
            target.setVersion(SnmpConstants.version2c);
            target.setTimeout(100); // milliseconds
            target.setRetries(2);

            Set<PDU> pdus = sensors.stream().map(sensor -> {
                PDU pdu = new PDU();
                // need to specify the system up time
                pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new OctetString(new Date().toString())));
                pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, sensor.getOid()));
                pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(sourceAddress)));
                pdu.add(new VariableBinding(sensor.getOid(), sensor.getCurrentValue()));
                pdu.setType(PDU.NOTIFICATION);
                return pdu;
            }).collect(Collectors.toSet());

            try {
                senderService.invokeAll(pdus.stream().map(pdu -> (Callable<ResponseEvent<?>>) () -> {
                    try {
                        return sendTrap(snmpSession, pdu, target);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toSet()));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, 0, 800, TimeUnit.MILLISECONDS);
    }

    private ResponseEvent<?> sendTrap(Snmp snmpSession, PDU pdu, CommunityTarget target) throws IOException {
            return snmpSession.send(pdu, target);
    }
}
