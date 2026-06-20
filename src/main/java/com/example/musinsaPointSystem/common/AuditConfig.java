package com.example.musinsaPointSystem.common;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
public class AuditConfig {
	@Bean
	public AuditorAware<String> auditorProvider() {
		return new AuditorAware<String>() {
			@Override
			public Optional<String> getCurrentAuditor() {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();

				if (auth == null || !auth.isAuthenticated()) {
					return Optional.of("anonymousUser");
				}

				return Optional.of(auth.getName());
			}
		};
	}
}
