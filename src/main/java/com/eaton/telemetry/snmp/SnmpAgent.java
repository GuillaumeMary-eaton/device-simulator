package com.eaton.telemetry.snmp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.BindException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.eaton.telemetry.Sensor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DefaultMOContextScope;
import org.snmp4j.agent.DefaultMOQuery;
import org.snmp4j.agent.DefaultMOServer;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOContextScope;
import org.snmp4j.agent.MOScope;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.io.ImportMode;
import org.snmp4j.agent.mo.ext.StaticMOGroup;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.TransportMappings;
import org.snmp4j.util.ThreadPool;

/**
 * This is the core class of the {@code SnmpApplication}. The agent simulates the SNMP-capable devices.
 * <br>
 * This class can be instantiated via the constructor {@link #SnmpAgent(AgentConfiguration)}, which
 * requires an instance of the {@link AgentConfiguration}.
 */
@Slf4j
public class SnmpAgent extends BaseAgent {

    /**
     * Returns the root OIDs of the bindings.
     *
     * @param bindings the variable bindings
     * @return the roots of the specified variable bindings
     */
    private static List<OID> getRoots(SortedMap<OID, Variable> bindings) {
        List<OID> potentialRoots = new ArrayList<>(bindings.size());

        OID last = null;
        for (OID oid : bindings.keySet()) {
            if (last != null) {
                int min = Math.min(oid.size(), last.size());
                while (min > 0) {
                    if (oid.leftMostCompare(min, last) == 0) {
                        OID root = new OID(last.getValue(), 0, min);
                        potentialRoots.add(root);
                        break;
                    }
                    min--;
                }
            }
            last = oid;
        }
        Collections.sort(potentialRoots);

        List<OID> roots = new ArrayList<>(potentialRoots.size());
        potentialRoots.stream().filter(potentialRoot -> potentialRoot.size() > 0).forEach(potentialRoot -> {
            OID trimmedPotentialRoot = new OID(potentialRoot.getValue(), 0, potentialRoot.size() - 1);
            while (trimmedPotentialRoot.size() > 0 && Collections.binarySearch(potentialRoots, trimmedPotentialRoot) < 0) {
                trimmedPotentialRoot.trim(1);
            }
            if (trimmedPotentialRoot.size() == 0 && !roots.contains(potentialRoot)) {
                roots.add(potentialRoot);
            }
        });

        log.trace("identified roots {}", roots);
        return roots;
    }

    /**
     * The configuration of this agent.
     */
    private final AgentConfiguration configuration;

    private final Address destination;

    /**
     * The list of managed object groups.
     */
    private final List<ManagedObject> groups = new ArrayList<>();

    private Set<Sensor<Variable>> bindings;

    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * Initializes a new instance of an SNMP agent.
     *
     * @param configuration the configuration for this agent
     */
    public SnmpAgent(AgentConfiguration configuration) {
        this(configuration, new HashSet<>());
    }

    /**
     * Initializes a new instance of an SNMP agent.
     *
     * @param configuration the configuration for this agent
     * @param sensors data to be exposed by the agent
     */
    public SnmpAgent(AgentConfiguration configuration, Set<? extends Sensor<Variable>> sensors) {
        super(new File(configuration.getPersistenceDirectory(), configuration.getName() + ".BC.cfg"),
                new File(configuration.getPersistenceDirectory(), configuration.getName() + ".Config.cfg"),
                new CommandProcessor(new OctetString(MPv3.createLocalEngineID())));
        this.agent.setWorkerPool(ThreadPool.create("RequestPool", 3));
        this.configuration = configuration;
        this.bindings = (Set<Sensor<Variable>>) sensors;
        this.destination = GenericAddress.parse("udp:" + configuration.getAddress().getHostName() + "/" + configuration.getAddress().getPort());
    }

    public void setBindings(Set<? extends Sensor<Variable>> bindings) {
        this.bindings = (Set<Sensor<Variable>>) bindings;
    }

    public SnmpAgent addBinding(String oid, Variable variable) {
        return addBinding(new OID(oid), variable);
    }

    public SnmpAgent addBinding(OID oid, Variable variable) {
        Sensor<Variable> sensor = new Sensor<>(oid, tick -> variable);
        this.bindings.add(sensor);
        return this;
    }

    /**
     * Returns the name of {@code this} agent.
     * <br>
     * See {@code AgentConfiguration.name} for more information on the return value.
     *
     * @return the name of {@code this} agent.
     */
    public String getName() {
        return configuration.getName();
    }

