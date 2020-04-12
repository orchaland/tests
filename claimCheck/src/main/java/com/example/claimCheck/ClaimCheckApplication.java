package com.example.claimCheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.ClaimCheckInTransformer;
import org.springframework.integration.transformer.ClaimCheckOutTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.GenericMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
public class ClaimCheckApplication {

	/*
	receive order from customer
	prepare order				=> returns a delivery
	when "prepare terminates"
	deliver prepare.result
	when "deliver terminates"	=> returns a delivery with done = true
	charge deliver.result, order
	 */

	@MessagingGateway
	public interface Cafe {
		@Gateway(requestChannel = "orders.input")
		void placeOrder(Order order);
	}

	@Bean
	public IntegrationFlow orders() {
		return f -> f
				.transform(claimCheckInTransformer())
				.enrichHeaders(e -> e.headerFunction("orderID", (message) -> message.getPayload()))
				.transform(claimCheckOutTransformer())
				.handle(processOrder(), "prepare")
				.handle(processOrder(), "deliver")
				.handle(processOrder(), "arrays")
				.handle(processOrder(), "charge")
				.log();
	}

	@Bean
	MessageStore messageStore(){
		return new SimpleMessageStore();
	}

	@Bean
	public ClaimCheckOutTransformer claimCheckOutTransformer() {
		return new ClaimCheckOutTransformer(messageStore());
	}

	@Bean
	public ClaimCheckInTransformer claimCheckInTransformer() {
		return new ClaimCheckInTransformer(messageStore());
	}

	/*class ProcessOrder{
		public Delivery prepare(Order order){
			System.out.print("prepare order: " + order);
			Delivery delivery = new Delivery(order.getId(), "Av des Champs Elysées, Paris");
			System.out.println(" and return: " + delivery);
			return delivery;
		}
		public Delivery deliver(Delivery delivery){
			System.out.print("deliver delivery: " + delivery);
			delivery.setDone(true);
			System.out.println(" and return: " + delivery);
			return delivery;
		}
		public Bill charge(Delivery delivery, Order order){
			System.out.print("charge delivery: " + delivery + " for order:" + order);
			Bill bill = new Bill(order, 1000);
			System.out.println(" and return: " + bill);
			return bill;
		}
	}*/

	@Bean
	ProcessOrderExtended processOrder(){
		return new ProcessOrderExtended();
	}

	class ProcessOrderExtended extends ProcessOrder{
		public List arrays(GenericMessage genericMessage){
			List array = new ArrayList();
			array.add(genericMessage.getPayload());
			UUID uuid = (UUID) genericMessage.getHeaders().get("orderID");
			Message message = MessageBuilder.withPayload(uuid).build();
			Order order = (Order) claimCheckOutTransformer().transform(message).getPayload();
			array.add(order);
			return array;
		}
		public Bill charge(@Payload("#this[0]")Delivery delivery, @Payload("#this[1]")Order order){
			return super.charge(delivery, order);
		}
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(ClaimCheckApplication.class, args);
		Cafe cafe = context.getBean(Cafe.class);
		cafe.placeOrder(new Order("TV", 1));

	}

}
