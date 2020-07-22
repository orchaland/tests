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

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Bean
	public IntegrationFlow pollingAdapterFlow() {
		return IntegrationFlows
				.from(Jpa.inboundAdapter(this.entityManagerFactory)
								.entityClass(StudentDomain.class)
								.maxResults(1)
								.expectSingleResult(true),
						e -> e.poller(p -> p.fixedDelay(5000)))
				.channel("enrollStudentChannel.input")
				.log()
				.get();
	}

	@Bean
	public IntegrationFlow enrollStudentChannel() {
		return f -> f
				.handle("PopulateDatabase", "enrollStudent", c -> c.transactional(true))
				.enrichHeaders(h -> h.headerExpression("messageID", "headers['id'].toString()"))
				.channel("outputRetainingAggregatorChannel");

	}

	@Bean
	public DirectChannel outputRetainingAggregatorChannel() {
		return new DirectChannel();
	}


	@Bean
	public IntegrationFlow outputRetainingAggregatorFlow() {
		return IntegrationFlows.from(outputRetainingAggregatorChannel())
				.aggregate(a -> a
						.releaseExpression("size()==1 and ( ((getMessages().toArray())[0].payload instanceof T(orcha.lang.configuration.Application) AND (getMessages().toArray())[0].payload.state==T(orcha.lang.configuration.Application.State).TERMINATED) )")
						.correlationExpression("headers['messageID']"))
				.get();
	}


	@Bean
	public IntegrationFlow outboundAdapterFlow() {
		return f -> f
				.handle(Jpa.outboundAdapter(this.entityManagerFactory)
								.entityClass(StudentDomain.class)
								.persistMode(PersistMode.PERSIST),
						e -> e.transactional());
	}

	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(transactionJPAApplication.class, args);

		PopulateDatabase populateDatabase = (PopulateDatabase) context.getBean("populateDatabase");
		List<?> results = populateDatabase.readDatabase();

		System.out.println("\nmanyStudentsInValideTransaction is starting\n");
		try {

			StudentDomain student = new StudentDomain("Morgane", 21, 1);
			populateDatabase.saveStudent(student);
			//List<?> results = populateDatabase.readDatabase();
			System.out.println("database: " + results);

		} catch (Exception e) {
			System.out.println(">>>>>> Caught exception: " + e);
		}

		results = populateDatabase.readDatabase();
		System.out.println("database: " + results);
		//List<?> results = populateDatabase.readDatabase();


	}
}