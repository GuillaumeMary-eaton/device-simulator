package com.eaton.telemetry.modbus;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.solarnetwork.io.modbus.BitsModbusMessage;
import net.solarnetwork.io.modbus.ModbusErrorCode;
import net.solarnetwork.io.modbus.ModbusMessage;
import net.solarnetwork.io.modbus.RegistersModbusMessage;
import net.solarnetwork.io.modbus.netty.msg.BaseModbusMessage;
import net.solarnetwork.io.modbus.tcp.netty.NettyTcpModbusServer;

import static net.solarnetwork.io.modbus.ModbusFunctionCodes.WRITE_COIL;
import static net.solarnetwork.io.modbus.ModbusFunctionCodes.WRITE_COILS;
import static net.solarnetwork.io.modbus.ModbusFunctionCodes.WRITE_HOLDING_REGISTER;
import static net.solarnetwork.io.modbus.ModbusFunctionCodes.WRITE_HOLDING_REGISTERS;
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.readCoilsResponse;
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.readDiscretesResponse;
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.writeCoilResponse;
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.writeCoilsResponse;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.readHoldingsResponse;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.readInputsResponse;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.writeHoldingResponse;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.writeHoldingsResponse;

@Slf4j
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
            log.info("Reading value of sensor: {}", msg.getUnitId());
            ModbusSensor<?> modbusSensor = sensors.get(msg.getUnitId());
            final Object sensorValue = modbusSensor.nextValue();
            if (sensorValue == null) {
                modbusMessage = new BaseModbusMessage(msg.getUnitId(), msg.getFunction(), ModbusErrorCode.ServerDeviceFailure);
                sender.accept(modbusMessage);
                return;
            } else {
                if (msg.getFunction().isReadFunction()) {
                    modbusMessage = handleRead(msg, sensorValue);
                } else {
                    modbusMessage = handleWrite(msg, sensorValue);
                }
            }
            sender.accept(modbusMessage);
        });
    }

    private ModbusMessage handleRead(ModbusMessage msg, Object sensorValue) {
        ModbusMessage modbusMessage = null;
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
        return modbusMessage;
    }

    private ModbusMessage handleWrite(ModbusMessage msg, Object sensorValue) {
        ModbusMessage modbusMessage = null;
        switch (msg.getFunction().blockType()) {
            // Modbus protocol only supports write for coils and holding registers
            case Coil -> {
                BitsModbusMessage tcpRequest = msg.unwrap(BitsModbusMessage.class);
                if (tcpRequest.getFunction().getCode() == WRITE_COILS) {
                    modbusMessage = writeCoilsResponse(tcpRequest.getUnitId(), tcpRequest.getAddress(), (int) sensorValue);
                } else if (tcpRequest.getFunction().getCode() == WRITE_COIL) {
                    modbusMessage = writeCoilResponse(tcpRequest.getUnitId(), tcpRequest.getAddress(), (boolean) sensorValue);
                }
            }
            case Holding -> {
                RegistersModbusMessage registerRequest = msg.unwrap(RegistersModbusMessage.class);
                if (registerRequest.getFunction().getCode() == WRITE_HOLDING_REGISTERS) {
                    modbusMessage = writeHoldingsResponse(registerRequest.getUnitId(), registerRequest.getAddress(), (int) sensorValue);
                } else if (registerRequest.getFunction().getCode() == WRITE_HOLDING_REGISTER) {
                    modbusMessage = writeHoldingResponse(registerRequest.getUnitId(), registerRequest.getAddress(), (int) sensorValue);
                }
            }
        }
        return modbusMessage;
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
