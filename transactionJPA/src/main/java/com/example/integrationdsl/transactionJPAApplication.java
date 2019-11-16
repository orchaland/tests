package com.example.integrationdsl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.jpa.dsl.Jpa;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


@SpringBootApplication
@IntegrationComponentScan
public class transactionJPAApplication  implements CommandLineRunner {

	class ConditionalService {

		public void failForStudentFemale(StudentDomain student) throws Exception {
			if(student.getGender() == Gender.FEMALE){
				throw new Exception();
			}
		}

	}

	@Bean
	ConditionalService conditionalService(){
		return new ConditionalService();
	}

	@Autowired
	private DataSource dataSource;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	@Qualifier("outboundAdapterFlow")
	private IntegrationFlow outboundAdapterFlowInput;

	@Bean
	//@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = ArrayStoreException.class)
	public IntegrationFlow outboundAdapterFlow() {
		return f -> f
				.handle(Jpa.updatingGateway(this.entityManagerFactory)
								.entityClass(StudentDomain.class)
								.persistMode(PersistMode.PERSIST),
						e -> e.transactional(true)).handle("conditionalService", "failForStudentFemale");

	}

	public static void main(String[] args) {

		SpringApplication.run(transactionJPAApplication.class, args);

	}

	@Override
	public void run(String... args) throws Exception {

		JdbcTemplate jdbcTemplate = null;

		try {

			jdbcTemplate = new JdbcTemplate(this.dataSource);

			Calendar dateOfBirth = Calendar.getInstance();
			dateOfBirth.set(1981, 9, 27);

			StudentDomain male = new StudentDomain()
					.withFirstName("Artem")
					.withLastName("Bilan")
					.withGender(Gender.MALE)
					.withDateOfBirth(dateOfBirth.getTime())
					.withLastUpdated(new Date());

			this.outboundAdapterFlowInput.getInputChannel().send(MessageBuilder.withPayload(male).build());

			StudentDomain female = new StudentDomain()
					.withFirstName("Lucille")
					.withLastName("Bilan")
					.withGender(Gender.FEMALE)
					.withDateOfBirth(dateOfBirth.getTime())
					.withLastUpdated(new Date());

			this.outboundAdapterFlowInput.getInputChannel().send(MessageBuilder.withPayload(female).build());

		} catch (Exception e){
		}

		List<?> results = jdbcTemplate.queryForList("Select * from Student");
		System.out.println("result = " + results);

	}
}
