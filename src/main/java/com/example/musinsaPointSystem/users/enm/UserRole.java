package com.example.musinsaPointSystem.users.enm;

public enum UserRole {
	USER("ROLE_USER"),
	Employee("ROLE_Employee"),
	ADMIN("ROLE_ADMIN");

	private final String authority;

	UserRole(String authority) {
		this.authority = authority;
	}

	public String getAuthority() {
		return authority;
	}
}
