package com.amt.push.handler;

import com.amt.push.beans.ServerMessage;
import com.amt.push.consts.Constant;
import com.amt.push.process.Messenger;
import com.amt.push.process.NodeLister;
import com.amt.push.pushlistener.NIOPushListener;
import com.amt.push.udpconnector.UdpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;


public final class IMServer {

    private static Logger logger = LoggerFactory.getLogger(IMServer.class);

    public static IMServer server;
    private UdpConnector udpConnector;
    private NodeStatus nodeStatus = NodeStatus.getInstance();
    private ArrayList<Messenger> workerList = new ArrayList<>();
    private Thread pushThread = null;
    private NIOPushListener pushListener = null;

    private int workerNum = Constant.CLIENT_UDP_WORKER_THREAD;//fixed work threads

    private IMServer() {

    }

    public static IMServer getInstance() {
        if (server == null) {
            synchronized (IMServer.class) {
                if (server == null) {
                    server = new IMServer();
                }
            }
        }
        return server;
    }
    /**
     * * @Description: 启动服务
     * * @author ckx
     * * @date 2018/12/20 15:54
     */
    public void start() throws Exception {
        logger.info("working dir: " + System.getProperty("user.dir"));
        //*       1.监听终端（手机、第三方服务器）发送的指令或消息；
        //*       2.通过NIO方式将该消息指令（一指令一任务线程）,根据数据中iptv账号信息 查询所绑定终端机（机顶盒）ip port地址，放入ConcurrentLinkedQueue待发送的队列中；
        initPushListener();
        //*       3.启动线程循环检查待发送的队列中是否有数据， 发送指令至终端机（机顶盒）;
        //*       4.启动线程循环检查终端机（机顶盒）是否有消息指令（心跳），将消息指令放入ConcurrentLinkedQueue已接收消息的队列中;
        initUdpConnector();
        //*       5.启动N个线程循环检查处理终端机发送的消息，如果接收到了终端机消息，更新终端机状态，如果是心跳包不做回应;
        initWorkers();
        //*       6.启动线程注册当前服务器节点信息(NodeCollectPojo),检查终端机最后一次心跳包超过配置时间的终端机，注册中心清除该终端机;;
        initQueueLister();
    }


    public void initPushListener() throws Exception {
        logger.info("star msg-dispatcher listener..."); // 消息监听开启
        pushListener = new NIOPushListener();
        pushThread = new Thread(pushListener, "IMServer-push-listener");
        pushThread.start();
    }

    public void initUdpConnector() throws Exception {
        logger.info("star STB-Udp listener...");
        udpConnector = new UdpConnector();
        udpConnector.start();
    }


    public void initWorkers() {
        logger.info("star " + workerNum + " workers...");
        for (int i = 0; i < workerNum; i++) {
            Messenger worker = new Messenger(udpConnector, nodeStatus);
            workerList.add(worker);
            Thread t = new Thread(worker, "IMServer-worker-" + i);
            worker.setHostThread(t);
            t.setDaemon(true);
            t.start();
        }
    }


    public void initQueueLister() {
        logger.info("star push-server&stb-node lister...");
        NodeLister queueLister = new NodeLister(udpConnector);
        Thread queueTread = new Thread(queueLister, "IMServer-queueLister");
        queueTread.start();
    }

    public void pushInstanceMessage(ServerMessage sm) throws Exception {
        if (sm == null || sm.getData() == null || sm.getSocketAddress() == null) {
            return;
        }
        if (udpConnector != null) {
            //将待发送的数据放入内存队列中 （向机顶盒发送的数据）
            udpConnector.send(sm);
        }
    }

    /**
     * * @Description: 停止服务时
     * * @author ckx
     * * @date 2018/12/20 15:54
     */
    protected void quit() throws Exception {
        try {
            stopPushListener();
            stopUdpConnector();
            stopWorkers();
        } catch (Throwable t) {
            logger.error(t.getMessage(),t);
        }
    }


    public void stopPushListener() throws Exception {
        pushListener.stop();
        pushThread.join();
    }

    public void stopUdpConnector() throws Exception {
        if (udpConnector == null) {
            return;
        }
        udpConnector.stop();
    }

    public void stopWorkers() throws Exception {
        for (int i = 0; i < workerList.size(); i++) {
            try {
                workerList.get(i).stop();
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
            }
        }
    }



}
