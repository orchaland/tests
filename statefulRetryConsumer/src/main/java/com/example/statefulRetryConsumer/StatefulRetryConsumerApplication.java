package com.example.statefulRetryConsumer;

import com.example.statefulRetry.Order;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer;
import org.springframework.integration.handler.advice.RequestHandlerCircuitBreakerAdvice;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.handler.advice.SpelExpressionRetryStateGenerator;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import javax.jms.ConnectionFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class StatefulRetryConsumerApplication {

	@Value("${activemq.broker-url}")
	String brokerURL;
	@Value("${activemq.user}")
	String brokerUserName;
	@Value("${activemq.password}")
	String brokerPassword;

	@Value("${jms.inbound.poller.fixedDelay:500}")
	long fixedDelay;

	class ProcessOrder{

		int nbOfStatefulFailure = 0;

		public Order statefulConfirm(Order order, @Header("uniqueId") String uniqueId) {
			System.out.print("statefulConfirm has been called: " + nbOfStatefulFailure + " times for " + order + " and uniqueId:" + uniqueId + " ");
			if (nbOfStatefulFailure < 1) {
				System.out.println(" => throw Exception");
				nbOfStatefulFailure++;
				throw new RuntimeException();
			} else {
				order.setId(0);
				System.out.println(" => return order: " + order);
				return order;
			}
		}

		int nbOfStatelessFailure = 0;

		public Order statelessConfirm(Order order) {
			System.out.print("statelesConfirm has been called: " + nbOfStatelessFailure + " times for " + order);
			if (nbOfStatelessFailure < 2) {
				System.out.println(" => throw Exception");
				nbOfStatelessFailure++;
				throw new RuntimeException();
			} else {
				order.setId(0);
				System.out.println(" => return order: " + order);
				return order;
			}
		}

		int nbOfFailureForCircuitBreaker = 0;

		public Order confirmWithCircuitBreaker(Order order) {
			System.out.print("confirmWithCircuitBreaker has been called: " + nbOfFailureForCircuitBreaker + " times for " + order);
			if (nbOfFailureForCircuitBreaker < 2) {
				System.out.println(" => throw Exception");
				nbOfFailureForCircuitBreaker++;
				throw new RuntimeException();
			} else {
				order.setId(0);
				System.out.println(" => return order: " + order);
				return order;
			}
		}

		public Object error(Object error){
			System.out.println(error);
			return error;
		}
	}

	@Bean
	ProcessOrder processOrder(){
		return new ProcessOrder();
	}

	@Bean
	public IntegrationFlow jmsInboundGatewayFlow(){
		return IntegrationFlows.from(Jms.inboundGateway(jmsConnectionFactory())
				.destination("jmsStatefulRetryTest")
				.configureListenerContainer(spec -> spec
								.pubSubDomain(true)				// true for topic, else queue
								.clientId("clientID1")			// for durable subscription
								.subscriptionDurable(true)
								.durableSubscriptionName("durableSubscriptionName"))
				.requestChannel(jmsInboundChannel())
				.replyChannel(jmsReplyChannel())
				.replyDeliveryPersistent(true)
				.extractRequestPayload(true))
				.get();
	}

	@Bean
	public ConnectionFactory jmsConnectionFactory(){
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
		connectionFactory.setBrokerURL(brokerURL);
		connectionFactory.setUserName(brokerUserName);
		connectionFactory.setPassword(brokerPassword);
		return new ActiveMQConnectionFactory();
	}

	/*@Bean
	public PollableChannel jmsInboundChannel() {
		return new QueueChannel();
	}*/

	@Bean
	public DirectChannel jmsInboundChannel() {
		return new DirectChannel();
	}

	@Bean
	public DirectChannel jmsReplyChannel() {
		return new DirectChannel();
	}

	@Bean
	//@Transactional(value="jmsTransactionManager", propagation = Propagation.NESTED)
	public IntegrationFlow serviceFlow() {
		return IntegrationFlows.from(jmsInboundChannel())
				.transform(Transformers.fromJson(Order.class), e -> e.transactional(this.jmsTransactionManager()))
				.handle("processOrder", "statefulConfirm", e -> e.advice(requestHandlerStatefullRetryAdvice()))
				.handle("processOrder", "statelessConfirm", e -> e.advice(requestHandlerStatelessRetryAdvice()))
				.handle("processOrder", "confirmWithCircuitBreaker", e -> e.advice(requestHandlerCircuitBreakerAdvice()))
				.transform(Transformers.toJson())
				.channel(jmsReplyChannel())
				.get();
	}

	@Bean
	public JmsTransactionManager jmsTransactionManager() {
		JmsTransactionManager jmsTransactionManager = new JmsTransactionManager();
		jmsTransactionManager.setConnectionFactory(jmsConnectionFactory());
		return jmsTransactionManager;
	}

	@Bean
	public RequestHandlerRetryAdvice requestHandlerStatefullRetryAdvice(){
		RequestHandlerRetryAdvice retryAdvice = new RequestHandlerRetryAdvice();
		retryAdvice.setRecoveryCallback(new ErrorMessageSendingRecoverer(recoveryChannel()));
		retryAdvice.setRetryStateGenerator(new SpelExpressionRetryStateGenerator("headers['uniqueId']"));
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(4));
		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		backOffPolicy.setInitialInterval(1000);
		backOffPolicy.setMultiplier(2.0);
		backOffPolicy.setMaxInterval(4000);
		retryTemplate.setBackOffPolicy(backOffPolicy);
		retryAdvice.setRetryTemplate(retryTemplate);

		/*ExponentialRandomBackOffPolicy exponentialRandomBackOffPolicy = new ExponentialRandomBackOffPolicy();

		FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
		fixedBackOffPolicy.setBackOffPeriod(1000);*/

		return retryAdvice;
	}

	@Bean
	public RequestHandlerRetryAdvice requestHandlerStatelessRetryAdvice(){
		RequestHandlerRetryAdvice retryAdvice = new RequestHandlerRetryAdvice();
		retryAdvice.setRecoveryCallback(new ErrorMessageSendingRecoverer(recoveryChannel()));
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(4));
		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		backOffPolicy.setInitialInterval(1000);
		backOffPolicy.setMultiplier(5.0);
		backOffPolicy.setMaxInterval(60000);
		retryTemplate.setBackOffPolicy(backOffPolicy);
		retryAdvice.setRetryTemplate(retryTemplate);

		/*ExponentialRandomBackOffPolicy exponentialRandomBackOffPolicy = new ExponentialRandomBackOffPolicy();

		FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
		fixedBackOffPolicy.setBackOffPeriod(1000);*/

		return retryAdvice;
	}

	@Bean
	public RequestHandlerCircuitBreakerAdvice requestHandlerCircuitBreakerAdvice(){
		RequestHandlerCircuitBreakerAdvice circuitBreakerAdvice = new RequestHandlerCircuitBreakerAdvice();
		circuitBreakerAdvice.setThreshold(2);
		circuitBreakerAdvice.setHalfOpenAfter(1000);
		return circuitBreakerAdvice;
	}

	@Bean
	public DirectChannel recoveryChannel() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow recoveryChannelFlow() {
		return IntegrationFlows.from(recoveryChannel())
				.log()
				.get();
	}

	@Bean
	public DirectChannel jmsErrorChannel() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow jmsErrorChannelFlow() {
		return IntegrationFlows.from(jmsErrorChannel())
				.handle("processOrder", "error")
				.log()
				.get();
	}

	public static void main(String[] args) {
		SpringApplication.run(StatefulRetryConsumerApplication.class, args);
	}

}
