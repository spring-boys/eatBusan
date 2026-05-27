package com.ssafy.eatBusan.global.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EBException.class)
    public ResponseEntity<Map<String, String >> handleEBException(EBException ex){
        String msg = ex.getErrorCode().getMessage();
        HttpStatus status = ex.getErrorCode().getStatus();
        return ResponseEntity.status(status).body(Map.of("message", msg));
    }

}
