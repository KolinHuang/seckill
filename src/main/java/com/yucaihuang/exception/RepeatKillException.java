package com.yucaihuang.exception;

/**
 * 重复秒杀异常，是一个运行时异常，不需要我们手动try catch
 * mysql只支持运行时异常的回滚操作
 */
public class RepeatKillException extends SeckillException {

    public RepeatKillException(String message) {
        super(message);
    }

    public RepeatKillException(String message, Throwable cause) {
        super(message, cause);
    }
}
