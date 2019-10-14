/*
 *Copyright 2014 DDPush
 *Author: AndyKwok(in English) GuoZhengzhu(in Chinese)
 *Email: ddpush@126.com
 *

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package com.amt.push.pushlistener;

import com.amt.push.consts.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 　　* @Description: NIO模式Scoket通信
 * 　　* @author update ckx
 * 　　* @date update 2018/9/26 17:04 添加注释 格式化代码
 * <p>
 * 1.初始化线程池，初始化selector
 * 2.注册ServerSocketChannel，接收呼入的请求
 * 3.遍历获取已就绪的事件
 * 4.接受客户端连接请求，与客户端建立连接
 * 5.注册SocketChanne，具体的业务处理，关注客户端写入时间，读取是否就绪
 * 6.执行任务事件
 */
public class NIOPushListener implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(NIOPushListener.class);

    private static int sockTimout = 1000 * Constant.PUSH_LISTENER_SOCKET_TIMEOUT;//获取推送消息超时时间
    private static int port = Constant.PUSH_LISTENER_PORT;//监听SCOKET请求端口

    private boolean stoped = false;

    ServerSocketChannel channel = null;//套接字的通道
    private Selector selector = null;//选择器

    private ExecutorService executor;//线程池

    private int minThreads = Constant.PUSH_LISTENER_MIN_THREAD;//最小线程数
    private int maxThreads = Constant.PUSH_LISTENER_MAX_THREAD;//最大线程数
    //并发线程安全队列。这里主要用来存放需要执行的线程，这里的线程主要执行
    protected ConcurrentLinkedQueue<Runnable> events = new ConcurrentLinkedQueue<>();

    /**
     * 　　* @Description: 初始化
     * 　　* @author ckx
     * 　　* @date 2018/9/26 17:17
     */
    public void init() throws Exception {
        initExecutor();
        initChannel();

    }

    /**
     * 　　* @Description: 初始化通信管道和选择器
     * 　　* @author ckx
     * 　　* @date 2018/9/27 14:17
     */
    public void initChannel() throws Exception {
        channel = ServerSocketChannel.open();
        channel.socket().bind(new InetSocketAddress(port));
        channel.configureBlocking(false);
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }


    /**
     * 　　* @Description: 初始化线程池
     * 　　* @author ckx
     * 　　* @date 2018/9/27 14:17
     */
    public void initExecutor() throws Exception {
        if (executor == null) {
            executor = new ThreadPoolExecutor(minThreads, maxThreads, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        }
    }

    @Override
    public void run() {
        InetAddress addr = null;
        try {
            init();
            addr = InetAddress.getLocalHost();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
        }
        String ip = addr.getHostAddress(); //获取本机ip
        logger.info("push-server ip:" + ip);
        logger.info("push-server port:" + port);

        while (!stoped && selector != null) {
            try {
                handleEvent();//执行任务
                handleTimeout();
                handleChannel();
            } catch (ClosedSelectorException cse) {
                logger.error(cse.getMessage(), cse);
            } catch (CancelledKeyException nx) {
                logger.error(nx.getMessage(), nx);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }

        closeSelector();
        stopExecutor();

    }


    /**
     * 　　* @Description: 添加线程节点事件
     * 　　* @author ckx
     * 　　* @date 2018/9/27 16:38
     */
    public void addEvent(Runnable event) {
        if (selector == null) {
            return;
        }
        events.add(event);

        if (!stoped && selector != null) {
            selector.wakeup();//唤起返回阻塞在select()方法的线程
        }
    }


    public void stop() {
        this.stoped = true;
        if (this.selector != null) {
            try {
                selector.wakeup();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void stopExecutor() {
        try {
            if (executor != null) executor.shutdownNow();//ignore left overs
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        executor = null;
    }

    private void closeSelector() {
        if (selector != null) {
            try {
                selector.wakeup();
                selector.close();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                selector = null;
            }
        }
    }

    /**
     * 　　* @Description: 处理线程队列
     * 　　* @author ckx
     * 　　* @date 2018/9/27 14:39
     */
    private void handleEvent() {
        Runnable r;
        while (true) {
            r = events.poll();//获取线程并且在队列中移除，如果队列为空返回null
            if (r == null) {
                return;
            }
            try {
                r.run();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 　　* @Description: 处理超时和已关闭通道
     * 　　* @author ckx
     * 　　* @date 2018/9/27 15:06
     */
    private void handleTimeout() {
        Selector tmpsel = selector;
        Set keys = (!stoped && tmpsel != null) ? tmpsel.keys() : null;
        if (keys == null) {
            return;
        }
        Iterator it = keys.iterator();
        long now = System.currentTimeMillis();
        while (it.hasNext()) {//遍历选择器中所有通道
            SelectionKey key = (SelectionKey) it.next();
            if (key.channel() instanceof ServerSocketChannel) {
                continue;
            }
            if (!key.isValid()) {//该通道是否关闭
                continue;
            }
            try {
                PushTask task = (PushTask) key.attachment();//获取SelectionKey中的任务
                if (task == null) {
                    cancelKey(key);
                    continue;
                }
                if (!task.isWritePending() && now - task.getLastActive() > sockTimout) {
                    cancelKey(key);
                }
            } catch (CancelledKeyException e) {
                cancelKey(key);
            }
        }
    }

    /**
     * 　　* @Description: 处理通道  将所读取的缓冲区数据放入Sender队列中
     * 　　* @author ckx
     * 　　* @date 2018/9/27 16:15
     */
    private void handleChannel() throws Exception {
        if (selector.select() == 0) {//如果该选择器没有就绪的通道
            try {
                Thread.sleep(1);//触发操作系统立刻重新进行一次CPU竞争
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            return;
        }

        //已就绪的通道
        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
        while (it.hasNext()) {
            SelectionKey key = it.next();
            it.remove();//删除已选的key,以防重复处理
            //服务器监听到了新的客户连接
            if (key.isAcceptable()) {
                try {
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel channel = server.accept();//接受客户端连接请求，与客户端建立连接
                    channel.configureBlocking(false);
                    channel.socket().setSoTimeout(sockTimout);
                    PushTask task = new PushTask(this, channel);
                    channel.register(selector, SelectionKey.OP_READ, task);//在选择器中注册该通道读取事件
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            //读或写就绪
            if (key.isReadable() || key.isWritable()) {
                try {
                    PushTask task = (PushTask) key.attachment();
                    if (task == null) {//this should never happen
                        cancelKey(key);
                        continue;
                    }
                    task.setKey(key);
                    executor.execute(task);//执行注册的任务事件 PushTask.run()
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

        }
    }

    /**
     * 　　* @Description: 断开该通道连接
     * 　　* @author ckx
     * 　　* @date 2018/9/27 16:31
     */
    public static void cancelKey(SelectionKey key) {
        if (key == null) return;

        key.cancel();
        try {
            ((SocketChannel) key.channel()).socket().close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        try {
            key.channel().close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }


}
