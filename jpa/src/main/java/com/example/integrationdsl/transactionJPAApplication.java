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

	@Bean
	public IntegrationFlow pollingAdapterFlow() {
		return IntegrationFlows
				.from(Jpa.inboundAdapter(this.entityManagerFactory)
								.entityClass(StudentDomain.class)
								.maxResults(1)
								.expectSingleResult(true),
						e -> e.poller(p -> p.fixedDelay(5000)))
						//e -> e.poller(p -> p.trigger(new CronTrigger("0 0-5 1 * * ?"))))
				.log()
				//.channel(c -> c.queue("pollingResults"))
				.get();
	}

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(transactionJPAApplication.class, args);

		PopulateDatabase populateDatabase = (PopulateDatabase)context.getBean("populateDatabase");

		try{

			StudentDomain student = new StudentDomain("Morgane", 21, 1);
			populateDatabase.saveStudent(student);

			List<?> results = populateDatabase.readDatabase();
			System.out.println("database: " + results);

		} catch(Exception e){
			System.out.println(">>>>>> Caught exception: " + e);
		}
	}
}
