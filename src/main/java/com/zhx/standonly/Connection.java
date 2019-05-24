package com.zhx.standonly;
/*
 * redis连接池
 * @Author: 遗忘的哈罗德
 * @Date: 2019-04-28 15:04
 */

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Connection {
    /**
     * 获取jedis连接池
     * @return
     */
    public static JedisPool getJedisPool(){
//        return new JedisPool(new JedisPoolConfig(), "47.101.159.36");
        return new JedisPool(new JedisPoolConfig(), "192.168.1.199");
    }

    /**
     * 获取jedis连接
     * @return
     */
    public static Jedis getJedis(){
        Jedis jedis = getJedisPool().getResource();
//        jedis.auth("yiwangdehaluode");
        return jedis;
    }
}
