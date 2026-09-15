package com.smartfactory.common.exception;

import com.smartfactory.common.response.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(
            BusinessException e) {

        return ResponseEntity
                .status(resolveHttpStatus(e.getCode()))
                .body(
                        Result.error(
                                e.getCode(),
                                e.getMessage()
                        )
                );
    }

    /**
     * 处理请求参数绑定校验异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(
            BindException e) {

        String message = e.getBindingResult().getFieldError() == null
                ? "请求参数错误"
                : e.getBindingResult()
                        .getFieldError()
                        .getDefaultMessage();

        return ResponseEntity
                .badRequest()
                .body(Result.error(40001, message));
    }

    /**
     * 处理 Controller 参数校验异常
     */
    @ExceptionHandler({
            HandlerMethodValidationException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<Result<Void>> handleParameterValidationException(
            Exception e) {

        return ResponseEntity
                .badRequest()
                .body(
                        Result.error(
                                40001,
                                "请求参数校验失败"
                        )
                );
    }

    private HttpStatus resolveHttpStatus(Integer code) {

        if (code == null || code >= 50000) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (code >= 40900 && code < 41000) {
            return HttpStatus.CONFLICT;
        }

        if (code >= 40400 && code < 40500) {
            return HttpStatus.NOT_FOUND;
        }

        if (code >= 40300 && code < 40400) {
            return HttpStatus.FORBIDDEN;
        }

        if (code >= 40100 && code < 40200) {
            return HttpStatus.UNAUTHORIZED;
        }

        if (code >= 40000 && code < 50000) {
            return HttpStatus.BAD_REQUEST;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
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
    public ResponseEntity<Result<Void>> handleValidationException(
            MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        return ResponseEntity
                .badRequest()
                .body(Result.error(40001, message));
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {

        e.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        Result.error(
                                50000,
                                "服务器内部错误"
                        )
                );
    }
}
