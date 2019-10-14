package com.amt.push.process;


import com.amt.push.consts.Constant;
import com.amt.push.udpconnector.UdpConnector;
import com.amt.push.util.OSUtils;
import com.amt.push.util.PropertyUtil;
import com.amt.push.util.RedisUtils;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * 　　* @Description: 节点监听
 * 　　* @author update ckx
 * 　　* @date 2018/9/28 16:00
 */
public class NodeLister implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(NodeLister.class);
    /**
     * 连接器.
     */
    private final UdpConnector connector;

    /**
     * 初始化连接器.
     *
     * @param udpConnector 连接器
     */
    public NodeLister(UdpConnector udpConnector) {
        connector = udpConnector;
    }


    /**
     * 当前节点编号. 以本机IP地址为节点ID
     */
    private final String NODE_ID = getHost();



    @Override
    public void run() {
        regisiterNode(); // 注册/更新节点信息
        while (true) {
            queueReport();
            try {
                //5秒上报
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 　　* @Description: 注
     * 　　* @author ckx
     * 　　* @date 2018/9/28 16:21
     */
    private void regisiterNode() {
        Jedis jedis = RedisUtils.getRedis();
        jedis.hset("IP_"+NODE_ID, "nodeId", getHost());
        jedis.hset("IP_"+NODE_ID, "ipAddress", getHost());
        jedis.hset("IP_"+NODE_ID, "nodeName", this.getNodeName() + getHost());
        jedis.hset("IP_"+NODE_ID, "redisDB", String.valueOf(Constant.REDIS_MACHINE_DATABASE));
        jedis.hset("IP_"+NODE_ID, "runEnv", this.getRunEnv());
        jedis.close();
    }

    /**
     * 　　* @Description: 获取当前服务器节点名称
     * 　　* @author ckx
     * 　　* @date 2018/9/28 16:22
     */
    private String getNodeName() {
        String nodeName = Constant.PUSH_NODE_NAME;
        return nodeName;
    }

    /**
     * 　　* @Description: 获取当前服务器节点ip
     * 　　* @author ckx
     * 　　* @date 2018/9/28 16:22
     */
    public static String getHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 　　* @Description: 获取当前服务器节点配置
     * 　　* @author ckx
     * 　　* @date 2018/9/28 16:22
     */
    private String getProperties() {
        Map<String, Properties> propMap = PropertyUtil.getAllProperties();
        return JSONObject.fromObject(propMap).toString();
    }

    /**
     * 　　* @Description: 获取服务器环境配置
     * 　　* @author ckx
     * 　　* @date 2018/9/28 16:23
     */
    public String getRunEnv() {
        StringBuilder builder = new StringBuilder();
        Properties prop = System.getProperties();
        Set<Entry<Object, Object>> sets = prop.entrySet();
        Iterator<?> it = sets.iterator();
        while (it.hasNext()) {
            builder.append(it.next()).append("\n");
        }
        return builder.toString();
    }

    /**
     * 　　* @Description: 上报节点处理信息
     * 　　* @author ckx
     * 　　* @date 2018/9/28 16:33
     */
    private void queueReport() {
        logger.info("准备上报："+NODE_ID);
        Jedis jedis = RedisUtils.getRedis();
        jedis.hset("IP_"+NODE_ID, "inQueueIn", String.valueOf(connector.getInqueueIn()));
        jedis.hset("IP_"+NODE_ID, "inQueueOut", String.valueOf(connector.getInqueueOut()));
        jedis.hset("IP_"+NODE_ID, "outQueueIn", String.valueOf(connector.getOutqueueIn()));
        jedis.hset("IP_"+NODE_ID, "outQueueOut", String.valueOf(connector.getOutqueueOut()));
//        jedis.hset("IP_"+NODE_ID, "cpuUsage", String.valueOf(OSUtils.cpuUsage()));
//        jedis.hset("IP_"+NODE_ID, "memoryUsage", String.valueOf(OSUtils.memoryUsage()));
        jedis.close();
        logger.info("上报成功：inQueueIn = "+connector.getInqueueIn()+" inQueueOut = "+connector.getInqueueOut());
    }

}
