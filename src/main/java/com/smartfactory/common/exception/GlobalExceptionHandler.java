package com.smartfactory.common.exception;

import com.smartfactory.common.response.Result;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(
            BusinessException e) {

        return Result.error(
                e.getCode(),
                e.getMessage()
        );
    }

    /**
     * 处理无权限访问
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(
            AccessDeniedException e) {

        return Result.error(40300, "无权访问");
    }

    /**
     * 处理认证失败
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(
            AuthenticationException e) {

        String message = e.getMessage() == null
                ? "认证失败"
                : e.getMessage();

        return Result.error(40100, message);
    }

    /**
     * 处理请求参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(
            MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        return Result.error(40001, message);
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {

        e.printStackTrace();

        return Result.error(
                50000,
                "服务器内部错误"
        );
    }
}
