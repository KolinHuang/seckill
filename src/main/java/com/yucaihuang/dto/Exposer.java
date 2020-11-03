package com.yucaihuang.dto;


/**
 * 暴露秒杀地址(接口)DTO
 */
public class Exposer {

    //是否开启秒杀
    private boolean exposed;

    //对秒杀地址加密的措施
    private String md5;

    //id为seckillId的商品的秒杀地址
    private long seckillId;

    //系统当前时间（毫秒）
    private long now_time;

    //秒杀的开启时间
    private long start_time;

    //秒杀的结束时间
    private long end_time;

    public Exposer(boolean exposed, String md5, long seckillId) {
        this.exposed = exposed;
        this.md5 = md5;
        this.seckillId = seckillId;
    }

    public Exposer(boolean exposed, long seckillId, long now_time, long start_time, long end_time) {
        this.exposed = exposed;
        this.seckillId = seckillId;
        this.now_time = now_time;
        this.start_time = start_time;
        this.end_time = end_time;
    }

    public Exposer(boolean exposed, long seckillId) {
        this.exposed = exposed;
        this.seckillId = seckillId;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(long seckillId) {
        this.seckillId = seckillId;
    }

    public long getNow_time() {
        return now_time;
    }

    public void setNow_time(long now_time) {
        this.now_time = now_time;
    }

    public long getStart_time() {
        return start_time;
    }

    public void setStart_time(long start_time) {
        this.start_time = start_time;
    }

    public long getEnd_time() {
        return end_time;
    }

    public void setEnd_time(long end_time) {
        this.end_time = end_time;
    }

    @Override
    public String toString() {
        return "Exposer{" +
                "exposed=" + exposed +
                ", md5='" + md5 + '\'' +
                ", seckillId=" + seckillId +
                ", now_time=" + now_time +
                ", start_time=" + start_time +
                ", end_time=" + end_time +
                '}';
    }
}
