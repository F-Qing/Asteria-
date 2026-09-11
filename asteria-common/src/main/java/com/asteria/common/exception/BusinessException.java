package com.asteria.common.exception;

/**
 * 业务异常：参数不合法、规则不满足等"预期内的失败"。
 * 继承 RuntimeException：不强制调用方 try-catch，让它一路抛到全局处理器统一翻译。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
