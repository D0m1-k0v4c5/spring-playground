package com.example.springplayground.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.function.Consumer;

import com.example.springplayground.WithTestContainers;
import com.example.springplayground.account.Account;
import com.example.springplayground.account.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author d0m1.k0v4c5 on 2024. 08. 19.
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(properties = {
		"spring.cloud.function.definition=account",
		"spring.cloud.stream.bindings.account-in-0.destination=SimpleTransactionSuccessTest.account",
		"spring.cloud.stream.bindings.account-in-0.group=SimpleTransactionSuccessTestGroup",
		"spring.cloud.stream.bindings.account-out-0.destination=SimpleTransactionSuccessTest.account",
		"spring.cloud.stream.bindings.account-out-0.group=SimpleTransactionSuccessTestGroup",
		"spring.cloud.stream.bindings.account-out-0.content-type=text/xml",
		"spring.cloud.stream.bindings.account-out-0.producer.required-groups=SimpleTransactionSuccessTestGroup",
		"spring.cloud.stream.output-bindings=account-out-0",
		"spring.datasource.url=jdbc:h2:mem:SimpleTransactionSuccessTest;DB_CLOSE_ON_EXIT=FALSE;MODE=ORACLE",

})
class SimpleTransactionSuccessTest extends WithTestContainers {

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	StreamBridge streamBridge;

	@SneakyThrows
	@Test
	void transaction_success() {
		// precondition : empty database
		assertThat(accountRepository.count()).isZero();

		// send a valid account to save
		Account account = new Account();
		account.setAccountName("SimpleTransactionSuccessTest1");
		account.setAccountNumber("NL29INGB2755167904");
		streamBridge.send("account-out-0",
				MessageBuilder.withPayload(new ObjectMapper().writeValueAsString(account)).build());

		//check successful result
		await().atMost(Duration.ofSeconds(10)).untilAsserted(
				() -> assertThat(accountRepository.count()).isOne()
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


