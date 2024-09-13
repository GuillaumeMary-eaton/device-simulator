package com.eaton.telemetry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.solarnetwork.io.modbus.ModbusBlockType;
import net.solarnetwork.io.modbus.ModbusErrorCode;
import net.solarnetwork.io.modbus.RegistersModbusMessage;
import net.solarnetwork.io.modbus.netty.msg.BaseModbusMessage;
import net.solarnetwork.io.modbus.tcp.netty.NettyTcpModbusServer;

import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.readHoldingsResponse;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.readInputsRequest;

public class ModbusAgent {

    record ModbusSensor(int sensorId, RegisterType registerType, Supplier<short[]> dataSupplier) {

        public short[] getValue() {
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

    public void addSensor(int sensorId, RegisterType registerType, Supplier<short[]> dataSupplier) {
        sensors.put(sensorId, new ModbusSensor(sensorId, registerType, dataSupplier));
    }

    public void configure() {
        server.setMessageHandler((msg, sender) -> {
            // this handler only supports read holding registers requests
            RegistersModbusMessage req = msg.unwrap(RegistersModbusMessage.class);
            if (req != null && req.getFunction().blockType() == ModbusBlockType.Holding) {
                ModbusSensor modbusSensor = sensors.get(req.getUnitId());
                // respond with the fake data
                net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage modbusMessage = null;
                switch (req.getFunction().blockType()) {
                    case Holding, Input -> modbusMessage = readHoldingsResponse(req.getUnitId(), req.getAddress(), modbusSensor.getValue());
                    case Coil, Discrete -> {
                        modbusMessage = readInputsRequest(req.getUnitId(), req.getAddress(), 1);
                    }
                    case Diagnostic -> {
                        sender.accept(new BaseModbusMessage(msg.getUnitId(), msg.getFunction(), ModbusErrorCode.Acknowledge));
                    }
                }
//                switch (modbusSensor.registerType()) {
//                    case H -> modbusMessage = readHoldingsResponse(req.getUnitId(), req.getAddress(), modbusSensor.getValue());
//                    case I, C, D -> modbusMessage = readInputsRequest(req.getUnitId(), req.getAddress(), 1);
//                }
                sender.accept(modbusMessage);
            } else {
                // send back error that we don't handle that request
                sender.accept(new BaseModbusMessage(msg.getUnitId(), msg.getFunction(),
                        ModbusErrorCode.IllegalFunction));
            }
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
