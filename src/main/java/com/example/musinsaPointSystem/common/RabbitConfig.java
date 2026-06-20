package com.example.musinsaPointSystem.common;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
	@Value("${message.broker.host}")
	private String host;

	@Value("${message.broker.port}")
	private int port;

	@Bean
	public ConnectionFactory connectionFactory() {
		CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
		factory.setUsername("admin");
		factory.setPassword("1234");
		return factory;
	}
}
