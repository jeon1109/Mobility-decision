package com.example.musinsaPointSystem.config.queue;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserQueueConfig {
	// 메시지 전달
	@Value("${message.exchange}")
	private String exchange;
	// 큐 저장소
	@Value("${message.queue.user}")
	private String queueUsers;

	@Value("${message.queue.join}")
	private String queueJoins;

	@Bean
	public TopicExchange exchange() {
		return new TopicExchange(exchange);
	}

	@Bean
	public Queue queueUsers() {
		return new Queue(queueUsers);
	}

	@Bean
	public Queue queueJoins() {
		return new Queue(queueJoins);
	}

	// 바인딩 해야 메시지가 전달이 된다!
	@Bean
	public Binding bindingUsers() {
		return BindingBuilder
			.bind(queueUsers())
			.to(exchange())
			.with(queueUsers);
	}

	@Bean
	public Binding bindingJoins() {
		return BindingBuilder
			.bind(queueJoins())
			.to(exchange())
			.with(queueJoins);
	}

}
