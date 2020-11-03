package com.yucaihuang.dao;

import com.yucaihuang.pojo.SuccessKilled;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring-dao.xml")
public class SuccessKilledMapperTest {

    @Autowired
    private SuccessKilledMapper successKilledMapper;

    @Test
    public void insertSuccessKilled() {
        successKilledMapper.insertSuccessKilled(1001,12855555);
    }

    @Test
    public void queryByIdWithSeckill() {
        SuccessKilled successKilled = successKilledMapper.queryByIdWithSeckill(1001, 12855555);
        System.out.println(successKilled);
    }
}