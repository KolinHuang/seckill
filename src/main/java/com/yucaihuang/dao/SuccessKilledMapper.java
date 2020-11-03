package com.yucaihuang.dao;

import com.yucaihuang.pojo.SuccessKilled;
import org.apache.ibatis.annotations.Param;

public interface SuccessKilledMapper {

    /**
     * 插入购买明细，可过滤重复
     * @param seckill_id
     * @param user_phone
     * @return  插入的行数
     */
    int insertSuccessKilled(@Param("seckillId") long seckill_id,@Param("userPhone") long user_phone);

    SuccessKilled queryByIdWithSeckill(@Param("seckillId") long seckill_id,@Param("userPhone") long user_phone);
}
