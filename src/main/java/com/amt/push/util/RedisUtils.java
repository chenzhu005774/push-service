package com.amt.push.util;

import com.amt.push.consts.Constant;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * * @Description: redis工具类
 * * @author ckx
 * * @date 2019/4/16 20:15
 */
public class RedisUtils {


    private static RedisConnectionFactory redisConnectionFactory;

    private static JedisCluster jedisCluster;

    /**
     * * @Description: jedis连接池配置
     * * @author ckx
     * * @date 2019/4/18 15:14
     */
    private static JedisClientConfiguration getJedisConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(15);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(0);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(false);
        poolConfig.setTestWhileIdle(true);
        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
                .usePooling().poolConfig(poolConfig).and().readTimeout(Duration.ofMillis(2000)).build();
        return clientConfig;
    }


    /**
     * * @Description: 初始化redis
     * * @author ckx
     * * @date 2019/4/18 15:19
     */
    private static void initRedis() {
        if (redisConnectionFactory == null) {
            JedisClientConfiguration clientConfig = getJedisConfig();
            if ("Standalone".equals(Constant.REDIS_TYPE)) {
                // 单点redis
                RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
                redisConfig.setHostName(Constant.REDIS_HOST);
                redisConfig.setPassword(RedisPassword.of(Constant.REDIS_PASS));
                redisConfig.setPort(Constant.REDIS_PORT);
                redisConfig.setDatabase(Constant.REDIS_MACHINE_DATABASE);
                redisConnectionFactory = new JedisConnectionFactory(redisConfig, clientConfig);
            } else if ("Cluster".equals(Constant.REDIS_TYPE)) {
                // 集群redis
                String[] hosts = Constant.REDIS_HOST.split("\\|");
                Set<HostAndPort> nodes = new HashSet<>();
                for (String hostport : hosts) {
                    String[] ipport = hostport.split(":");
                    String ip = ipport[0];
                    int port = Integer.parseInt(ipport[1]);
                    nodes.add(new HostAndPort(ip, port));
                }
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxTotal(15);
                config.setMaxIdle(5);
                config.setMaxWaitMillis(0);
                config.setTestOnBorrow(true);
                config.setTestOnReturn(false);
                config.setTestWhileIdle(true);
                jedisCluster = new JedisCluster(nodes,config);
            }
        }
    }


        /**
         * * @Description: 获取redis连接
         * * @author ckx
         * * @date 2019/4/18 15:24
         */
        public static Jedis getRedis () { //双重检查
            if (redisConnectionFactory == null) {
                synchronized (RedisUtils.class) {
                    if (redisConnectionFactory == null) {
                        initRedis();
                    }
                }
            }
            return (Jedis) redisConnectionFactory.getConnection().getNativeConnection();
        }

        /**
         * * @Description: 获取redis集群连接
         * * @author ckx
         * * @date 2019/4/18 15:24
         */
        public static JedisCluster getJedisCluster () { //双重检查
            if (jedisCluster == null) {
                synchronized (RedisUtils.class) {
                    if (jedisCluster == null) {
                        initRedis();
                    }
                }
            }
            return jedisCluster;
        }

    }