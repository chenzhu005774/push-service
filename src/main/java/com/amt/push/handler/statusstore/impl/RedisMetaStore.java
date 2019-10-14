package com.amt.push.handler.statusstore.impl;

import com.amt.push.beans.ClientMessage;
import com.amt.push.beans.ClientStatMachine;
import com.amt.push.consts.Constant;
import com.amt.push.handler.statusstore.MetaStore;
import com.amt.push.process.NodeLister;
import com.amt.push.util.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

/**
 * @Auther: ckx
 * @Date: 2019/1/29 09:44
 * @Description:
 */
public class RedisMetaStore implements MetaStore {

    private static Logger logger = LoggerFactory.getLogger(RedisMetaStore.class);
    /**
     * 清理时间.5min
     */
    private int expiredSeconds = Constant.CLEANER_DEFAULT_EXPIRED_SECONDS;
    /**
     * 当前节点编号.
     */
    private final String NODE_ID = NodeLister.getHost();

    private volatile static RedisMetaStore instance;

    public static RedisMetaStore create() {
        if (null == instance) {
            synchronized (RedisMetaStore.class) {
                if (null == instance) {
                    instance = new RedisMetaStore();
                }
            }
        }
        return instance;
    }


    @Override
    public void put(String k, ClientStatMachine v) {
        upToDate(k, v);
    }

    @Override
    public ClientStatMachine get(String uuid) {
        Jedis jedis = RedisUtils.getRedis ();
        Object ip = jedis.hget(uuid, "ip");
        Object port = jedis.hget(uuid, "port");
        jedis.close();
        if (ip == null || port == null) {
            return null;
        }
        ClientStatMachine csm = ClientStatMachine.newByClientStatMachinePojo(ip.toString(), Integer.valueOf(port.toString()));
        return csm;
    }

    @Override
    public ClientStatMachine get(String uuid, ClientMessage m) {
        Jedis jedis = RedisUtils.getRedis ();
        Object ip = jedis.hget(uuid, "ip");
        Object port = jedis.hget(uuid, "port");
        jedis.close();
        if (ip == null || port == null) {
            return null;
        }
        ClientStatMachine csm = ClientStatMachine.newByClientStatMachinePojo(ip.toString(), Integer.valueOf(port.toString()));
        return csm;
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public long allSize() {
        return 0;
    }

    @Override
    public boolean remove(String k) {
        return false;
    }

    @Override
    public void upToDate(String uuid, ClientStatMachine csm) {
        Jedis jedis = RedisUtils.getRedis ();
        String lastAddr = csm.getLastAddr().toString();
        jedis.hset(uuid, "ip", lastAddr.substring(1, lastAddr.indexOf(":")));
        jedis.hset(uuid, "port", lastAddr.substring(lastAddr.indexOf(":") + 1));
        jedis.expire(uuid, expiredSeconds);
        jedis.close();
    }

}
