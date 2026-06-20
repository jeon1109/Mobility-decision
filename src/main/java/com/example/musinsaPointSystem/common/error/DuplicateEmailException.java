package com.example.musinsaPointSystem.common.error;

public class DuplicateEmailException extends RuntimeException {
	public DuplicateEmailException(String email) {
		super("email exists: " + email);
	}
}
