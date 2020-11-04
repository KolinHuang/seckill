package com.yucaihuang.dao.cache;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.yucaihuang.pojo.Seckill;
import com.yucaihuang.utils.JedisUtils;
import org.springframework.cglib.core.internal.Function;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.UUID;

public class RedisMapper {
    private final JedisPool jedisPool;


    public RedisMapper(String ip, int port){
        jedisPool = new JedisPool(ip,port);
    }

    //这是序列化吗
    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);



    public Seckill getSeckill(long seckillId){
        return getSeckill(seckillId,null);
    }

    /**
     * 从redis里读数据，如果不存在就返回null
     * @param seckillId
     * @param jedis
     * @return
     */
    public Seckill getSeckill(long seckillId, Jedis jedis){
        boolean hasJedis = jedis != null;

        try{
            if(!hasJedis){
                jedis = jedisPool.getResource();
            }
            try {
                String key = getSeckillRedisKey(seckillId);
                //根据key查询
                byte[] bytes = jedis.get(key.getBytes());
                //如果查到了，说明redis里有这个key的缓存，就反序列化，返回seckill对象
                if(bytes != null){
                    Seckill seckill = schema.newMessage();
                    ProtostuffIOUtil.mergeFrom(bytes, seckill,schema);
                    return seckill;
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(!hasJedis){
                    jedis.close();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private String getSeckillRedisKey(long seckillId){
        return "seckill:" + seckillId;
    }


    /**
     * 从redis中先读数据，如果没有，就从数据库中读
     * 这个Function挺有意思的，学习一下！
     * @param seckillId
     * @param getDataFromDb
     * @return
     */
    public Seckill getOrPutSeckill(long seckillId, Function<Long, Seckill> getDataFromDb){
        String lockKey = "seckill:locks:getSeckill:"+seckillId;
        String lockRequestId = UUID.randomUUID().toString();
        Jedis jedis = jedisPool.getResource();

        try{
            //循环争用锁，直到拿到了锁
            for(;;){
                Seckill seckill = getSeckill(seckillId, jedis);
                if(seckill != null){
                    return seckill;
                }
                //尝试获取锁
                boolean getLock = JedisUtils.tryGetDistributedLock(jedis,lockKey,lockRequestId,1000);
                if (getLock){
                    //获取到了锁,从数据库拿数据，存redis
                    seckill = getDataFromDb.apply(seckillId);
                    putSeckill(seckill, jedis);
                    return seckill;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //无论如何都要把锁释放
            JedisUtils.releaseDistributedLock(jedis, lockKey, lockRequestId);
            jedis.close();
        }
        return null;
    }

    public String putSeckill(Seckill seckill) {
        return putSeckill(seckill, null);
    }

    //将Seckill对象序列化后，存入redis
    public String putSeckill(Seckill seckill, Jedis jedis){
        boolean hasJedis = jedis != null;
        try {
            if(!hasJedis){
                jedis = jedisPool.getResource();
            }
            try {
                String key = getSeckillRedisKey(seckill.getSeckill_id());
                byte[] bytes = ProtostuffIOUtil.toByteArray(seckill, schema, LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
                //超时缓存1小时
                int timeout = 60 * 60;
                String result = jedis.setex(key.getBytes(), timeout, bytes);
                return result;
            }finally {
                if(!hasJedis){
                    jedis.close();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
