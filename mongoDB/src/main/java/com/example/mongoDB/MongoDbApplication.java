package com.example.mongoDB;


import com.mongodb.Mongo;
import com.mongodb.client.MongoClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.PriorityChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.mongodb.store.MongoDbChannelMessageStore;
import org.springframework.integration.mongodb.store.MongoDbMessageStore;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.store.BasicMessageGroupStore;
import org.springframework.integration.store.MessageGroupQueue;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;


@SpringBootApplication
public class MongoDbApplication {

	@MessagingGateway
	public interface Cafe {
		@Gateway(requestChannel = "orders.input")
		void placeOrder(int order);
	}

	@Bean(name = PollerMetadata.DEFAULT_POLLER)
	public PollerMetadata poller() {
		return Pollers.fixedDelay(1000).get();
	}

	@Bean
	public IntegrationFlow orders() {
		return f -> f.
				channel(priorityQueue())
				.log();
	}

	@Bean
	public MongoDbFactory mongoDbFactory(){
		return new SimpleMongoClientDbFactory(MongoClients.create(), "test");
	}

	@Bean
	public BasicMessageGroupStore mongoDbChannelMessageStore() {
		MongoDbChannelMessageStore store = new MongoDbChannelMessageStore(mongoDbFactory());
		store.setPriorityEnabled(true);
		return store;
	}

	@Bean
	public PollableChannel priorityQueue() {
		return new PriorityChannel(new MessageGroupQueue(mongoDbChannelMessageStore(), "priorityQueue"));
	}

	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(MongoDbApplication.class, args);

		Cafe cafe = context.getBean(Cafe.class);
		cafe.placeOrder(10);

	}

}
