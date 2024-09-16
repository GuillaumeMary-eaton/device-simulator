package com.eaton.telemetry.modbus;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.solarnetwork.io.modbus.BitsModbusMessage;
import net.solarnetwork.io.modbus.ModbusErrorCode;
import net.solarnetwork.io.modbus.ModbusMessage;
import net.solarnetwork.io.modbus.RegistersModbusMessage;
import net.solarnetwork.io.modbus.netty.msg.BaseModbusMessage;
import net.solarnetwork.io.modbus.tcp.netty.NettyTcpModbusServer;

import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.*;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.readHoldingsResponse;

public class ModbusAgent {

    record ModbusSensor(int sensorId, RegisterType registerType, Supplier<Object> dataSupplier) {

        public Object getValue() {
            return dataSupplier.get();
        }
    }

    static {
        System.setProperty("io.netty.tryReflectionSetAccessible", "true");
    }

    private NettyTcpModbusServer server;

    private final Map<Integer, ModbusSensor> sensors = new HashMap<>();

    public ModbusAgent(int port) {
        this.server = new NettyTcpModbusServer(port);
    }

    public void addSensor(int sensorId, RegisterType registerType, Supplier<Object> dataSupplier) {
        sensors.put(sensorId, new ModbusSensor(sensorId, registerType, dataSupplier));
    }

    public void configure() {
        server.setMessageHandler((msg, sender) -> {
            ModbusMessage modbusMessage = null;
            ModbusSensor modbusSensor = sensors.get(msg.getUnitId());
            switch (msg.getFunction().blockType()) {
                case Coil, Discrete -> {
                    BitsModbusMessage tcpRequest = msg.unwrap(BitsModbusMessage.class);
                    modbusMessage = readCoilsResponse(tcpRequest.getUnitId(), tcpRequest.getAddress(), tcpRequest.getCount(), (BigInteger) modbusSensor.getValue());
                }
                case Holding -> {
                    RegistersModbusMessage registerRequest = msg.unwrap(RegistersModbusMessage.class);
                    modbusMessage = readHoldingsResponse(registerRequest.getUnitId(), registerRequest.getAddress(), (short[]) modbusSensor.getValue());
                }
                case Input -> {
                    throw new UnsupportedOperationException("Not yet implemented");
                }
                case Diagnostic -> {
                    modbusMessage = new BaseModbusMessage(msg.getUnitId(), msg.getFunction(), ModbusErrorCode.Acknowledge);
                }
            }
            sender.accept(modbusMessage);
        });
    }

    public void start() {
        try {
            server.start();
            System.out.println("Server started");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        server.stop();
    }
}
