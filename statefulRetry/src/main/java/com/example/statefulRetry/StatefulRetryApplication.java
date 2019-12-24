package com.example.statefulRetry;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.integration.jms.JmsOutboundGateway;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.integration.file.dsl.Files;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class StatefulRetryApplication {

	@Bean
	public IntegrationFlow fileToJMS() {
		return IntegrationFlows.from(Files.inboundAdapter(new File("." + File.separator + "input"))
						.autoCreateDirectory(true)
						.patternFilter("order.json"),
				e -> e.poller(Pollers.fixedDelay(1000)))
				.transform(fileToStringTransformer())
				.transform(Transformers.fromJson(Order.class))
				.enrichHeaders(h -> h.header("uniqueId", System.nanoTime()))
				.handle("processOrder", "confirm")
				.transform(Transformers.toJson())
				.enrichHeaders(h -> h.header("replyChannel", "jmsReplyChannel"))
				.handle(jmsOutboundGateway())
				.get();
	}

	@Bean
	public FileToStringTransformer fileToStringTransformer() {
		return new FileToStringTransformer();
	}

	class ProcessOrder{
		public Order confirm(Order order, @Header("uniqueId") String uniqueId){
			System.out.println("confirm order: " + order + " and uniqueId:" + uniqueId);
			return order;
		}
		public void closeOrder(Order order){
			System.out.println("Response: " + order);
		}
	}

	@Bean
	ProcessOrder processOrder(){
		return new ProcessOrder();
	}

	/*
	With such a gateway a response is received by not in the jmsReplyChannel ?

	@MessagingGateway
	public interface Cafe {
		@Gateway(requestChannel = "orders.input")
		void placeOrder(Order order);
	}

	@Bean
	public IntegrationFlow orders() {
		return f -> f
				.enrichHeaders(h -> h.header("replyChannel", "jmsReplyChannel"))
				.enrichHeaders(h -> h.header("uniqueId", System.nanoTime()))
				.handle("processOrder", "confirm")
				.transform(Transformers.toJson())
				.handle(jmsOutboundGateway())
				.log();
	}*/

	// Note that, if the service is never expected to return a reply, it would be better to use a <int-jms:outbound-channel-adapter/> instead of a <int-jms:outbound-gateway/> with requires-reply="false".
	@Bean
	public JmsOutboundGateway jmsOutboundGateway() {
		JmsOutboundGateway jmsOutboundGateway = new JmsOutboundGateway();
		jmsOutboundGateway.setConnectionFactory(jmsConnectionFactory());
		jmsOutboundGateway.setRequestDestinationName("jmsStatefulRetryTest");
		jmsOutboundGateway.setRequestPubSubDomain(true);		// true for topic, else queue
		jmsOutboundGateway.setDeliveryPersistent(true);

		jmsOutboundGateway.setExplicitQosEnabled(true);
		jmsOutboundGateway.setTimeToLive(0);

		jmsOutboundGateway.setReplyChannel(jmsReplyChannel());
		jmsOutboundGateway.setRequiresReply(true);
		jmsOutboundGateway.setExtractReplyPayload(true);
		jmsOutboundGateway.setReceiveTimeout(15000);
		jmsOutboundGateway.setReplyDestinationName("jmsReplyChannel");
		jmsOutboundGateway.setReplyPubSubDomain(false);

		return jmsOutboundGateway;
	}

	@Bean
	public DirectChannel jmsReplyChannel() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow jmsReplyChannelFlow() {
		return IntegrationFlows.from(jmsReplyChannel())
				.transform(Transformers.fromJson(Order.class))
				.handle("processOrder", "closeOrder")
				.get();
	}

	@Value("${activemq.broker-url}")
	String brokerURL;
	@Value("${activemq.user}")
	String brokerUserName;
	@Value("${activemq.password}")
	String brokerPassword;

	@Bean
	public ConnectionFactory jmsConnectionFactory(){
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
		connectionFactory.setBrokerURL(brokerURL);
		connectionFactory.setUserName(brokerUserName);
		connectionFactory.setPassword(brokerPassword);
		return new ActiveMQConnectionFactory();
	}

	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(StatefulRetryApplication.class, args);
		/*Cafe cafe = context.getBean(Cafe.class);
		cafe.placeOrder(new Order("TV", 1));*/

	}

}
