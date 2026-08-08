package com.kshiteesh.datingapp.identityservice.token.access;

public class InvalidTokenConfigurationException extends RuntimeException {

	public InvalidTokenConfigurationException(String message) {
		super(message);
	}

	public InvalidTokenConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
}
