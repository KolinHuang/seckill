package com.yucaihuang.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;

public class JedisUtils {

    private static final String LOCK_SUCESS = "OK";
    private static final Long RELEASE_SUCESS = 1L;


    /**
     * 尝试获取分布式锁
     * @param jedis
     * @param lockKey
     * @param requestId
     * @param expireTime
     * @return
     */
    public static boolean tryGetDistributedLock(Jedis jedis, String lockKey,
                                                String requestId, int expireTime){
        SetParams setParams = new SetParams();
        setParams.nx();
        setParams.ex(expireTime);

        String result = jedis.set(lockKey,requestId,setParams);
        return LOCK_SUCESS.equals(result);
    }


    /**
     * 释放分布式锁
     * @param jedis
     * @param lockKey
     * @param requestId
     * @return
     */
    public static boolean releaseDistributedLock(Jedis jedis, String lockKey, String requestId){
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));

        return RELEASE_SUCESS.equals(result);
    }

}