    /**
     * Starts this agent instance.
     *
     * @throws IOException signals that this agent could not be initialized by the {@link #init()} method
     */
    public void execute() throws IOException {
        this.init();
        this.loadConfig(ImportMode.REPLACE_CREATE);
        this.addShutdownHook();
        this.getServer().addContext(new OctetString("public"));
        this.getServer().addContext(new OctetString(""));
        // configure community index contexts
        for (Long vlan : configuration.getDevice().getVlans()) {
            this.getServer().addContext(new OctetString(String.valueOf(vlan)));
        }

        this.finishInit();
        this.run();
        this.sendColdStartNotification();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initTransportMappings() {
        log.trace("starting to initialize transport mappings for agent \"{}\"", configuration.getName());
        try {
            transportMappings = new TransportMapping[] { TransportMappings.getInstance().createTransportMapping(destination) };
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BindException) {
                throw new RuntimeException("could not bind to " + destination.getSocketAddress(), e.getCause());
            }
        }
    }

    @Override
    protected void registerManagedObjects() {
        unregisterDefaultManagedObjects(null);
        unregisterDefaultManagedObjects(new OctetString());
        List<Long> vlans = configuration.getDevice().getVlans();
        for (Long vlan : vlans) {
            unregisterDefaultManagedObjects(new OctetString(String.valueOf(vlan)));
        }

        log.trace("registering managed objects for agent \"{}\"", configuration.getName());
        for (Long vlan : vlans) {
            SortedMap<OID, Variable> variableBindings = this.getVariableBindings(new OctetString(String.valueOf(vlan)));

            OctetString context = new OctetString(String.valueOf(vlan));

            List<OID> roots = SnmpAgent.getRoots(variableBindings);
            for (OID root : roots) {
                MOGroup group = createGroup(root, variableBindings);
                Iterable<VariableBinding> subtree = generateSubtreeBindings(variableBindings, root);
                DefaultMOContextScope scope = new DefaultMOContextScope(context, root, true, root.nextPeer(), false);
                ManagedObject mo = server.lookup(new DefaultMOQuery(scope, false));
                if (mo != null) {
                    for (VariableBinding variableBinding : subtree) {
                        group = new MOGroup(variableBinding.getOid(), variableBinding.getOid(), variableBinding.getVariable());
                        scope = new DefaultMOContextScope(context, variableBinding.getOid(), true, variableBinding.getOid().nextPeer(), false);
                        mo = server.lookup(new DefaultMOQuery(scope, false));
                        if (mo != null) {
                            log.warn("could not register single OID at {} because ManagedObject {} is already registered.", variableBinding.getOid(), mo);
                        } else {
                            groups.add(group);
                            registerGroupAndContext(group, context);
                        }
                    }
                } else {
                    groups.add(group);
                    registerGroupAndContext(group, context);
                }
            }
        }
        createAndRegisterDefaultContext();
    }

    private MOGroup createGroup(OID root, SortedMap<OID, Variable> variableBindings) {
        SortedMap<OID, Variable> subtree = new TreeMap<>();
        variableBindings.entrySet().stream().filter(binding -> binding.getKey().size() >= root.size()).filter(
                binding -> binding.getKey().leftMostCompare(root.size(), root) == 0).forEach(
                        binding -> subtree.put(binding.getKey(), binding.getValue())
        );

        return new MOGroup(root, subtree);
    }

    /**
     * Creates the {@link StaticMOGroup} with all information necessary to register it to the server.
     */
    private void createAndRegisterDefaultContext() {
        SortedMap<OID, Variable> variableBindings = this.getVariableBindings(new OctetString());
        List<OID> roots = SnmpAgent.getRoots(variableBindings);
        for (OID root : roots) {
            MOGroup group = createGroup(root, variableBindings);
            registerDefaultGroups(group);
        }
    }

    /**
     * Creates a list of {@link VariableBinding} out of a mapping of {@link OID} and {@link Variable}.
     *
     * @param variableBindings mapping of {@link OID} and {@link Variable}.
     * @param root             root SNMP OID.
     * @return list of {@link VariableBinding}.
     */
    private List<VariableBinding> generateSubtreeBindings(SortedMap<OID, Variable> variableBindings, OID root) {
        return variableBindings.entrySet().stream()
                .filter(binding -> binding.getKey().size() >= root.size())
                .filter(binding -> binding.getKey().leftMostCompare(root.size(), root) == 0)
                .map(binding -> new VariableBinding(binding.getKey(), binding.getValue()))
                .toList();
    }

    /**
     * Registers a {@link ManagedObject} to the server with an empty {@link OctetString} community context.
     *
     * @param group {@link ManagedObject} to register.
     */
    private void registerDefaultGroups(MOGroup group) {
        groups.add(group);
        registerGroupAndContext(group, new OctetString(""));
    }

