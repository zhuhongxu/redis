package com.zhx.standonly;
/*
 * 限流
 * @Author: 遗忘的哈罗德
 * @Date: 2019-04-30 11:19
 */

import redis.clients.jedis.Jedis;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FlowLimit {

    private final static String PREFIX_SINGLE_IPFLOW_LIMIT = "sssessaqsssingleIpFlowLimit";//单个ip流量限制前缀
    private final static String ALL_IP_FLOW_LIMIT = "sasessqsswsAllIwpFlowLimit";//所有ip流量限制

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(100);
        final Random random = new Random();
        //模拟用户抢单
        for (int i = 0; i < 1000; i++){
            pool.execute(new Runnable() {
                public void run() {
                    String ip = "192.168.1." + String.valueOf(random.nextInt(999));
                    gateWay(ip);
                }
            });
        }

    }


    /**
     * 网关
     */
    private static void gateWay(String ip){
        Jedis jedis = Connection.getJedis();
        //同一个ip5秒内最多只能访问3次
        boolean singleIp = singleIpAccessLimit(jedis, ip);
        //所有ip加起来10秒内最多访问100次
        boolean allIp = allIpAccessLimit(jedis);
        if (singleIp && allIp){
            grabOrder(ip);
        }
    }

    /**
     * 抢单
     */
    static int count = 0;
    private synchronized static void grabOrder(String ip){
        count++;
        System.out.println("ip ： " + ip + "调用了抢单逻辑" + "，" + count);
    }

    /**
     * 单个ip的访问限制
     */
    private static boolean singleIpAccessLimit(Jedis jedis, String ip){
        String key = PREFIX_SINGLE_IPFLOW_LIMIT + ip;
        //获取过期时间段内的查询次数
        String countStr = jedis.get(key);
        if (null != countStr){
            int count = Integer.valueOf(countStr);
            //说明还没到过期时间
            if (count > 3){
                System.out.println("ip : " + ip + "，操作过于频繁");
                return false;
            } else {
                jedis.incr(key);
            }
        } else {
            jedis.set(key, "1");
            jedis.expire(key, 5000);
        }
        return true;
    }

    /**
     * 所有ip加起来的访问限制
     */
    private static boolean allIpAccessLimit(Jedis jedis){
        String key = ALL_IP_FLOW_LIMIT;
        //获取查询次数
        String countStr = jedis.get(key);
        if (null != countStr){
            int count = Integer.valueOf(countStr);
            //说明还没到过期时间
            if (count > 100){
//                System.out.println("已经达到流量限制上限");
                return false;
            } else {
                jedis.incr(key);
            }
        } else {
            jedis.set(key, "1");
            jedis.expire(key, 100000);
        }
        return true;
    }


}
