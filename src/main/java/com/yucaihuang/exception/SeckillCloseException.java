package com.yucaihuang.exception;


/**
 * 秒杀关闭异常，当秒杀结束时，用户还要进行秒杀，就会出现这个异常
 */
public class SeckillCloseException extends SeckillException {
    public SeckillCloseException(String message) {
        super(message);
    }

    public SeckillCloseException(String message, Throwable cause) {
        super(message, cause);
    }
}
