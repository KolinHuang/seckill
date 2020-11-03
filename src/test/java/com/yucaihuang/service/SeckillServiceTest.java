package com.yucaihuang.service;

import com.yucaihuang.dto.Exposer;
import com.yucaihuang.dto.SeckillExecution;
import com.yucaihuang.exception.RepeatKillException;
import com.yucaihuang.exception.SeckillCloseException;
import com.yucaihuang.pojo.Seckill;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:ApplicationContext.xml")
public class SeckillServiceTest {

    @Autowired
    SeckillService seckillService;

    @Test
    public void getSeckillList() {
        List<Seckill> seckillList = seckillService.getSeckillList();
        for (Seckill seckill : seckillList) {
            System.out.println(seckill);
        }
    }

    @Test
    public void getSeckillById() {
        Seckill seckillById = seckillService.getSeckillById(1001);
        System.out.println(seckillById);

    }

    @Test
    public void exportSeckillUrl() {
        Exposer exposer = seckillService.exportSeckillUrl(1002);
        System.out.println(exposer);
    }

    @Test
    public void executeSeckill() {
        SeckillExecution seckillExecution = seckillService.executeSeckill(1002, 1506779719, "80267e7716eeec0135c23d6a4a61add4");
        System.out.println(seckillExecution);
    }

    @Test
    public void testSeckillSeckillLogic() throws Exception{
        long seckillId = 1002;
        long userPhone = 15067729719L;
        Exposer exposer = seckillService.exportSeckillUrl(seckillId);
        if(exposer.isExposed()){
            System.out.println(exposer);
            String md5 = exposer.getMd5();
            try {
                seckillService.executeSeckill(seckillId,userPhone,md5);
            }catch (RepeatKillException e1){
                throw e1;
            }catch (SeckillCloseException e2){
                throw e2;
            }
        }else {
            //秒杀未开启
            System.out.println(exposer);
        }
    }
}