package com.campus.repair.config;

import com.campus.repair.common.BusinessException;
import com.campus.repair.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** @Valid 校验失败（如登录参数为空）返回 400 与首条校验信息 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBind(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(IOException.class)
    public Result<?> handleIOException(IOException e) {
        log.warn("IO 异常: {}", e.getMessage());
        return Result.error(500, "文件读写失败，请稍后重试");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("文件大小超限: {}", e.getMessage());
        return Result.error(400, "文件大小超过限制，请上传 10MB 以内的文件");
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public Result<?> handleRedisFailure(RedisConnectionFailureException e) {
        log.warn("Redis 连接失败: {}", e.getMessage());
        return Result.error(503, "服务暂不可用，请稍后重试");
    }

    @ExceptionHandler(DataAccessException.class)
    public Result<?> handleDataAccess(DataAccessException e) {
        log.error("数据库异常", e);
        String msg = "数据库连接不可用。请确认：1) MySQL 容器已启动；2) 端口/账号密码配置正确；3) 数据库已初始化";
        return Result.error(503, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统繁忙，请稍后重试");
    }
}
