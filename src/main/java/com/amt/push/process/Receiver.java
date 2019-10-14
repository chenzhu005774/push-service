package com.amt.push.process;

import com.amt.push.beans.ClientMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 　　* @Description: 接收者，接收机顶盒发送的消息
 * 　　* @author ckx
 * 　　* @date 2018/9/28 18:11
 */
public class Receiver implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(Receiver.class);

    protected DatagramChannel channel;

    protected int bufferSize = 1024;

    protected boolean stoped = false;
    protected ByteBuffer buffer;
    private SocketAddress address;

    private AtomicLong queueIn = new AtomicLong(0);
    private AtomicLong queueOut = new AtomicLong(0);
    protected ConcurrentLinkedQueue<ClientMessage> mq = new ConcurrentLinkedQueue<>();

    public Receiver(DatagramChannel channel) {
        this.channel = channel;
    }

    public void init() {
        buffer = ByteBuffer.allocate(this.bufferSize);
    }

    public void stop() {
        this.stoped = true;
    }

    public long getQueueIn() {
        return queueIn.longValue();
    }

    public long getQueueOut() {
        return queueOut.longValue();
    }

    public int getMq() {
        return mq.size();
    }

    @Override
    public void run() {
        while (!this.stoped) {
            try {
                processMessage();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }
    }

    /**
     * * @Description: 接收机顶盒端的UDP数据
     * * @author ckx
     * * @date 2018/12/25 15:51
     */
    protected void processMessage() throws Exception {

        address = null;
        buffer.clear();
        try {
            address = this.channel.receive(buffer);
        } catch (SocketTimeoutException timeout) {
            logger.error(timeout.getMessage());
        }
        if (address == null) {
            try {
                Thread.sleep(1);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            return;
        }
        buffer.flip();
        byte[] swap = new byte[buffer.limit() - buffer.position()];
        System.arraycopy(buffer.array(), buffer.position(), swap, 0, swap.length);
        ClientMessage m = new ClientMessage(address, swap);
        //将接收到的数据放入内存队列中
        enqueue(m);
    }

    /**
     * * @Description: 将接收到的数据放入内存队列中
     * * @author ckx
     * * @date 2018/12/25 15:54
     */
    protected boolean enqueue(ClientMessage message) {
        boolean result = mq.add(message);
        if (result == true) {
            queueIn.addAndGet(1);
        }
        return result;
    }

    protected ClientMessage dequeue() {
        ClientMessage m = mq.poll();
        if (m != null) {
            queueOut.addAndGet(1);
        }
        return m;
    }

    /**
     * * @Description: 取出内存队列中的udp数据
     * * @author ckx
     * * @date 2018/12/25 15:54
     */
    public ClientMessage receive() {
        return dequeue();
    }
}