    /**
     * Registers a {@link ManagedObject} to the server with a {@link OctetString} community context.
     *
     * @param group   {@link ManagedObject} to register.
     * @param context community context.
     */
    private void registerGroupAndContext(MOGroup group, OctetString context) {
        try {
            if (context == null || context.toString().isEmpty()) {
                MOContextScope contextScope = new DefaultMOContextScope(new OctetString(), group.getScope());
                ManagedObject other = server.lookup(new DefaultMOQuery(contextScope, false));
                if (other != null) {
                    log.warn("group {} already existed", group);
                    return;
                }

                contextScope = new DefaultMOContextScope(null, group.getScope());
                other = server.lookup(new DefaultMOQuery(contextScope, false));
                if (other != null) {
                    registerHard(group);
                    return;
                }
                this.server.register(group, new OctetString());
            } else {
                this.server.register(group, context);
            }
        } catch (DuplicateRegistrationException e) {
            log.error("duplicate registrations are not allowed", e);
        }
    }

    /**
     * Sets the private registry value of {@link DefaultMOServer} via reflection.
     * FIXME
     * If there is any possibility to avoid this, then replace!
     *
     * @param group {@link ManagedObject} to register.
     */
    private void registerHard(MOGroup group) {
        try {
            Field registry = server.getClass().getDeclaredField("registry");
            registry.setAccessible(true);
            SortedMap<MOScope, ManagedObject<?>> reg = server.getRegistry();
            DefaultMOContextScope contextScope = new DefaultMOContextScope(new OctetString(""), group.getScope());
            reg.put(contextScope, group);
            registry.set(server, reg);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("could not set server registry", e);
        }
    }

    /**
     * Unregisters all default managed objects in the specified context {@code ctx}.
     *
     * @param ctx the context from which all default managed objects should be unregistred
     */
    private void unregisterDefaultManagedObjects(OctetString ctx) {
        OID startOID = new OID(".1");
        DefaultMOContextScope hackScope = new DefaultMOContextScope(ctx, startOID, true, startOID.nextPeer(), false);
        ManagedObject query;
        while ((query = server.lookup(new DefaultMOQuery(hackScope, false))) != null) {
            server.unregister(query, ctx);
        }
    }

    /**
     * Returns the variable bindings for a device configuration and a list of bindings.
     *
     * @return the variable bindings for the specified device configuration
     */
    @SuppressWarnings("unchecked")
    private SortedMap<OID, Variable> getVariableBindings(OctetString context) {
        log.trace("get variable bindings for agent \"{}\"", configuration.getName());
        int tick = counter.getAndIncrement();
        SortedMap<OID, Variable> result = new TreeMap<>();
        for (Sensor<Variable> binding : bindings) {
            log.trace("created modified variable for OID {}", binding.getOid());
            result.put(binding.getOid(), binding.getValue(tick));
        }
        return result;
    }

    @Override
    protected void unregisterManagedObjects() {
        log.trace("unregistered managed objects for agent \"{}\"", agent);
        for (ManagedObject mo : groups) {
            server.unregister(mo, null);
        }
    }

    @Override
    protected void addUsmUser(USM usm) {
        log.trace("adding usm user {} for agent \"{}\"", usm.toString(), configuration.getName());
        // do nothing here
    }

    @Override
    protected void addNotificationTargets(SnmpTargetMIB snmpTargetMIB, SnmpNotificationMIB snmpNotificationMIB) {
        log.trace("adding notification targets {}, {} for agent \"{}\"", snmpTargetMIB.toString(), snmpNotificationMIB.toString(), configuration.getName());
        // do nothing here
    }

