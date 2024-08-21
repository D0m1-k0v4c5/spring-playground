package com.example.springplayground.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.function.Consumer;

import com.example.springplayground.WithTestContainers;
import com.example.springplayground.account.Account;
import com.example.springplayground.account.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author d0m1.k0v4c5 on 2024. 08. 19.
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(properties = {
		"spring.cloud.function.definition=account",
		"spring.cloud.stream.bindings.account-in-0.destination=TransactionRollbackTest.account",
		"spring.cloud.stream.bindings.account-in-0.group=TransactionRollbackTestGroup",
		"spring.cloud.stream.bindings.account-in-0.consumer.max-attempts=3",
		"spring.cloud.stream.bindings.account-in-0.consumer.default-retryable=true",
		"spring.cloud.stream.bindings.account-in-0.consumer.back-off-initial-interval=100",
		"spring.cloud.stream.bindings.account-out-0.destination=TransactionRollbackTest.account",
		"spring.cloud.stream.bindings.account-out-0.group=TransactionRollbackTestGroup",
		"spring.cloud.stream.bindings.account-out-0.content-type=text/xml",
		"spring.cloud.stream.output-bindings=account-out-0",
		"spring.cloud.stream.rabbit.bindings.account-in-0.consumer.dead-letter-exchange=DLX",
		"spring.cloud.stream.rabbit.bindings.account-in-0.consumer.dead-letter-queue-name=TransactionRollbackTest.account.dlq",
		"spring.cloud.stream.rabbit.bindings.account-in-0.consumer.auto-bind-dlq=true",
		"spring.datasource.url=jdbc:h2:mem:TransactionRollbackTest;DB_CLOSE_ON_EXIT=FALSE;MODE=ORACLE",
})
class SimpleTransactionRollbackTest extends WithTestContainers {

	@SpyBean
	AccountRepository accountRepository;

	@Autowired
	StreamBridge streamBridge;

	@Autowired
	RabbitAdmin rabbitAdmin;

	@SneakyThrows
	@Test
	void transaction_rollback() {
		//precondition
		assertThat(accountRepository.count()).isZero();
		assertThat(rabbitAdmin.getQueueInfo("TransactionRollbackTest.account.dlq").getMessageCount()).isZero();

		//send wrong message
		Account account = new Account();
		account.setAccountName("TransactionRollbackTest1");
		account.setAccountNumber("NL29INGB2755167904NL29INGB2755167904");//longer than 34 chars
		streamBridge.send("account-out-0",
				MessageBuilder.withPayload(new ObjectMapper().writeValueAsString(account)).build());

		//check the result
		await().atMost(Duration.ofSeconds(10)).untilAsserted(
				() -> {
					verify(accountRepository, atLeast(3)).save(any());
					assertThat(rabbitAdmin.getQueueInfo("TransactionRollbackTest.account.dlq").getMessageCount()).isOne();
				}
		);
	}

	@TestConfiguration
	public static class Config {

		@Transactional
		public interface AccountTestConsumer extends Consumer<Message<String>> {
		}

		@Bean
		public IntegrationFlow accountFlow(ObjectMapper objectMapper, AccountRepository accountRepository) {
			return IntegrationFlow
					.from(AccountTestConsumer.class, spec -> spec.beanName("account"))
					.log(LoggingHandler.Level.DEBUG, "com.example.SimpleTransactionRollbackTest", t -> "Flow started: " + t)
					.transform((String t) -> readAccount(objectMapper, t))
					.<Account, Account>transform(accountRepository::save)
					.channel( "nullChannel")
					.get();
		}

		@SneakyThrows
		private Account readAccount(ObjectMapper objectMapper, String payload) {
			return objectMapper.readValue(payload, Account.class);
		}

	}

}


