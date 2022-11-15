package com.sy.common;

/**
 * 推理异常
 * @author sy
 * @date 2022/9/13 21:30
 */
public class PredictException extends RuntimeException {

    public PredictException() {
    }

    public PredictException(Throwable exception) {
        super(exception);
    }

    public PredictException(String message, Throwable cause) {
        super(message, cause);
    }
}
