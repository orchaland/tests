package com.example.transactionJMS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.*;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.ConnectionFactory;
import java.io.File;

@SpringBootApplication
//@IntegrationComponentScan
@Configuration
@ImportAutoConfiguration({ ActiveMQAutoConfiguration.class, JmxAutoConfiguration.class, IntegrationAutoConfiguration.class })
@IntegrationComponentScan
@ComponentScan
public class transactionJMSApplication implements CommandLineRunner {

	@Bean
	public FileToStringTransformer fileToStringTransformer() {
		return new FileToStringTransformer();
	}

	@Bean
	@Transactional(value="jmsTransactionManager", propagation = Propagation.REQUIRES_NEW)
	public IntegrationFlow fileToJMS() {
		return IntegrationFlows.from(Files.inboundAdapter(new File("." + File.separator + "input"))
						.autoCreateDirectory(true)
						.patternFilter("*.txt"),
				e -> e.poller(Pollers.fixedDelay(5000)))
				.transform(fileToStringTransformer())
				.transform("payload.replaceAll('\r\n', '\n')")
				.handle("conditionalService", "failForStudentFemale")
				.channel(jmsOutboundGatewayFlow().getInputChannel())
				.get();
	}

	/*@Transactional(value="jmsTransactionManager", propagation = Propagation.REQUIRES_NEW)
	public Message readAndProcessMessage() throws JMSException
	{
		Message jmsMessage = jmsTemplate.receive(heldTransmissionDestination);
		if(jmsMessage != null)
		{
			process(jmsMessage);
		}
		//have to return to break out of the while in the caller
		return jmsMessage;
	}

	@Transactional(value="jmsTransactionManager", propagation = Propagation.NESTED)
	protected void process(Message jmsMessage)
	{
		//code to process the jmsMessage, can potentially throw
		//an exception that requires rolling back the jms transaction
	}*/


	class ConditionalService {

		public Object failForStudentFemale(Object student) throws Exception {
			System.out.println("----------- " + student);
			/*if(student.getGender() == Gender.FEMALE){
				throw new Exception();
			}*/
			return student;
		}

	}

	@Bean
	ConditionalService conditionalService(){
		return new ConditionalService();
	}

	@Autowired
	private ConnectionFactory jmsConnectionFactory;

	@Bean
	public JmsTransactionManager jmsTransactionManager() {
		JmsTransactionManager jmsTransactionManager = new JmsTransactionManager();
		jmsTransactionManager.setConnectionFactory(this.jmsConnectionFactory);
		return jmsTransactionManager;
	}

	@Bean
	public IntegrationFlow jmsOutboundGatewayFlow() {
		return f -> f.handle(Jms.outboundAdapter(this.jmsConnectionFactory)
				.destination("jmsPipelineTest"),e -> e.transactional(true));
	}

	@Bean
	public PollableChannel jmsInboundChannel() {
		return new QueueChannel();
	}

	@Bean
	public IntegrationFlow jmsInboundGatewayFlow(){
		return IntegrationFlows.from(Jms.inboundGateway(this.jmsConnectionFactory)
				.destination("jmsPipelineTest")
				.requestChannel(jmsInboundChannel()))
				.get();
	}

	@Bean
	public IntegrationFlow fileWritingFlow() {
		return IntegrationFlows.from(jmsInboundChannel())
				.enrichHeaders(h -> h.header("directory", new File("." + File.separator + "output")))
				.handle(Files.outboundGateway(new File( "." + File.separator + "output")).autoCreateDirectory(true) )
				.handle("conditionalService", "failForStudentFemale")
				.log()
				.get();
	}

	/*@Bean
	public IntegrationFlow fileWritingFlow1() {
		return IntegrationFlows.from(jmsInboundChannel())
				.enrichHeaders(h -> h.header("directory", new File("." + File.separator + "output")))
				.handle(Files.outboundGateway(new File( "." + File.separator + "output")).autoCreateDirectory(true) )
				.handle("conditionalService", "failForStudentFemale")
				.log()
				.get();
	}*/

	@Bean(name = PollerMetadata.DEFAULT_POLLER)
	public PollerMetadata fileWritingPoller() {
		return Pollers.fixedRate(500).get();
	}

	public static void main(String[] args) {

		SpringApplication.run(transactionJMSApplication.class, args);

	}

	@Override
	public void run(String... args) throws Exception {

		/*PollableChannel replyChannel = new QueueChannel();

		Message<String> message = MessageBuilder.withPayload("hello through the jms pipeline")
				.setHeader("destination", "jmsPipelineTest")
				.build();

		this.jmsOutboundGateway.getInputChannel().send(message);*/


		//Message<?> receive = jmsOutboundInboundReplyChannel().receive(5000);

		//System.out.println("-----------------recu = " + receive);

	}
}
