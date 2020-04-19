package com.example.gettingStarted;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.file.dsl.Files;
import org.springframework.messaging.MessageChannel;

import java.io.File;

@SpringBootApplication
public class GettingStartedApplication {

    /**
     * Read the order.json file
     * @return
     */
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

    /**
     * Call the prepare method of the ProcessOrder class
     * @return
     */
    @Bean
    public IntegrationFlow processFileChannelFlow() {
        return IntegrationFlows.from(processFileChannel())
                .handle("processOrder", "prepare")
                .get();
    }

   /* class ProcessOrder{
        public void prepare(Order order){
            System.out.println("prepare: " + order);
        }
    }*/

    @Bean
    ProcessOrder processOrder(){
        return new ProcessOrder();
    }

    public static void main(String[] args){
        SpringApplication.run(GettingStartedApplication .class, args);
    }
}
