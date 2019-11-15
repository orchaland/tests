package com.example.integrationdsl;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.TransactionManager;

import javax.naming.Context;
import java.io.File;


@SpringBootApplication
public class IntegrationdslApplication {


	public static void main(String[] args) {

		/*try {
			CodeGeneration codeGeneration = new CodeGeneration();
			codeGeneration.generate();
		} catch (Exception e){
			e.printStackTrace();
		}*/

		new SpringApplicationBuilder(IntegrationdslApplication.class).web(WebApplicationType.NONE).run(args);

	}

}
