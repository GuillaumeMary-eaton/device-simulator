import java.io.IOException;

import net.solarnetwork.io.modbus.ModbusBlockType;
import net.solarnetwork.io.modbus.ModbusErrorCode;
import net.solarnetwork.io.modbus.RegistersModbusMessage;
import net.solarnetwork.io.modbus.netty.msg.BaseModbusMessage;
import net.solarnetwork.io.modbus.tcp.netty.NettyTcpModbusServer;

import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.readHoldingsResponse;

public class ModbusServer {

    public static void main(String[] args) {
        NettyTcpModbusServer server = new NettyTcpModbusServer(54321);
        server.setMessageHandler((msg, sender) -> {
            // this handler only supports read holding registers requests
            RegistersModbusMessage req = msg.unwrap(RegistersModbusMessage.class);
            if (req != null && req.getFunction().blockType() == ModbusBlockType.Holding) {

                // generate some fake data that matches the request register count
                short[] resultData = new short[req.getCount()];
                for (int i = 0; i < resultData.length; i++) {
                    resultData[i] = (short) i;
                }

                // respond with the fake data
                sender.accept(readHoldingsResponse(req.getUnitId(), req.getAddress(), resultData));
            } else {
                // send back error that we don't handle that request
                sender.accept(new BaseModbusMessage(msg.getUnitId(), msg.getFunction(),
                        ModbusErrorCode.IllegalFunction));
            }
        });

        try {
            server.start();
            while (true) {
                Thread.sleep(60_000);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            server.stop();
        }
    }
}