    @Override
    protected void addViews(VacmMIB vacmMIB) {
        log.trace("adding views in the vacm MIB {} for agent \"{}\"", vacmMIB.toString(), configuration.getName());
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_SNMPv1, new OctetString(configuration.getCommunity()), new OctetString("v1v2group"), StorageType.nonVolatile);
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(configuration.getCommunity()), new OctetString("v1v2group"), StorageType.nonVolatile);
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("SHADES"), new OctetString("v3group"), StorageType.nonVolatile);
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("TEST"), new OctetString("v3test"), StorageType.nonVolatile);
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("SHA"), new OctetString("v3restricted"), StorageType.nonVolatile);
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("v3notify"), new OctetString("v3restricted"), StorageType.nonVolatile);

        // configure community index contexts
        for (Long vlan : configuration.getDevice().getVlans()) {
            vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_SNMPv1, new OctetString(configuration.getCommunity() + "@" + vlan), new OctetString("v1v2group"), StorageType.nonVolatile);
            vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(configuration.getCommunity() + "@" + vlan), new OctetString("v1v2group"), StorageType.nonVolatile);
            vacmMIB.addAccess(new OctetString("v1v2group"), new OctetString(String.valueOf(vlan)), SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"), new OctetString("fullWriteView"), new OctetString("fullNotifyView"), StorageType.nonVolatile);
        }

        vacmMIB.addAccess(new OctetString("v1v2group"), new OctetString(), SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"), new OctetString("fullWriteView"), new OctetString("fullNotifyView"), StorageType.nonVolatile);
        vacmMIB.addAccess(new OctetString("v3group"), new OctetString(), SecurityModel.SECURITY_MODEL_USM, SecurityLevel.AUTH_PRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"), new OctetString("fullWriteView"), new OctetString("fullNotifyView"), StorageType.nonVolatile);
        vacmMIB.addAccess(new OctetString("v3restricted"), new OctetString(), SecurityModel.SECURITY_MODEL_USM, SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("restrictedReadView"), new OctetString("restrictedWriteView"), new OctetString("restrictedNotifyView"), StorageType.nonVolatile);
        vacmMIB.addAccess(new OctetString("v3test"), new OctetString(), SecurityModel.SECURITY_MODEL_USM, SecurityLevel.AUTH_PRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("testReadView"), new OctetString("testWriteView"), new OctetString("testNotifyView"), StorageType.nonVolatile);

        vacmMIB.addViewTreeFamily(new OctetString("fullReadView"), new OID("1"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("fullWriteView"), new OID("1"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("fullNotifyView"), new OID("1"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

        vacmMIB.addViewTreeFamily(new OctetString("restrictedReadView"), new OID("1.3.6.1.2"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("restrictedWriteView"), new OID("1.3.6.1.2.1"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("restrictedNotifyView"), new OID("1.3.6.1.2"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("restrictedNotifyView"), new OID("1.3.6.1.6.3.1"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

        vacmMIB.addViewTreeFamily(new OctetString("testReadView"), new OID("1.3.6.1.2"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("testReadView"), new OID("1.3.6.1.2.1.1"), new OctetString(), VacmMIB.vacmViewExcluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("testWriteView"), new OID("1.3.6.1.2.1"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("testNotifyView"), new OID("1.3.6.1.2"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
    }

    @Override
    protected void addCommunities(SnmpCommunityMIB snmpCommunityMIB) {
        log.trace("adding communities {} for agent \"{}\"", snmpCommunityMIB.toString(), configuration.getName());
        // configure community index contexts
        for (Long vlan : configuration.getDevice().getVlans()) {
            configureSnmpCommunity(snmpCommunityMIB, vlan);
        }
        configureSnmpCommunity(snmpCommunityMIB, null);
    }

    /**
     * Configures an SNMP community for a given SNMP community context.
     *
     * @param snmpCommunityMIB SNMP community.
     * @param context          SNMP community context.
     */
    private void configureSnmpCommunity(SnmpCommunityMIB snmpCommunityMIB, Long context) {
        String communityString;
        OctetString contextName;
        if (context != null) {
            communityString = configuration.getCommunity() + "@" + context;
            contextName = new OctetString(String.valueOf(context));
        } else {
            communityString = configuration.getCommunity();
            contextName = new OctetString();
        }
        Variable[] com2sec = new Variable[]{
                new OctetString(communityString),       // community name
                new OctetString(communityString),       // security name
                getAgent().getContextEngineID(),        // local engine ID
                contextName,                            // default context name
                new OctetString(),                      // transport tag
                new Integer32(StorageType.readOnly),    // storage type
                new Integer32(RowStatus.active)         // row status
        };
        SnmpCommunityMIB.SnmpCommunityEntryRow row = snmpCommunityMIB.getSnmpCommunityEntry().createRow(
                new OctetString(communityString + "2" + communityString).toSubIndex(true), com2sec);
        snmpCommunityMIB.getSnmpCommunityEntry().addRow(row);
    }

    /**
     * Wait until specified agent is started.
     * <br>
     * A call of this method is blocking.
     *
     * @throws InitializationException if the specified agent is already stopped
     */
    public void waitForStartup() {
        if (getAgentState() == STATE_STOPPED) {
            throw new InitializationException("agent " + getName() + " already stopped while initialization was running");
        } else if (getAgentState() != STATE_RUNNING) {
            try {
                Thread.sleep(100L);
                waitForStartup();
            } catch (InterruptedException e) {
                log.warn("wait was interrupted", e);
            }
        }
    }
}
