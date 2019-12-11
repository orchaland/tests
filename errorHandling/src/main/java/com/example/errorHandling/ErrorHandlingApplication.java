package com.example.errorHandling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.*;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.handler.advice.RetryStateGenerator;
import org.springframework.integration.handler.advice.SpelExpressionRetryStateGenerator;
import org.springframework.integration.jpa.dsl.Jpa;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.BackOffContext;
import org.springframework.retry.backoff.BackOffInterruptedException;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.File;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class ErrorHandlingApplication {

    @Service
	class ErrorGenerator{

		public TableInt table(TableInt i) throws Exception {
			System.out.println("------------- table 1 => arg = " + i + ", thread=" + Thread.currentThread().getId());
			//throw new Exception();
			return i;
		}

		public int threadInputFile1(int i) throws Exception {
			System.out.println("------------- threadInputFile 1 => arg = " + i + ", thread=" + Thread.currentThread().getId());
			//throw new Exception();
			return i;
		}

		public int threadInputFile2(TableInt i) throws Exception {
			System.out.println("------------- threadInputFile 2 => arg = " + i + ", thread=" + Thread.currentThread().getId());
			//throw new Exception();
			return i.getI();
		}

		public int threadBeforeDirectChannel(int i) throws Exception {
			System.out.println("------------- threadBeforeDirectChannel => arg = " + i + ", thread=" + Thread.currentThread().getId());
			//throw new Exception();
			return i;
		}

		public int threadAfterDirectChannel(int i) throws Exception {
			System.out.println("------------- threadAfterDirectChannel => arg = " + i + ", thread=" + Thread.currentThread().getId());
			//throw new Exception();
			return i;
		}

		public int threadAfterQueueChannel(int i) throws Exception {
			System.out.println("------------- threadAfterQueueChannel => arg = " + i + ", thread=" + Thread.currentThread().getId());
			//throw new Exception();
			return i;
		}

		public int threadRoute1(int i) throws Exception {
			System.out.println("------------- threadRoute1 => arg = " + i + ", thread=" + Thread.currentThread().getId());
			//throw new Exception();
			return i;
		}

		public int threadRoute2(int i) throws Exception {
			System.out.println("------------- threadRoute2 => arg = " + i + ", thread=" + Thread.currentThread().getId());
			//throw new Exception();
			return i;
		}

		public int[] threadAggregator(int[] array) throws Exception {
			System.out.println("------------- threadAggregator => arg = " + Arrays.toString(array) + ", thread=" + Thread.currentThread().getId());
			if(true){
				throw new Exception();
			}
			return array;
		}

		public Object errorChannelBeforeTableQuery(Object error) throws Exception {
			System.out.println("------------- errorChannelBeforeTableQuery => arg = " + error + ", thread=" + Thread.currentThread().getId());
			return error;
		}

		public TableInt[] errorChannelAfterTableQuery(TableInt[] array) throws Exception {
			System.out.println("------------- errorChannelAfterTableQuery => arg = " + Arrays.toString(array) + ", thread=" + Thread.currentThread().getId());
			return array;
		}

	}

	@Bean
	ErrorGenerator errorGenerator(){
		return new ErrorGenerator();
	}

	@Bean
	MessageChannel errorChannel(){
		return MessageChannels.direct().get();
	}

	@Bean
	public IntegrationFlow errorChannelFlow() {
		return IntegrationFlows.from(errorChannel())
				.handle("errorGenerator", "errorChannelBeforeTableQuery")
				.handle(Jpa.retrievingGateway(this.entityManagerFactory)
						.jpaQuery("from TableInt s"))
				.handle("errorGenerator", "errorChannelAfterTableQuery")
				.log()
				.get();
	}

	/*@Bean
	public RequestHandlerRetryAdvice retryAdvice() {
		RequestHandlerRetryAdvice requestHandlerRetryAdvice = new RequestHandlerRetryAdvice();

		RetryTemplate retryTemplate = new RetryTemplate();
		FixedBackOffPolicy policy = new FixedBackOffPolicy();
		policy.setBackOffPeriod(1000);
		retryTemplate.setBackOffPolicy(policy);

		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts(3);
		retryTemplate.setRetryPolicy(retryPolicy);

		requestHandlerRetryAdvice.setRecoveryCallback(errorMessageSendingRecoverer());
		return requestHandlerRetryAdvice;
	}

	@Bean
	public ErrorMessageSendingRecoverer errorMessageSendingRecoverer() {
		return new ErrorMessageSendingRecoverer(recoveryChannel());
	}

	@Bean
	public MessageChannel recoveryChannel() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow handleRecovery() {
		return IntegrationFlows.from("recoveryChannel")
				.log(LoggingHandler.Level.ERROR, "error",
						m -> m.getPayload())
				.get();
	}*/

	@Bean
	public IntegrationFlow file1ReadingFlow() {
		return IntegrationFlows.from(Files.inboundAdapter(new File(".\\files")).patternFilter("essai1.txt"), a -> a.poller(Pollers.fixedDelay(1000).transactional()))
		//return IntegrationFlows.from(Files.inboundAdapter(new File(".\\files")).patternFilter("essai1.txt"))
				.transform(Files.toStringTransformer())
				.<String, Integer>transform(Integer::parseInt)
				.handle("errorGenerator", "threadInputFile1")
				.channel("processFileChannel")
				.get();
	}

	@Bean
	public IntegrationFlow file2ReadingFlow() {
		//return IntegrationFlows.from(Files.inboundAdapter(new File(".\\files")).patternFilter("essai2.txt"), a -> a.poller(Pollers.fixedDelay(1000).transactional()))
		return IntegrationFlows.from(Files.inboundAdapter(new File(".\\files")).patternFilter("essai2.txt"), a -> a.poller(Pollers.fixedDelay(1000).transactional()))
				.transform(Files.toStringTransformer())
				.<String, Integer>transform(Integer::parseInt)
				.<Integer, TableInt>transform(TableInt::new)
				.handle("errorGenerator", "table")
				.enrichHeaders(h -> h.header("myCorrelationKey", 1))
				.handle(Jpa.updatingGateway(this.entityManagerFactory)
								.entityClass(TableInt.class)
								.persistMode(PersistMode.PERSIST),e -> e.transactional(true))
				.handle("errorGenerator", "threadInputFile2")
				.channel("aggregator.input")
				.get();
	}

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private DataSource dataSource;

	@Bean
	public MessageChannel processFileChannel() {
		return new DirectChannel();
	}

	/*@Bean
	public IntegrationFlow orders() {
		return f -> f
				.handle("errorGenerator", "threadBeforeDirectChannel")
				.channel(directChannel())
				.handle("errorGenerator", "threadAfterDirectChannel")
				.channel(queueChannel())
				.handle("errorGenerator", "threadAfterQueueChannel", c -> c.poller(Pollers.fixedRate(100)))
				.routeToRecipients(r -> r
						.recipient(route1())
						.recipient(route2()));
	}*/

	@Bean
	public IntegrationFlow ordersFlow() {
		return IntegrationFlows.from(processFileChannel())
				.handle("errorGenerator", "threadBeforeDirectChannel")
				.channel(directChannel())
				.handle("errorGenerator", "threadAfterDirectChannel")
				.routeToRecipients(r -> r
						.recipient(route1())
						.recipient(route2()))
				.get();
	}

	@Bean
	MessageChannel directChannel(){
		return MessageChannels.direct().get();
	}

	@Bean
	public IntegrationFlow route1Flow() {
		return IntegrationFlows.from(route1())
				.enrichHeaders(h -> h.header("myCorrelationKey", 1))
				.handle("errorGenerator", "threadRoute1")
				.<Integer, TableInt>transform(TableInt::new)
				.handle(Jpa.updatingGateway(this.entityManagerFactory)
						.entityClass(TableInt.class)
						.persistMode(PersistMode.PERSIST),e -> e.transactional(true))
				.<TableInt, Integer>transform(TableInt::getI)
				.channel("aggregator.input")
				.get();
	}

	@Bean
	public DirectChannel route1() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow route2Flow() {
		return IntegrationFlows.from(route2())
				.enrichHeaders(h -> h.header("myCorrelationKey", 1))
				.handle("errorGenerator", "threadRoute2")
				.channel("aggregator.input")
				.get();
	}

	@Bean
	public DirectChannel route2() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow aggregator() {
		return f -> f.aggregate(a ->
				a.correlationStrategy(m -> m.getHeaders().get("myCorrelationKey"))
				.releaseStrategy(g -> g.size() == 3))
				.enrichHeaders(h -> h.header("uniqueId", System.nanoTime()))
				.handle("errorGenerator", "threadAggregator", e -> e.advice(retryAdvice()))
				.log();
	}

	@Bean
	public RequestHandlerRetryAdvice retryAdvice() {

		RequestHandlerRetryAdvice requestHandlerRetryAdvice = new RequestHandlerRetryAdvice();

		RetryTemplate retryTemplate = new RetryTemplate();
		FixedBackOffPolicy policy = new FixedBackOffPolicy();
		policy.setBackOffPeriod(1000);
		retryTemplate.setBackOffPolicy(policy);

		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts(3);
		retryTemplate.setRetryPolicy(retryPolicy);

		requestHandlerRetryAdvice.setRetryTemplate(retryTemplate);

		requestHandlerRetryAdvice.setRecoveryCallback(new ErrorMessageSendingRecoverer(recoveryChannel()));
		//RetryStateGenerator retryStateGenerator = new SpelExpressionRetryStateGenerator("headers['uniqueId']");
		//requestHandlerRetryAdvice.setRetryStateGenerator(retryStateGenerator);
		return requestHandlerRetryAdvice;
	}

	@Bean
	public DirectChannel recoveryChannel() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow recoveryChannelFlow() {
		return IntegrationFlows.from(recoveryChannel())
				.transform("'permanently failed:' + payload.failedMessage.payload + ' handled by recovery'")
				.log()
				.get();
	}

	public static void main(String[] args) {

		try{
			SpringApplication.run(ErrorHandlingApplication.class, args);
		}catch (Exception e){
			System.out.println("--------------------------------------------------------------------------------------------------------" + e);
		}



		/*ConfigurableApplicationContext context = SpringApplication.run(ErrorHandlingApplication.class, args);

		Cafe cafe = context.getBean(Cafe.class);
		cafe.placeOrder(10);*/

	}

}
