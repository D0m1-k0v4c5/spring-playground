package com.example.springplayground;


import static org.awaitility.Awaitility.await;

import org.awaitility.Durations;
import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @author d0m1.k0v4c5 on 2024. 08. 19.
 */
@Testcontainers
public abstract class WithTestContainers {

	@Container
	protected static RabbitMQContainer rabbitMQContainer =  new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13.6-management-alpine"));

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
	}

	@AfterAll
	static void tearDown() {
		await().pollDelay(Durations.FIVE_SECONDS).until(() -> true);
	}
}
