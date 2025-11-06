package com.healthapp.shared.exception;

import lombok.Getter;

/**
 * Exception de base pour toutes les exceptions personnalisées
 */
@Getter
public class BaseException extends RuntimeException {
    
    private final String errorCode;
    private final int httpStatus;
    
    public BaseException(String message) {
        super(message);
        this.errorCode = "INTERNAL_ERROR";
        this.httpStatus = 500;
    }
    
    public BaseException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public BaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INTERNAL_ERROR";
        this.httpStatus = 500;
    }
    
    public BaseException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}