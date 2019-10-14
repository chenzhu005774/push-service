package com.amt.push.process;


import com.amt.push.beans.ServerMessage;
import com.amt.push.consts.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 　　* @Description: 消息发送者，向终端机发送指令消息
 * 　　* @author ckx
 * 　　* @date 2018/9/28 18:10
 */
public class Sender implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(Sender.class);
    //UDP 通道
    protected DatagramChannel channel;
    //数据包收包计数器
    private AtomicLong queueIn = new AtomicLong(0);
    //数据包 发包计数器
    private AtomicLong queueOut = new AtomicLong(0);
    //定义 UDP 发射的数据包的最大缓冲区大小
    protected int bufferSize = Constant.PUSH_MSG_HEADER_LEN + Constant.PUSH_MSG_MAX_CONTENT_LEN;
    //服务器是否被暂停标志
    protected boolean stoped = false;
    //缓冲区
    protected ByteBuffer buffer;
    //并发线程安全队列。这里主要用来存放需要下发的消息
    protected ConcurrentLinkedQueue<ServerMessage> mq = new ConcurrentLinkedQueue<>();

    public Sender(DatagramChannel channel) {
        this.channel = channel;
    }

    public long getQueueIn() {
        return queueIn.longValue();
    }

    public long getQueueOut() {
        return queueOut.longValue();
    }

    public void init() {
        buffer = ByteBuffer.allocate(bufferSize);
    }

    public void stop() {
        this.stoped = true;
    }

    @Override
    public void run() {
        try {
            while (mq.isEmpty() && !stoped) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            //如果有消息了,就开始处理数据
            processMessage();

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

    /**
     * * @Description: 向机顶盒发送UDP消息命令
     * * @author ckx
     * * @date 2018/12/28 16:32
     */
    protected void processMessage() throws Exception {
        buffer.clear();
        //从并发线程安全的消息队列中取出一个被封装的待发射的UDP消息
        ServerMessage pendingMessage = dequeue();
        if (pendingMessage == null) {
            return;
        }
        buffer.put(pendingMessage.getData());
        buffer.flip();
        //发射这个UDP数据包
        channel.send(buffer, pendingMessage.getSocketAddress());
    }

    protected boolean enqueue(ServerMessage message) {
        boolean result = mq.add(message);
        if (result) {
            queueIn.addAndGet(1);
        }
        return result;
    }

    /**
     * * @Description: 取出内存队列中的待发送数据
     * * @author ckx
     * * @date 2018/12/25 15:54
     */
    protected ServerMessage dequeue() {
        ServerMessage m = mq.poll();
        if (m != null) {
            queueOut.addAndGet(1);
        }
        return m;
    }

    /**
     * * @Description: 将待发送的数据放入内存队列中
     * * @author ckx
     * * @date 2018/12/25 15:54
     */
    public boolean send(ServerMessage message) {
        return enqueue(message);
    }
}