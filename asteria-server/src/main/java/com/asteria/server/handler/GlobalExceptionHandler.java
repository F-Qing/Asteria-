package com.asteria.server.handler;

import com.asteria.common.exception.BusinessException;
import com.asteria.common.result.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 【全局异常处理器】位于 handler 包（对应苍穹外卖 sky-server 的 com.sky.handler.GlobalExceptionHandler）。
 * 作用：任何接口抛出的异常都统一翻译成 ApiResponse，避免把 Spring 默认报错页/堆栈抛给前端。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务校验失败（如文件类型不对）：按契约返回 HTTP 200 + body 里 code!=0。
     * 前端只认 body.code；HTTP 4xx/5xx 留给"契约之外"的系统级错误。
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常：code={}, msg={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /** 404：请求的接口不存在（前端会弹 message 并静默降级为空态） */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(40400, "接口不存在：" + e.getResourcePath()));
    }

    /** 兜底 500：堆栈只进日志，不抛给前端 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(50000, "服务器内部错误，请稍后重试"));
    }
}