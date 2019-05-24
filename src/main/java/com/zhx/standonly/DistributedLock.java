package com.zhx.standonly;
/*
 * 参考资料：https://mp.weixin.qq.com/s/Le65eKhl6B9mEJbi6hBGog
 * 单机redis中的分布式锁
 * redis实现分布式锁必须要满足以下要求：
 * 加锁：
 *  1、使用senNx
 *  2、加上过期时间防止死锁
 *  3、以上两个步骤必须是原子的，这里的解决方案是用了一行代码：jedis.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
 * 解锁：
 *  1、获取key的值
 *  2、拿着自己当时设的值去和上一步比较，相等才删除键
 *  3、以上两个步骤必须是原子的，这里的解决方案是通过lua脚本
 * 以上的加锁和解锁总结起来就是必须要满足：
 *  1、互斥性：任意时刻只有一个客户端可以获取到锁
 *  2、谁加的锁，谁才有资格解锁，别的客户端不能够解锁
 *  3、不要因为客户端宕机而引发死锁
 * 优化：
 *  过期时间可以设置长一点，但是尝试获取锁的阻塞时间不要设置那么长，以免一个客户端已经释放锁了其它客户端还在无谓的等待
 * 存在的问题：
 * 1、该分布式锁只适合在单机redis中使用，相应的，如果redis宕机了，则该分布式锁不可用。
 * 2、为什么该分布式锁不能够在redis集群中使用，想象以下，客户端A使用setNx在一个master中成功申请到锁，master节点还没来得及向slave节点同步该锁对应key的数据，
 * 此时master宕机，其它的某个slave竞选为master，另外一个客户端B此时在master成功地申请到一把锁，也就是此时A和B同时拥有锁，便很有可能造成数据不一致的情况。
 * @Author: 遗忘的哈罗德
 * @Date: 2019-04-29 10:31
 */

import redis.clients.jedis.Jedis;
import java.util.Arrays;
import java.util.UUID;

public class DistributedLock {

    private static ThreadLocal<String> local = new ThreadLocal<String>();

    private static int i = 100;
    public static void main(String[] args) throws InterruptedException {
        Runnable runnable = new Runnable() {
            public void run() {
                Jedis jedis = Connection.getJedis();
                String lockKey = "decrease_data_i";
                int expireTime = 50;
                while (i > 0){
                    lock(jedis, lockKey, expireTime);
                    //再次检查，因为进入while循环后，在阻塞的过程中，i的值可能已经小与0，此时就不可以再递减了，因此要再次检查一下i的值
                    if (i > 0){
                        i--;
                        System.out.println("线程" + Thread.currentThread().getName() + " : " + i);
                    }
                    unLock(jedis, lockKey);
                }
            }
        };
        Thread thread1 = new Thread(runnable, "thread1");
        Thread thread2 = new Thread(runnable, "thread2");
        Thread thread3 = new Thread(runnable, "thread3");
        Thread thread4 = new Thread(runnable, "thread4");
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        Thread.currentThread().join();
    }


    private static final String LOCK_SUCCESS = "OK";
    private static final String SET_IF_NOT_EXIST = "NX";
    private static String SET_WITH_EXPIRE_TIME = "PX";
    private static final Long RELEASE_SUCCESS = 1L;

    /**
     * 获取分布式锁
     * @param jedis
     * @param lockKey
     * @param expireTime
     * @return
     */
    public static boolean tryGetDistributedLock(Jedis jedis, String lockKey, int expireTime){
        String requestId = UUID.randomUUID().toString().replace("-", "").toLowerCase();;
        String result = jedis.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
//        System.out.println(result);
        if(LOCK_SUCCESS.equals(result)){
            local.set(requestId);
            return true;
        }
        return false;
    }

    /**
     * 释放分布式锁
     * @param jedis
     * @param lockKey
     */
    public static boolean unLock(Jedis jedis, String lockKey){
        String lua = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedis.eval(lua, Arrays.asList(lockKey), Arrays.asList(local.get()));
        if (RELEASE_SUCCESS.equals(result)) {
            return true;
        } else {
            System.out.println("解锁失败");
        }
        return false;
    }

    /**
     * 阻塞式加锁
     */
    public static void lock(Jedis jedis, String lockKey, int expireTime){
        //加锁成功，直接返回
//        System.out.println("try");
        if(tryGetDistributedLock(jedis, lockKey, expireTime)){
            return;
        }

        //加锁失败，休眠一段时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //递归调用，再次尝试加锁
        lock(jedis, lockKey, expireTime);
    }
}
