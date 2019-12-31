package com.example.integrationdsl;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.List;

@Component
class ConditionalService{

    @Autowired
    transactionJPAApplication.School school;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    public StudentDomain administrativeEnrollment(StudentDomain student) throws Exception {
        System.out.print("administrativeEnrollment receives: " + student);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.joinTransaction();
        StudentDomain studentDomain = new StudentDomain("Ben", 25);
        entityManager.persist(studentDomain);		// attention même si merge est utilisé, si nouvelle tx => nouvelle insertion !
        /*if(student.getAge() < 18){
            System.out.println(" and throws an exception.");
            throw new Exception();
        }*/
        System.out.println(" and returns: " + student);
        return student;
    }

    public StudentDomain courseEnrollment(StudentDomain student) throws Exception {
        System.out.print("courseEnrollment receives: " + student);
        student.setFirstName(student.getFirstName() + "-fileTransaction");
        if(student.getAge() > 30){
            System.out.println(" and throws an exception.");
            throw new Exception();
        }
        System.out.println(" and returns: " + student);
        return student;
    }

    /*public Object endOfAggregator(org.springframework.messaging.Message message){
        System.out.println("******************** endOfAggregator messages: " + message);
        return message;
    }*/

    public List<?> readDatabase(){
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
        List<?> results = jdbcTemplate.queryForList("Select * from Student");		// Morgane
        return results;
    }

    @Transactional
    public void manyStudentsInSeparatedTransactions(){
        StudentDomain morgane = new StudentDomain("A", 20);		// no exception => A is commited in the database
        school.add(morgane);
        System.out.println("\nmanyStudentsInSeparatedTransactions first student added\n");
        StudentDomain loic = new StudentDomain("B", 30);			// exception but new transaction => B is rolled back in the database
        school.add(loic);
    }

    @Transactional
    public int manyStudentsInTheSameTransaction(){
        StudentDomain morgane = new StudentDomain("C", 20);		// no exception => added to the transaction
        school.add(morgane);
        System.out.println("\nmanyStudentsInTheSameTransaction first student added\n");
        StudentDomain loic = new StudentDomain("D", 50);			// exception in the same transaction => C and D are rollback in the database
        school.add(loic);
        System.out.println("\nmanyStudentsInTheSameTransaction terminates");
        return 0;
    }

    public Object a(Object object){
        System.out.println("aaaaaaaaaaaaaa "+ object);
        return object;
    }

    public Object l1(Object object, @Header(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER) String SEQUENCE_NUMBER, @Header(IntegrationMessageHeaderAccessor.CORRELATION_ID) String CORRELATION_ID){
        System.out.println("llllllllllllll1111111111 "+ object + ", SEQUENCE_NUMBER: " + SEQUENCE_NUMBER + ", CORRELATION_ID: " + CORRELATION_ID);
        return object;
    }

    public Object list(Object object, @Header("sendChannel") String sendChannel, @Header(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER) String SEQUENCE_NUMBER, @Header(IntegrationMessageHeaderAccessor.CORRELATION_ID) String CORRELATION_ID){
        System.out.println("oooooooooooooooooooooooo "+ object + ", sendChannel: " + sendChannel + ", SEQUENCE_NUMBER: " + SEQUENCE_NUMBER + ", CORRELATION_ID: " + CORRELATION_ID);
        return object;
    }

    /*public Object list(Object object, @Header(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER) String SEQUENCE_NUMBER, @Header(IntegrationMessageHeaderAccessor.CORRELATION_ID) String CORRELATION_ID){
        System.out.println("oooooooooooooooooooooooo "+ object + ", SEQUENCE_NUMBER: " + SEQUENCE_NUMBER + ", CORRELATION_ID: " + CORRELATION_ID);
        return object;
    }*/

}