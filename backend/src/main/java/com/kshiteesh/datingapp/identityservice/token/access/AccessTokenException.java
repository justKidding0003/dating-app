package com.kshiteesh.datingapp.identityservice.token.access;

public class AccessTokenException extends RuntimeException {

	public AccessTokenException(String message) {
		super(message);
	}

	public AccessTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}
