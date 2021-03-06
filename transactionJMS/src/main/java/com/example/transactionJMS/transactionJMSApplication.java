package com.example.transactionJMS;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.*;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.integration.jms.JmsInboundGateway;
import org.springframework.integration.jms.JmsOutboundGateway;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.messaging.PollableChannel;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ErrorHandler;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Topic;
import java.io.File;

@SpringBootApplication
@Configuration
@ImportAutoConfiguration({ ActiveMQAutoConfiguration.class, JmxAutoConfiguration.class, IntegrationAutoConfiguration.class })
@IntegrationComponentScan
@ComponentScan
public class transactionJMSApplication implements CommandLineRunner {

	@Bean
	ConditionalService conditionalService(){
		return new ConditionalService();
	}

	@Bean
	public FileToStringTransformer fileToStringTransformer() {
		return new FileToStringTransformer();
	}

	@Value("${activemq.broker-url}")
	String brokerURL;
	@Value("${activemq.user}")
	String brokerUserName;
	@Value("${activemq.password}")
	String brokerPassword;

	@Bean
	public ConnectionFactory jmsConnectionFactory(){
		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
		activeMQConnectionFactory.setBrokerURL(brokerURL);
		activeMQConnectionFactory.setUserName(brokerUserName);
		activeMQConnectionFactory.setPassword(brokerPassword);
		return new ActiveMQConnectionFactory();
	}

	@Autowired
	ConnectionFactory jmsConnectionFactory;

	@Bean
	public JmsTransactionManager jmsTransactionManager() {
		JmsTransactionManager jmsTransactionManager = new JmsTransactionManager();
		jmsTransactionManager.setConnectionFactory(this.jmsConnectionFactory);
		return jmsTransactionManager;
	}

	@Bean
	//@Transactional(value="jmsTransactionManager", propagation = Propagation.REQUIRES_NEW)
	public IntegrationFlow fileToJMS() {
		return IntegrationFlows.from(Files.inboundAdapter(new File("." + File.separator + "input"))
						.autoCreateDirectory(true)
						.patternFilter("*.txt"),
				e -> e.poller(Pollers.fixedDelay(5000)))
				.transform(fileToStringTransformer())
				.handle("conditionalService", "beforeSendingJMS")
				.channel(fileToJMSChannel())
				.get();
	}

	@Bean
	public DirectChannel fileToJMSChannel() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow jmsOutboundGatewayFlow() {
		return IntegrationFlows.from(fileToJMSChannel())
				.enrichHeaders(h -> h.header("JMSReplyTo", "jmsReplyDestinationName"))
				.handle(jmsOutboundGateway())
				.get();
	}

	// Note that, if the service is never expected to return a reply, it would be better to use a <int-jms:outbound-channel-adapter/> instead of a <int-jms:outbound-gateway/> with requires-reply="false".
	@Bean
	public JmsOutboundGateway jmsOutboundGateway() {
		JmsOutboundGateway jmsOutboundGateway = new JmsOutboundGateway();
		jmsOutboundGateway.setConnectionFactory(this.jmsConnectionFactory);
		jmsOutboundGateway.setRequestDestinationName("jmsPipelineTest");
		jmsOutboundGateway.setRequestPubSubDomain(true);		// true for topic, else queue
		jmsOutboundGateway.setReplyChannel(jmsReplyChannel());
		jmsOutboundGateway.setReplyDestinationName("jmsReplyDestinationName");

		jmsOutboundGateway.setExtractRequestPayload(true);
		jmsOutboundGateway.setExtractReplyPayload(true);

		jmsOutboundGateway.setDeliveryPersistent(true);
		jmsOutboundGateway.setReceiveTimeout(1000);

		jmsOutboundGateway.setExplicitQosEnabled(true);
		jmsOutboundGateway.setTimeToLive(0);

		jmsOutboundGateway.setRequiresReply(true);

		return jmsOutboundGateway;
	}

	@Bean
	public DirectChannel jmsReplyChannel() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow jmsReplyChannelFlow() {
		return IntegrationFlows.from(jmsReplyChannel())
				.handle("conditionalService", "jmsReplyChannelFlow")
				.log()
				.get();
	}

	public static void main(String[] args) {

		SpringApplication.run(transactionJMSApplication.class, args);

	}

	@Override
	public void run(String... args) throws Exception {


	}
}
