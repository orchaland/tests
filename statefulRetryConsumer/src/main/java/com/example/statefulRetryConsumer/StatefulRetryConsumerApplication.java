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
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.handler.advice.SpelExpressionRetryStateGenerator;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;

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

		int nbOfFailure = 0;

		public Order confirm(Order order, @Header("uniqueId") String uniqueId) {
			System.out.print("confirm has been called: " + nbOfFailure + " times for " + order + " and uniqueId:" + uniqueId + " ");
			if (nbOfFailure < 1) {
				System.out.print(" => throw Exception");
				nbOfFailure++;
				throw new RuntimeException();
			} else {
				order.setId(0);
				System.out.print(" => return order: " + order);
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
				.handle("processOrder", "confirm", e -> e.advice(requestHandlerRetryAdvice()))	// The entire flow is transactional (for example, if there is a downstream outbound channel adapter)
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
	public RequestHandlerRetryAdvice requestHandlerRetryAdvice(){
		RequestHandlerRetryAdvice retryAdvice = new RequestHandlerRetryAdvice();
		retryAdvice.setRecoveryCallback(new ErrorMessageSendingRecoverer(recoveryChannel()));
		retryAdvice.setRetryStateGenerator(new SpelExpressionRetryStateGenerator("headers['uniqueId']"));
		return retryAdvice;
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
