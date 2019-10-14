package com.amt.push.udpconnector;

import com.amt.push.beans.ClientMessage;
import com.amt.push.beans.ServerMessage;
import com.amt.push.consts.Constant;
import com.amt.push.process.Receiver;
import com.amt.push.process.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

public class UdpConnector {

    private static Logger logger = LoggerFactory.getLogger(UdpConnector.class);

    protected DatagramChannel antenna;//天线

    protected Receiver receiver;
    protected Sender sender;

    protected Thread receiverThread;
    protected Thread senderThread;

    protected int port = Constant.CLIENT_UDP_PORT;

    public void start() throws Exception {
        if (antenna != null) {
            throw new Exception("antenna is not null, may have run before");
        }
        antenna = DatagramChannel.open();
        antenna.socket().bind(new InetSocketAddress(port));
        logger.info("push-server udp port:" + port);
        // non-blocking
        antenna.configureBlocking(false);
        antenna.socket().setReceiveBufferSize(1024 * 1024 * Constant.CLIENT_UDP_BUFFER_RECEIVE);
        antenna.socket().setSendBufferSize(1024 * 1024 * Constant.CLIENT_UDP_BUFFER_SEND);
        logger.info("udp connector recv buffer size:" + antenna.socket().getReceiveBufferSize());
        logger.info("udp connector send buffer size:" + antenna.socket().getSendBufferSize());


        this.receiver = new Receiver(antenna);
        this.receiver.init();
        this.sender = new Sender(antenna);
        this.sender.init();

        this.senderThread = new Thread(sender, "AsynUdpConnector-sender");
        this.receiverThread = new Thread(receiver, "AsynUdpConnector-receiver");
        this.receiverThread.start();
        this.senderThread.start();
    }

    public void stop() throws Exception {
        receiver.stop();
        sender.stop();
        try {
            receiverThread.join();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        try {
            senderThread.join();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        try {
            antenna.socket().close();
        } catch (Exception e) {
//            logger.error(e.getMessage(),e);
        }
        try {
            antenna.close();
        } catch (Exception e) {
//            logger.error(e.getMessage(),e);
        }
    }

    public long getInqueueIn() {
        return this.receiver.getQueueIn();
    }

    public long getInqueueOut() {
        return this.receiver.getQueueOut();
    }

    public long getOutqueueIn() {
        return this.sender.getQueueIn();
    }

    public long getOutqueueOut() {
        return this.sender.getQueueOut();
    }

    public int getMqSize() {
        return this.receiver.getMq();
    }


    /**
     * * @Description: 取出内存队列中的数据（机顶盒端发送过来的）
     * * @author ckx
     * * @date 2018/12/25 15:54
     */
    public ClientMessage receive() throws Exception {
        return receiver.receive();
    }

    /**
     * * @Description: 将待发送的数据放入内存队列中 （向机顶盒发送的数据）
     * * @author ckx
     * * @date 2018/12/25 15:54
     */
    public boolean send(ServerMessage message) throws Exception {
        return sender.send(message);

    }

}
