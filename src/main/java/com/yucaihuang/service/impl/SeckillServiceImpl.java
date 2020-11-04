package com.yucaihuang.service.impl;

import com.yucaihuang.dao.SeckillMapper;
import com.yucaihuang.dao.SuccessKilledMapper;
import com.yucaihuang.dao.cache.RedisMapper;
import com.yucaihuang.dto.Exposer;
import com.yucaihuang.dto.SeckillExecution;
import com.yucaihuang.enums.SeckillStatEnum;
import com.yucaihuang.exception.RepeatKillException;
import com.yucaihuang.exception.SeckillCloseException;
import com.yucaihuang.exception.SeckillException;
import com.yucaihuang.pojo.Seckill;
import com.yucaihuang.pojo.SuccessKilled;
import com.yucaihuang.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.internal.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.List;

public class SeckillServiceImpl implements SeckillService {


    //日志对象
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    //加入一个混淆字符串（秒杀接口）的salt，为了避免用户猜出我们的md5值，值任意给，越复杂越好
    private final String salt="safjlvllj`asdl.kn";

    private SeckillMapper seckillMapper;
    private SuccessKilledMapper successKilledMapper;
    private RedisMapper redisMapper;

    public void setSeckillMapper(SeckillMapper seckillMapper) {
        this.seckillMapper = seckillMapper;
    }

    public void setSuccessKilledMapper(SuccessKilledMapper successKilledMapper) {
        this.successKilledMapper = successKilledMapper;
    }

    public void setRedisMapper(RedisMapper redisMapper) {
        this.redisMapper = redisMapper;
    }

    public List<Seckill> getSeckillList() {
        return seckillMapper.queryAll(0,4);
    }

    public Seckill getSeckillById(long seckillId) {
        return redisMapper.getOrPutSeckill(seckillId, new Function<Long, Seckill>() {
            public Seckill apply(Long id) {
                return seckillMapper.queryById(id);
            }
        });
    }

    /**
     * 根据seckillId来验证此产品是否在秒杀商品信息中，如果存在就判断当前时间是否在秒杀时间段内
     * 如果二者都成立，就生成一个加密后的md5，返回
     * @param seckillId
     * @return
     */
    public Exposer exportSeckillUrl(long seckillId) {
        Seckill seckill = seckillMapper.queryById(seckillId);
        //说明查不到这个秒杀产品的记录
        if(seckill == null){
            return new Exposer(false,seckillId);
        }
        Date start_time = seckill.getStart_time();
        Date end_time = seckill.getEnd_time();
        Date now_time = new Date();
        //若是当前时间不在秒杀时间段内
        if(start_time.getTime() > now_time.getTime() || end_time.getTime() < now_time.getTime()){
            return new Exposer(false, seckillId, now_time.getTime(), start_time.getTime(),end_time.getTime());
        }

        //秒杀开启，返回秒杀商品的id、用给接口加密的md5
        String md5 = getMD5(seckillId);
        return new Exposer(true, md5, seckillId);

    }

    private String getMD5(long seckillId){
        String base = seckillId + "/" + salt;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }

    /**
     * 秒杀是否成功，若成功：减库存，增加明细；失败：抛出异常，mysql自动事务回滚
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    @Transactional
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {

        if(md5 == null || !md5.equals((getMD5(seckillId)))){
            //md5不匹配，说明秒杀数据被重写了，抛出异常
            throw new SeckillException("seckill data has been rewrite");
        }

        Date now_time = new Date();

        try{
            //减库存
            int updateCount = seckillMapper.reduceNumber(seckillId, now_time);
            if(updateCount <= 0){
                //没有更新库存记录，说明秒杀结束
                throw  new SeckillCloseException("seckill is closed");
            }else {
                //成功更新了库存
                int insertCount = successKilledMapper.insertSuccessKilled(seckillId, userPhone);
                //是否该明细被重复插入，即用户是否重复秒杀
                if(insertCount <= 0){
                    throw new RepeatKillException("seckill repeated");
                }else {
                    SuccessKilled successKilled = successKilledMapper.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS,successKilled);
                }
            }
        }catch (SeckillCloseException e1){
            throw e1;
        }catch (RepeatKillException e2){
            throw e2;
        } catch (Exception e){
            logger.error(e.getMessage(),e);
            //编译期异常转化为运行期异常
            throw new SeckillException("seckill inner error :"+e.getMessage());
        }
    }
}
