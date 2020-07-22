package com.example.integrationdsl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.*;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.jpa.dsl.Jpa;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.scheduling.support.CronTrigger;

import javax.persistence.EntityManagerFactory;
import java.io.File;
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
				.split()
				.filter(String.class, m -> m!="commited transaction")
				.route("headers['sendChannel']")
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

	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(transactionJPAApplication.class, args);

		ConditionalService conditionalService = (ConditionalService) context.getBean("conditionalService");

		System.out.println("\nmanyStudentsInValideTransaction is starting\n");

		try{
			conditionalService.manyStudentsInValideTransaction();
		} catch(Exception e){
			System.out.println(">>>>>> Caught exception: " + e);
		}

		System.out.println("\nmanyStudentsInValideTransaction is terminated\n");

		List<?> results = conditionalService.readDatabase();
		System.out.println("database: " + results);

		System.out.println("\nmanyStudentsInUnvalideTransaction is starting\n");

		try{
			int i = conditionalService.manyStudentsInUnvalideTransaction();
		} catch(Exception e){
			System.out.println(">>>>>>> Caught exception: " + e);
		}

		System.out.println("\nmanyStudentsInUnvalideTransaction is terminated\n");

		results = conditionalService.readDatabase();
		System.out.println("database: " + results);

	}
}
