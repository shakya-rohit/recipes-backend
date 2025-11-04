package com.publicis.recipes.exception;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class GlobalExceptionHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
	// Handle Custom Exception
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex, WebRequest request) {
    	logger.error("Handled CustomException - Status: {}, Message: {}", ex.getStatus(), ex.getMessage());
        if(ex.status==null) {
        	return buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.name(), request);
        }
        else {
        	return buildResponse(ex, ex.getStatus(), ex.getStatus().name(), request);
        }
    }
    
    // Handle all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, WebRequest request) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", request);
    }
    
    private ResponseEntity<ErrorResponse> buildResponse(Exception ex, HttpStatus status, String error, WebRequest request) {
        return buildResponse(ex, status, error, request, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildResponse(Exception ex, HttpStatus status, String error, WebRequest request, String message) {
    	ErrorResponse response = new ErrorResponse();
    	response.setTimestamp(LocalDateTime.now());
    	response.setStatus(status.value());
    	response.setError(error);
    	response.setMessage(message);
    	response.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(response, status);
    }
}