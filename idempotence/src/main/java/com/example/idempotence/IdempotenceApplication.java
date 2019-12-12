package com.example.idempotence;

import com.mongodb.client.MongoClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.mongodb.metadata.MongoDbMetadataStore;
import org.springframework.integration.mongodb.store.MongoDbMessageStore;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.transformer.ClaimCheckInTransformer;
import org.springframework.integration.transformer.Transformer;
import org.springframework.integration.transformer.support.ExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.messaging.MessageHeaders;

@SpringBootApplication
public class IdempotenceApplication {

	@MessagingGateway
	public interface Cafe {
		@Gateway(requestChannel = "orders.input")
		void placeOrder(Order order);
	}

	class ProcessOrder{
		public Order confirm(Order order){
			System.out.println("confirm order: " + order);
			return order;
		}
		public Order closeOrder(Order order){
			System.out.println("close order: " + order);
			return order;
		}
	}

	@Bean
	ProcessOrder processOrder(){
		return new ProcessOrder();
	}

	@Bean(name = PollerMetadata.DEFAULT_POLLER)
	public PollerMetadata poller() {
		return Pollers.fixedDelay(1000).get();
	}

	@Bean
	public IntegrationFlow orders() {
		return f -> f
				.enrichHeaders(e -> e.headerFunction("orderID", (message) -> ((Order)message.getPayload()).getId()))
				.handle(processOrder(), "confirm", e -> e.advice(idempotentReceiverInterceptor()))
				.channel(afterServiceChannel())
				.log();
	}

	@Bean
	IdempotentReceiverInterceptor idempotentReceiverInterceptor() {
		IdempotentReceiverInterceptor interceptor = new IdempotentReceiverInterceptor(
				new MetadataStoreSelector(m -> (String) m
						.getHeaders()
						//.get(MessageHeaders.TIMESTAMP).toString(),
						.get("orderID").toString(),
						mongoDbMetadataStore(mongoDbFactory())));
		interceptor.setDiscardChannel(afterServiceChannel());
		return interceptor;
	}

	@Bean
	public MongoDbMetadataStore mongoDbMetadataStore(MongoDbFactory factory) {
		return new MongoDbMetadataStore(factory, "integrationMetadataStore");
	}

	@Bean
	public MongoDbFactory mongoDbFactory(){
		MongoDbFactory mongoDbFactory = new SimpleMongoClientDbFactory(MongoClients.create(), "test");
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.dropCollection("integrationMetadataStore");
		return mongoDbFactory;
	}

	@Bean
	public DirectChannel afterServiceChannel() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow afterServiceChannelFlow() {
		return IntegrationFlows.from(afterServiceChannel())
				.handle(processOrder(), "closeOrder")
				.log()
				.get();
	}

	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(IdempotenceApplication.class, args);

		Cafe cafe = context.getBean(Cafe.class);

		cafe.placeOrder(new Order("TV", 1));

		cafe.placeOrder(new Order("TV", 2));

		cafe.placeOrder(new Order("TV", 2));

	}

}
