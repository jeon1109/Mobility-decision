package com.example.musinsaPointSystem.common.jwt;

import static org.springframework.security.core.context.SecurityContextHolder.*;

import org.springframework.security.core.context.SecurityContext;

public class SecurityContextHolder {
	private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();

	public static SecurityContext getContext() {
		SecurityContext ctx = contextHolder.get();
		if (ctx == null) {
			ctx = createEmptyContext();
			contextHolder.set(ctx);
		}
		return ctx;
	}
}
