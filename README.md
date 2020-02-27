# tests

The goal of this intership is to generate programmatically code for Spring
Integration from an Orcha program (http://www.orchalang.com/)

## Generate Spring Integration Java DSL

Code to be generated: https://github.com/orchaland/tests/blob/master/generateDSL/src/main/java/com/example/integrationdsl/RestWebService.java

Code generation: https://github.com/orchaland/tests/blob/master/generateDSL/src/main/java/com/example/integrationdsl/CodeGeneration.java

## Claim check

With the claim check property, a service can use again any data previously processed in the process. In the following example, although order is processed at line “prepare order”, it can be used at the last line “charge deliver.result, order”:

<code>
receive order from customer<br>
prepare order               // returns a delivery<br><br>
when "prepare terminates"<br>
deliver prepare.result        // returns a delivery with done = true<br>
when "deliver terminates"<br>      
charge deliver.result, order    // claim-check : retrieve the order<br>
</code>

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

<code>controldentity with person</code>

To make the service controldentity retryable it must be annotated with @Retry:

<code>
@Configuration<br>
class BoardingConfiguration {<br>

   @Bean<br>
   @Retry(intervalBetweenTheFirstAndSecondAttempt=1000, intervalMultiplierBetweenAttemps=5.0, maximumIntervalBetweenAttempts=60000)<br>
   fun controlIdentity(): Application {<br>
       val application = Application("controlIdentity", "Java")<br>
       val javaAdapter = JavaServiceAdapter("service.airport.ControlPassengerIdentity", "control")<br>
       application.input = Input(adapter=javaAdapter, type="service.airport.Person")<br>
       application.output = Output(adapter=javaAdapter, type="service.airport.Passenger")<br>
       return application<br>
   }<br>

}<br>
</code>

Peut-être niveau technique (=> throw erreur) ou métier 
If after the configured attempts the service still fails, this permanent failure must be taken into account by Orcha with:

<code>when “controlIdentity fails” …</code>

There are situations where you don’t want a service to retry automatically after a failure, but you expect the caller resubmits the request, i.e. the smart contract is launched again. Nevertheless, the state of the previous attempts must be recovered for the new retry. The exactly what a stateful retry does. Such a retry must be declared with the @StatefulRetry  annotation:

In memory by default !

<code>
@Configuration<br>
class BoardingConfiguration {<br>

   @Bean<br>
   @StatefulRetry(intervalBetweenTheFirstAndSecondAttempt=1000, intervalMultiplierBetweenAttemps=5.0, maximumIntervalBetweenAttempts=60000)<br>
   fun controlIdentity(): Application {<br>
       val application = Application("controlIdentity", "Java")<br>
       val javaAdapter = JavaServiceAdapter("service.airport.ControlPassengerIdentity", "control")<br>
       application.input = Input(adapter=javaAdapter, type="service.airport.Person")<br>
       application.output = Output(adapter=javaAdapter, type="service.airport.Passenger")<br>
       return application<br>
   }<br>

}<br>
</code>

The way the failed operations are recognized is by identifying the state across multiple invocations of the retry. To identify the state, a field among all the fields in the input of the service must be annotated with @StatefulRetryDiscriminant: 

<code>class Person (val name: String, @StatefulRetryDiscriminant val passportID: String)</code>

Timestamp pour StatefulRetryDiscriminant tant que le service n’a pas réussit le client garde l’id

If after the configured attempts the service still fails, this permanent failure must be taken into account by Orcha with:

<code>when “controlIdentity fails” …</code>

Implementation: 

https://github.com/orchaland/tests/blob/master/statefulRetry/src/main/java/com/example/statefulRetry/StatefulRetryApplication.java

https://github.com/orchaland/tests/blob/master/statefulRetryConsumer/src/main/java/com/example/statefulRetryConsumer/StatefulRetryConsumerApplication.java

https://github.com/orchaland/tests/blob/master/idempotence/src/main/java/com/example/idempotence/IdempotenceApplication.java


Often, you must combine stateful retry with idempotence since a stateful retry occurs when a smart contract is launched again: stateful retry for services that can fail and idempotence to never called twice the other services when the retry occurs. 

## Transactions

Spring Integration : https://docs.spring.io/spring-integration/docs/5.2.3.RELEASE/reference/html/index-single.html#transactions

Transaction with a relational database implementation: https://github.com/orchaland/tests/blob/master/transactionJPA/src/main/java/com/example/integrationdsl/transactionJPAApplication.java

Transaction with a message broker implementation: 

https://github.com/orchaland/tests/blob/master/transactionJMS/src/main/java/com/example/transactionJMS/transactionJMSApplication.java

https://github.com/orchaland/tests/blob/master/transactionJMSConsumer/src/main/java/com/example/transactionJMSConsumer/transactionJMSConsumerApplication.java
## Message store mongoDB

Database directory c:\data\db

Lauch mongo (from bin folder): mongod

Check database content (from bin folder): mongo

show dbs

use <db>

show collections

db.collection.find()