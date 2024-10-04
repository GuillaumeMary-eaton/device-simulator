package com.eaton.telemetry.modbus;

import  java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import net.solarnetwork.io.modbus.BitsModbusMessage;
import net.solarnetwork.io.modbus.ModbusErrorCode;
import net.solarnetwork.io.modbus.ModbusMessage;
import net.solarnetwork.io.modbus.RegistersModbusMessage;
import net.solarnetwork.io.modbus.netty.msg.BaseModbusMessage;
import net.solarnetwork.io.modbus.tcp.netty.NettyTcpModbusServer;

import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.readCoilsResponse;
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.readDiscretesResponse;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.readHoldingsResponse;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.readInputsResponse;

public class ModbusAgent {

    private NettyTcpModbusServer server;

    private final Map<Integer, ModbusSensor> sensors;

    public ModbusAgent(int port) {
        this(port, new HashSet<>());
    }

    public ModbusAgent(int port, Set<? extends ModbusSensor<?>> sensors) {
        this.server = new NettyTcpModbusServer(port);
        this.sensors = sensors.stream().collect(Collectors.toMap(ModbusSensor::getIdentifier, s -> s));
    }

    public void addSensor(int sensorId, RegisterType registerType, IntFunction<?> dataSupplier) {
        sensors.put(sensorId, new ModbusSensor<>(sensorId, registerType, dataSupplier));
    }

    public void configure() {
        server.setMessageHandler((msg, sender) -> {
            ModbusMessage modbusMessage = null;
            ModbusSensor<?> modbusSensor = sensors.get(msg.getUnitId());
            final Object sensorValue = modbusSensor.nextValue();
            if (sensorValue == null) {
                modbusMessage = new BaseModbusMessage(msg.getUnitId(), msg.getFunction(), ModbusErrorCode.ServerDeviceFailure);
                sender.accept(modbusMessage);
                return;
            } else {
                switch (msg.getFunction().blockType()) {
                    case Coil -> {
                        BitsModbusMessage tcpRequest = msg.unwrap(BitsModbusMessage.class);
                        modbusMessage = readCoilsResponse(tcpRequest.getUnitId(), tcpRequest.getAddress(), tcpRequest.getCount(), (BigInteger) sensorValue);
                    }
                    case Discrete -> {
                        BitsModbusMessage tcpRequest = msg.unwrap(BitsModbusMessage.class);
                        modbusMessage = readDiscretesResponse(tcpRequest.getUnitId(), tcpRequest.getAddress(), tcpRequest.getCount(), (BigInteger) sensorValue);
                    }
                    case Holding -> {
                        RegistersModbusMessage registerRequest = msg.unwrap(RegistersModbusMessage.class);
                        modbusMessage = readHoldingsResponse(registerRequest.getUnitId(), registerRequest.getAddress(), (short[]) sensorValue);
                    }
                    case Input -> {
                        RegistersModbusMessage registerRequest = msg.unwrap(RegistersModbusMessage.class);
                        modbusMessage = readInputsResponse(registerRequest.getUnitId(), registerRequest.getAddress(), (short[]) sensorValue);
                    }
                    case Diagnostic -> {
                        modbusMessage = new BaseModbusMessage(msg.getUnitId(), msg.getFunction(), ModbusErrorCode.Acknowledge);
                    }
                }
            }
            sender.accept(modbusMessage);
        });
    }

    public void start() {
        try {
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        server.stop();
    }
}
