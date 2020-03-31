package com.example.gettingStarted;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
/*import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.ClaimCheckInTransformer;
import org.springframework.integration.transformer.ClaimCheckOutTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.GenericMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
*/
@SpringBootApplication
public class GettingStartedApplication {

	/*
	receive order from customer
	prepare order				=> returns a delivery
	when "prepare terminates"
	deliver prepare.result
	when "deliver terminates"	=> returns a delivery with done = true
	charge deliver.result, order
	 */
/*
	@Bean
	public IntegrationFlow fileReadingFlow() {
		return IntegrationFlows.from(Files.inboundAdapter(new File(".\\files")).patternFilter("*.json"),
				a -> a.poller(Pollers.fixedDelay(1000)))
				.transform(Files.toStringTransformer())
				.transform(Transformers.fromJson(Order.class))
				.channel("processFileChannel").get();
	}

	@Bean
	public MessageChannel processFileChannel() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow processFileChannelFlow() {
		return IntegrationFlows.from(processFileChannel())
				.handle("processOrder", "prepare")
				.get();
	}

	class ProcessOrder{
		public void prepare(Order order){
			System.out.println("prepare: " + order);
		}
	}

	@Bean
	ProcessOrder processOrder(){
		return new ProcessOrder();
	}
*/
	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(GettingStartedApplication .class, args);
	}

}
