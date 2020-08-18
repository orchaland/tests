package com.example.mongodbReadWrite;

import com.mongodb.client.MongoClients;
import orcha.lang.compiler.referenceimpl.springIntegration.ApplicationToMessage;
import orcha.lang.compiler.referenceimpl.springIntegration.MessageToApplication;
import orcha.lang.configuration.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;
import org.springframework.integration.mongodb.dsl.MongoDb;
import org.springframework.integration.mongodb.dsl.MongoDbOutboundGatewaySpec;
import org.springframework.integration.mongodb.outbound.MongoDbStoringMessageHandler;
import org.springframework.messaging.MessageHandler;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * mongod --config /usr/local/etc/mongod.conf
 * mongo
 * use test
 * show collections
 * db.student.find()
 * mongo admin --eval "db.shutdownServer()"
 *
 *
 */
@SpringBootApplication
public class MongodbReadWriteApplication {

    @Bean
    MongoDatabaseFactory mongoDbFactory(){
        return new SimpleMongoClientDatabaseFactory(MongoClients.create(), "test");
    }

    @Autowired
    private MongoDatabaseFactory mongoDbFactory;

    /*@Autowired
    private MongoConverter mongoConverter;

    @Bean
    public IntegrationFlow gatewaySingleQueryFlow() {
        return f -> f
                .handle(MongoDb.outboundGateway(mongoDbFactory(), this.mongoConverter)
                        .query("{firstName : 'Lineda'}")
                        .collectionNameExpression("'student'")
                        //.collectionNameFunction(m -> (String)m.getHeaders().get("student"))
                        .expectSingleResult(true)
                        .entityClass(StudentDomain.class))
                .enrichHeaders(h -> h.headerExpression("messageID", "headers['id'].toString()"))
                .channel("enrollStudentChannel.input")
                .log();
    }

    @Bean(name = "enrollStudent")
    EnrollStudent enrollStudent() {
        return new EnrollStudent();
    }

    @Bean
    MessageToApplication enrollStudentMessageToApplication() {
        return new MessageToApplication(Application.State.TERMINATED, "enrollStudent");
    }

    @Bean
    ApplicationToMessage applicationToMessage() {
        return new ApplicationToMessage();
    }

    @Bean
    public IntegrationFlow enrollStudentChannel() {
        return f -> f.handle("enrollStudent", "enroll").handle(enrollStudentMessageToApplication(), "transform").channel("aggregateEnrollStudentChannel.input");
    }

    @Bean
    public IntegrationFlow aggregateEnrollStudentChannel() {
        return f -> f.aggregate(a -> a.releaseExpression("size()==1 AND (((getMessages().toArray())[0].payload instanceof Transpiler(orcha.lang.App) AND (getMessages().toArray())[0].payload.state==Transpiler(orcha.lang.configuration.State).TERMINATED))").correlationExpression("headers['messageID']")).transform("payload.?[name=='enrollStudent']").handle(applicationToMessage(), "transform").channel("studentOutputDatabaseChannel.input");
    }*/

    @MessagingGateway
    public interface School {
        @Gateway(requestChannel = "studentOutputDatabaseChannel.input")
        void placeOrder(StudentDomain student);
    }

    @Bean
    public IntegrationFlow studentOutputDatabaseChannel() {
        return f -> f
                .handle(mongoOutboundAdapter(mongoDbFactory()));
    }

    @Bean
    @Autowired
    public MessageHandler mongoOutboundAdapter(MongoDatabaseFactory mongo) {
        MongoDbStoringMessageHandler mongoHandler = new MongoDbStoringMessageHandler(mongo);
        mongoHandler.setCollectionNameExpression(new LiteralExpression("student"));
        return mongoHandler;
    }

    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication.run(MongodbReadWriteApplication.class, args);

        School school = context.getBean(School.class);
        StudentDomain studentDomain = new StudentDomain("Lineda", 40, -1);
        school.placeOrder(studentDomain);

        /*PopulateDatabase populateDatabase = (PopulateDatabase) context.getBean("populateDatabase");
        //List<?> results = populateDatabase.readDatabase();
        Flux<StudentDomain> results;

        System.out.println("\nmanyStudentsInValideTransaction is starting\n");
        try {

            StudentDomain student = new StudentDomain("Morgane", 21, -1);
            populateDatabase.saveStudent(student);
            results = populateDatabase.readDatabase();
            System.out.println("database: " + results);

        } catch (Exception e) {
            System.out.println(">>>>>> Caught exception: " + e);
        }

        //results = populateDatabase.readDatabase();
        //System.out.println("database: " + results);
        //List<?> results = populateDatabase.readDatabase();*/


    }
}
