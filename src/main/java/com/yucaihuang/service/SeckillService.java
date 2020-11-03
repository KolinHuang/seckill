package com.yucaihuang.service;


import com.yucaihuang.dto.Exposer;
import com.yucaihuang.dto.SeckillExecution;
import com.yucaihuang.exception.RepeatKillException;
import com.yucaihuang.exception.SeckillCloseException;
import com.yucaihuang.exception.SeckillException;
import com.yucaihuang.pojo.Seckill;

import java.util.List;

/**
 * 业务层
 */
public interface SeckillService {

    /**
     * 查询全部的秒杀记录
     * @return
     */
    List<Seckill> getSeckillList();

    /**
     * 按ID查询秒杀记录
     * @return
     */
    Seckill getSeckillById(long seckillId);

    //往下是我们最重要的行为的一些接口

    /**
     * 在秒杀开启时输出秒杀接口的地址，否则输出系统时间和秒杀时间
     * @param seckillId
     * @return
     */
    Exposer exportSeckillUrl(long seckillId);

    /**
     * 执行秒杀操作，有可能失败，有可能成功，所以要抛出我们允许的异常
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException;
}
