package com.zhx.standonly;
/*
 * redis支持的5种数据结构
 * @Author: 遗忘的哈罗德
 * @Date: 2019-04-28 15:08
 */

import redis.clients.jedis.Jedis;

import java.util.*;


public class FiveDataStructure {
    public static void main(String[] args) {
//        testString();
//        testHash();
//        testList();
//        testSet();
        testSortedSet();
    }


    /**
     * 字符串
     * set name zhx
     * get name
     */
    private static void testString(){
        Jedis jedis = Connection.getJedis();
        jedis.set("name", "zhx");
        System.out.println(jedis.get("name"));
    }

    /**
     * 测试Hash(可以把Hash这种数据结构简单的理解为java.util.HashMap)
     * hmset info name zhx sex male age 25 job java
     * hmget info age job
     */
    private static void testHash(){
        Jedis jedis = Connection.getJedis();
        Map<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("name", "zhx");
        hashMap.put("sex", "male");
        hashMap.put("age", "25");
        hashMap.put("job", "java");
        jedis.hmset("info", hashMap);
        System.out.println(jedis.hmget("info", "age", "job"));
    }

    /**
     * 测试List(可以把List这种数据结构简单的理解为java.util.List)
     * lpush foods egg fruit apple
     * lrange foods 0 10
     */
    private static void testList(){
        Jedis jedis = Connection.getJedis();
        jedis.lpush("foods", "egg", "fruit", "apple");
        List<String> list = jedis.lrange("foods", 0, 10);
        for (String var : list){
            System.out.println(var);
        }
    }

    /**
     * 测试Set(可以把List这种数据结构简单的理解为java.util.Set)
     */
    private static void testSet(){
        Jedis jedis = Connection.getJedis();
        jedis.sadd("sport", "basketball", "football", "baseball", "football", "basketball");
        Collection<String> set = jedis.smembers("sport");
        for (String var : set){
            System.out.println(var);
        }
    }

    /**
     * 测试有序集合
     * zadd course 100 physics
     * zadd course 70 math
     * zadd course 40 chemistry
     * zadd course 60 biology
     * zrange course 1 100
     */
    private static void testSortedSet(){
        Jedis jedis = Connection.getJedis();
        jedis.zadd("course", 100, "physics");
        jedis.zadd("course", 70, "math");
        jedis.zadd("course", 40, "chemistry");
        jedis.zadd("course", 60, "biology");
        Set<String> set = jedis.zrange("course", 0, -1);
        for (String var : set){
            System.out.println(var);
        }
    }

}
