package com.searchlocal.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(IndexingException.class)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> handleIndexingException(IndexingException e) {
        logger.error("Indexing error: {}", e.getMessage(), e);
        return Map.of("result", false, "error", e.getMessage());
    }
    
    @ExceptionHandler(SearchException.class)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> handleSearchException(SearchException e) {
        logger.error("Search error: {}", e.getMessage(), e);
        return Map.of("result", false, "error", e.getMessage());
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.error("Validation error: {}", e.getMessage(), e);
        return Map.of("result", false, "error", e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception e) {
        logger.error("Unexpected error: {}", e.getMessage(), e);
        return Map.of("result", false, "error", "Внутренняя ошибка сервера");
    }
}
