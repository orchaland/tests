# tests

The goal of this intership is to generate programmatically code for Spring
Integration from an Orcha program (http://www.orchalang.com/)

## Generate Spring Integration Java DSL

Code to be generated: https://github.com/orchaland/tests/blob/master/generateDSL/src/main/java/com/example/integrationdsl/RestWebService.java

Code generation: https://github.com/orchaland/tests/blob/master/generateDSL/src/main/java/com/example/integrationdsl/CodeGeneration.java

## Getting Started

Software to be installed:
- Java version jdk 1.8
- Intellij
- git client

### Clone this project
Use the git bash to clone this project:
```java
git clone https://github.com/orchaland/tests
```

### Project importation
This project is compose of several independant projects. Open the getting started sub project inside Intellij.

### Run
The main program is inside this class: https://github.com/orchaland/tests/blob/master/gettingStarted/src/main/java/com/example/gettingStarted/GettingStartedApplication.java

Run this program inside Intellij (if nothing happens close and open again the project inside Intellij): the Intellij console should display: 
```java
prepare: Order{product='TV', id=1}
 ```

### What happened
A file containing: 
```java
{"product":"TV","id":1}
```
has been read, then a Json converter create an instance of this class: https://github.com/orchaland/tests/blob/master/gettingStarted/src/main/java/com/example/gettingStarted/Order.java
```java
    @Bean
    public IntegrationFlow fileReadingFlow() {
        return IntegrationFlows.from(Files.inboundAdapter(new File(".\\files")).patternFilter("*.json"),
                a -> a.poller(Pollers.fixedDelay(1000)))
                .transform(Files.toStringTransformer())
                .transform(Transformers.fromJson(Order.class))
                .channel("processFileChannel").get();
    }
```

A channel receives the object:
```java
    @Bean
    public MessageChannel processFileChannel() {
        return new DirectChannel();
    }
 ```
Then, thanks to the handle method, it is processed by a prepare method: 
```java
    class ProcessOrder{
        public void prepare(Order order){
            System.out.println("prepare: " + order);
        }
    }

```

### Orcha translation

The equivalent Orcha program is like:
```java
receive order
prepare order
```

## Claim check

With the claim check property, a service can use again any data previously processed in the process. In the following example, although order is processed at line “prepare order”, it can be used at the last line “charge deliver.result, order”:

```java
receive order from customer
prepare order               // returns a delivery
when "prepare terminates"
deliver prepare.result        // returns a delivery with done = true
when "deliver terminates"
charge deliver.result, order    // claim-check : retrieve the order
```

Spring Integration: https://docs.spring.io/spring-integration/docs/5.2.3.RELEASE/reference/html/index-single.html#claim-check

Implementation: https://github.com/orchaland/tests/blob/master/claimCheck/src/main/java/com/example/claimCheck/ClaimCheckApplication.java

## Error handling

Spring integration: https://docs.spring.io/spring-integration/docs/5.2.3.RELEASE/reference/html/index-single.html#error-handling

test: https://github.com/orchaland/tests/blob/master/errorHandling/src/main/java/com/example/errorHandling/ErrorHandlingApplication.java

## Retry

Spring integration: 

https://docs.spring.io/spring-integration/docs/5.2.3.RELEASE/reference/html/index-single.html#retry-advice

https://docs.spring.io/spring-integration/docs/5.2.3.RELEASE/reference/html/index-single.html#idempotent-receiver

Recovery of interrupted Smart contracts
Advices (Aspect Oriented Programming) are added to each service call (Orcha compute instruction):
the returned state of a succeeded service if stored as an event inside a persistent storage, so, when a smart contact is recovering from a failure, the service is never called again but its final state is recovered from the stored event instead.
each failed service is retried with a state full retry (https://docs.spring.io/spring-batch/docs/current/reference/html/retry.html), i.e. the context that was on the stack is stored when an exception occurs, and recovered during the retry
after many attempts, is the retry failed again, a recover method is called. Its role is defined as a failed clause in the Orcha Smart Contract Player… on fait quoi ?
Retry policy

Stateless => même thread
Stateful = > dans un autre thread

When a service fails it can retry automatically. Suppose the Orcha instruction:

```java
controldentity with person
```

To make the service controldentity retryable it must be annotated with @Retry:

```java
@Configuration
class BoardingConfiguration {

   @Bean
   @Retry(intervalBetweenTheFirstAndSecondAttempt=1000, intervalMultiplierBetweenAttemps=5.0, maximumIntervalBetweenAttempts=60000)
   fun controlIdentity(): Application {
       val application = Application("controlIdentity", "Java")
       val javaAdapter = JavaServiceAdapter("service.airport.ControlPassengerIdentity", "control")
       application.input = Input(adapter=javaAdapter, type="service.airport.Person")
       application.output = Output(adapter=javaAdapter, type="service.airport.Passenger")
       return application
   }

}
```

Peut-être niveau technique (=> throw erreur) ou métier 
If after the configured attempts the service still fails, this permanent failure must be taken into account by Orcha with:

```java
when “controlIdentity fails” …
```

There are situations where you don’t want a service to retry automatically after a failure, but you expect the caller resubmits the request, i.e. the smart contract is launched again. Nevertheless, the state of the previous attempts must be recovered for the new retry. The exactly what a stateful retry does. Such a retry must be declared with the @StatefulRetry  annotation:

In memory by default !

```java
@Configuration
class BoardingConfiguration {

   @Bean
   @StatefulRetry(intervalBetweenTheFirstAndSecondAttempt=1000, intervalMultiplierBetweenAttemps=5.0, maximumIntervalBetweenAttempts=60000)
   fun controlIdentity(): Application {
       val application = Application("controlIdentity", "Java")
       val javaAdapter = JavaServiceAdapter("service.airport.ControlPassengerIdentity", "control")
       application.input = Input(adapter=javaAdapter, type="service.airport.Person")
       application.output = Output(adapter=javaAdapter, type="service.airport.Passenger")
       return application
   }

}
```

The way the failed operations are recognized is by identifying the state across multiple invocations of the retry. To identify the state, a field among all the fields in the input of the service must be annotated with @StatefulRetryDiscriminant: 

```java
class Person (val name: String, @StatefulRetryDiscriminant val passportID: String)
```

Timestamp pour StatefulRetryDiscriminant tant que le service n’a pas réussit le client garde l’id

If after the configured attempts the service still fails, this permanent failure must be taken into account by Orcha with:

```java
when “controlIdentity fails” …
```

Implementation: 

https://github.com/orchaland/tests/blob/master/statefulRetry/src/main/java/com/example/statefulRetry/StatefulRetryApplication.java

https://github.com/orchaland/tests/blob/master/statefulRetryConsumer/src/main/java/com/example/statefulRetryConsumer/StatefulRetryConsumerApplication.java

https://github.com/orchaland/tests/blob/master/idempotence/src/main/java/com/example/idempotence/IdempotenceApplication.java


Often, you must combine stateful retry with idempotence since a stateful retry occurs when a smart contract is launched again: stateful retry for services that can fail and idempotence to never called twice the other services when the retry occurs. 

## Transactions

Spring Integration : https://docs.spring.io/spring-integration/docs/5.2.3.RELEASE/reference/html/index-single.html#transactions

### Transaction with a relational database 

Implementation: https://github.com/orchaland/tests/blob/master/transactionJPA/src/main/java/com/example/integrationdsl/transactionJPAApplication.java

### Transaction with a message broker 

ActiveMQ is used as a message broker. Docker is used to install and launch the broker. The Dockerfile is there: https://github.com/orchaland/tests/tree/master/brokers/activemq

Build the Docker image with (from the directory containing the Dockerfile): 

```java
docker build -t active-mq .
```

Run the container: 

```java
docker run -p 61616:61616 -p 8161:8161 active-mq
```

Open the activemq admin console: http://localhost:8161/

Then select: Manage ActiveMQ broker

login: admin

password: admin

Implementation: 

Message producer: https://github.com/orchaland/tests/blob/master/transactionJMS/src/main/java/com/example/transactionJMS/transactionJMSApplication.java

Message consumer: https://github.com/orchaland/tests/blob/master/transactionJMSConsumer/src/main/java/com/example/transactionJMSConsumer/transactionJMSConsumerApplication.java

Launch the consumer first, then the producer.

A queue should has been created: jmsReplyDestinationName

A topice should has been created: jmsPipelineTest


## Message store mongoDB
Download image mongo : docker pull mongo

Run the container: docker run -d -p 27017-27019:27017-27019 --name mongodb mongo

connect to our deployment mongodb: docker exec -it mongodb bash

Lauch MongoDB shell client : mongo

show dbs

use test 

show collections

db.collection.find()
