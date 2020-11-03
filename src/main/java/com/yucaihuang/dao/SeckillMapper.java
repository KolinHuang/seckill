package com.yucaihuang.dao;

import com.yucaihuang.pojo.Seckill;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface SeckillMapper {

    /**
     * 减库存的方法
     * @param seckill_id
     * @param kill_time
     * @return  表示更新库存的记录行数
     */
    public int reduceNumber(@Param("seckillId") long seckill_id,@Param("killTime") Date kill_time);

    /**
     * 根据id查询秒杀的商品信息
     * @param seckill_id
     * @return
     */
    public Seckill queryById(@Param("seckillId") long seckill_id);

    /**
     * 根据偏移量查询秒杀商品列表（什么偏移量？）
     * @param off
     * @param limit
     * @return
     */
    public List<Seckill> queryAll(@Param("offset") int off,@Param("limit") int limit);
}
