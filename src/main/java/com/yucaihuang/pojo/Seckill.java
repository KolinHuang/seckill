package com.yucaihuang.pojo;

import java.util.Date;

public class Seckill {
    private long seckill_id;
    private String name;
    private int number;
    private Date start_time;
    private Date end_time;
    private Date create_time;

    public Seckill(long seckill_id, String name, int number, Date start_time, Date end_time, Date create_time) {
        this.seckill_id = seckill_id;
        this.name = name;
        this.number = number;
        this.start_time = start_time;
        this.end_time = end_time;
        this.create_time = create_time;
    }

    public Seckill() {
    }

    @Override
    public String toString() {
        return "Seckill{" +
                "seckill_id=" + seckill_id +
                ", name='" + name + '\'' +
                ", number=" + number +
                ", start_time=" + start_time +
                ", end_time=" + end_time +
                ", create_time=" + create_time +
                '}';
    }

    public long getSeckill_id() {
        return seckill_id;
    }

    public void setSeckill_id(long seckill_id) {
        this.seckill_id = seckill_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Date getStart_time() {
        return start_time;
    }

    public void setStart_time(Date start_time) {
        this.start_time = start_time;
    }

    public Date getEnd_time() {
        return end_time;
    }

    public void setEnd_time(Date end_time) {
        this.end_time = end_time;
    }

    public Date getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }
}
