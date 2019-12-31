package com.example.integrationdsl;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.BarrierMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.*;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.jpa.dsl.Jpa;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transaction.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SpringBootApplication
@IntegrationComponentScan
public class transactionJPAApplication {

	/*
	send student to studentDataBase		// transactional by default as soon as the database is transactional
	administrativeEnrollment student
	courseEnrollment student
	send student to welcomeFile

	Anotation sur le config de 0welcomeFile @RetainUntilTransactionCommit
	*/

	@Value("${orcha.rollback-transaction-directory}")
	String rollbackTransactionDirectory;

	@MessagingGateway
	public interface School {
		@Gateway(requestChannel = "school.input")
		//@Transactional
		void add(StudentDomain student);
	}

	@Bean
	public IntegrationFlow school() {
		return f -> f
				.handle(Jpa.updatingGateway(this.entityManagerFactory)
						.entityClass(StudentDomain.class)
						.persistMode(PersistMode.PERSIST), e -> e.transactional(true))
				.handle("conditionalService", "administrativeEnrollment")
				.channel("courseEnrollmentChannel.input");
	}

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Bean
	public IntegrationFlow courseEnrollmentChannel() {
		return f -> f
				//.handle("conditionalService", "courseEnrollment", c -> c.transactional(true).advice(expressionAdvice()))
				.handle("conditionalService", "courseEnrollment", c -> c.transactional(true))
				.enrichHeaders(h -> h.header("sendChannel", "send_student_to_welcomeFile"))
				.routeToRecipients(r -> r
						.recipient("outputRetainingAggregatorChannel")
						.recipient("outputChannel")							// next channel
				);
	}

	@Bean
	public DirectChannel outputRetainingAggregatorChannel() {
		return new DirectChannel();
	}

	/**
	 * Act as a buffer of all messages nested into a transactions or messages not transactional
	 * @return
	 */
	@Bean
	public IntegrationFlow outputRetainingAggregatorFlow() {
		return IntegrationFlows.from(outputRetainingAggregatorChannel())
				.aggregate(a ->	a
						.correlationStrategy(transactionCorrelationStrategy)
						.releaseStrategy(transactionReleaseStrategy))
				.routeToRecipients(r -> r
								.recipient("commitedTransactionChannel", "'commited transaction' == payload[payload.size()-1]")
								.recipient("failure.input", "'rolled back transaction' == payload[payload.size()-1]"))
				.get();
	}

	@Autowired
	TransactionCorrelationStrategy transactionCorrelationStrategy;

	@Autowired
	TransactionReleaseStrategy transactionReleaseStrategy;

	@Bean
	public DirectChannel commitedTransactionChannel() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow commitedTransactionFlow() {
		return IntegrationFlows.from(commitedTransactionChannel())
				.split()
				.filter(String.class, m -> m!="commited transaction")
				.route("headers['sendChannel']")
				.get();
	}

	@Bean
	public DirectChannel send_student_to_welcomeFile() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow outputFileFlow() {
		return IntegrationFlows.from(send_student_to_welcomeFile())
				.enrichHeaders(s -> s.headerExpressions(h -> h.put("file_name", "payload.getFirstName()")))
				.transform(Transformers.toJson())
				.handle(Files.outboundAdapter(new File("." + File.separator + "output2")).autoCreateDirectory(true))
				.get();
	}

	@Bean
	public DirectChannel outputChannel() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow outputChannelFlow() {
		return IntegrationFlows.from(outputChannel())
				.handle("conditionalService", "a")
				.log()
				.get();
	}

	/*@Bean
	public Advice expressionAdvice() {
		ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
		//advice.setSuccessChannelName("success.input");
		//advice.setOnSuccessExpressionString("payload + ' was successful'");
		advice.setFailureChannelName("failure.input");
		advice.setOnFailureExpressionString("payload");
		advice.setTrapException(false);
		return advice;
	}*/

	/*@Bean
	public IntegrationFlow success() {
		return f -> f.handle(System.out::println);
	}*/

	@Bean
	public IntegrationFlow failure() {
		return f -> f
				.split()
				.filter(String.class, m -> m!="rolled back transaction")
				.enrichHeaders(s -> s.headerExpressions(h -> h.put("file_name", "payload.getFirstName()")))
				.transform(Transformers.toJson())
				.handle(Files.outboundAdapter(new File( "." + File.separator + rollbackTransactionDirectory))
						.autoCreateDirectory(true));
	}

	/*@Bean
	public IntegrationFlow failure() {
	    return f -> f.<ExpressionEvaluatingRequestHandlerAdvice.MessageHandlingExpressionEvaluatingAdviceException, StudentDomain>transform(m -> (StudentDomain) m.getEvaluationResult())
				.enrichHeaders(s -> s.headerExpressions(h -> h.put("file_name", "payload.getFirstName()")))
				.transform(Transformers.toJson())
				.handle(Files.outboundAdapter(new File( "." + File.separator + rollbackTransactionDirectory))
						.autoCreateDirectory(true));
	}*/

	/*@Bean
	PseudoTransactionManager pseudoTransactionManager(){
		PseudoTransactionManager pseudoTransactionManager = new PseudoTransactionManager();
		return pseudoTransactionManager;
	}

	public TransactionSynchronizationFactory transactionSynchronizationFactory() {
		TransacSynchro syncProcessor = new TransacSynchro();
		return new DefaultTransactionSynchronizationFactory(syncProcessor);
	}

	class TransacSynchro implements TransactionSynchronizationProcessor {
		@Override
		public void processBeforeCommit(IntegrationResourceHolder holder) {
			//System.out.println(holder.getMessage());
		}
		@Override
		public void processAfterCommit(IntegrationResourceHolder holder) {
			Message message = holder.getMessage();
			if(message != null){
				System.out.println("processAfterCommit: " + holder.getMessage());
			}
		}
		@Override
		public void processAfterRollback(IntegrationResourceHolder holder) {
			Message message = holder.getMessage();
			if(message != null){
				System.out.println("processAfterRollback: " + holder.getMessage());
			}
		}
	}*/

	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(transactionJPAApplication.class, args);
		ConditionalService conditionalService = (ConditionalService) context.getBean("conditionalService");

		System.out.println("\nmanyStudentsInSeparatedTransactions is starting\n");

		try{
			conditionalService.manyStudentsInSeparatedTransactions();
		} catch(Exception e){
			System.out.println(">>>>>> Caught exception: " + e);
		}

		System.out.println("\nmanyStudentsInSeparatedTransactions is terminated\n");

		List<?> results = conditionalService.readDatabase();
		System.out.println("database: " + results);

		System.out.println("\nmanyStudentsInTheSameTransaction is starting\n");

		try{
			int i = conditionalService.manyStudentsInTheSameTransaction();
		} catch(Exception e){
			System.out.println(">>>>>>> Caught exception: " + e);
		}

		System.out.println("\nmanyStudentsInTheSameTransaction is terminated\n");

		results = conditionalService.readDatabase();
		System.out.println("database: " + results);

	}
}
