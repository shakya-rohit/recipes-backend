package com.publicis.recipes.exception;

import org.springframework.http.HttpStatus;

public class CustomException extends Exception {
	private static final long serialVersionUID = 1L;
	
	HttpStatus status;
	String message;

	public CustomException(String message){
		super(message);
		this.message=message;
	}
	
	public CustomException(HttpStatus status, String message){
		super(message);
		this.status=status;
		this.message=message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public void setStatus(HttpStatus status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}