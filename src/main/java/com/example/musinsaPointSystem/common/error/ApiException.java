package com.example.musinsaPointSystem.common.error;

import com.example.musinsaPointSystem.users.enm.BasicResponseMessage;

public class ApiException extends RuntimeException {
	private final BasicResponseMessage basicResponseMessage;

	public ApiException(BasicResponseMessage basicResponseMessage) {
		super(basicResponseMessage.getMessage());
		this.basicResponseMessage = basicResponseMessage;
	}

	public BasicResponseMessage getBasicResponseMessage() {
		return basicResponseMessage;
	}
}
