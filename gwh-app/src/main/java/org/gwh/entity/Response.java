package org.gwh.entity;

import lombok.Data;

/**
 * 通用响应实体类
 */
@Data
public class Response<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    private Response(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应
     */
    public static <T> Response<T> success(T data) {
        return new Response<>(200, "success", data);
    }

    /**
     * 成功响应（带自定义消息）
     */
    public static <T> Response<T> success(String message, T data) {
        return new Response<>(200, message, data);
    }

    /**
     * 错误响应
     */
    public static <T> Response<T> error(String message) {
        return new Response<>(500, message, null);
    }

    /**
     * 错误响应（带自定义错误码）
     */
    public static <T> Response<T> error(int code, String message) {
        return new Response<>(code, message, null);
    }
} 