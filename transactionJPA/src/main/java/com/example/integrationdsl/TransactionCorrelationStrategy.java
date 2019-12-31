package com.example.integrationdsl;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class TransactionCorrelationStrategy implements CorrelationStrategy {

    long groupId = 0;
    int transactionDepth = 0;
    int sequenceNumber = 2;

    /**
     * Before all transactional methods
     * @param joinPoint
     */
    @Before("execution(public * *(..)) && @annotation(org.springframework.transaction.annotation.Transactional)")
    public void beginTransaction(JoinPoint joinPoint){
        transactionDepth++;
        if(transactionDepth == 1){
            groupId++;                  // initiate a new group for all the next messages nested in the transaction
        }
        System.out.println("TransactionCorrelationStrategy beginTransaction for " + joinPoint.getSignature().getName() + " => transactionDepth: " + transactionDepth + ", groupId: " + groupId);
    }

    /**
     * After all transactional methods
     * @param joinPoint
     */
    @After("execution(public * *(..)) && @annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionCommited(JoinPoint joinPoint){
        transactionDepth--;
        System.out.println("TransactionCorrelationStrategy commitTransaction for " + joinPoint.getSignature().getName() + "  => transactionDepth: " + transactionDepth + ", groupId: " + groupId);
    }

    @Override
    public Object getCorrelationKey(Message<?> message) {

        sequenceNumber++;
        Integer seqNumber = new Integer(sequenceNumber);
        if(seqNumber == Integer.MAX_VALUE){
            sequenceNumber = 2;
        }

        boolean endOfTransaction = false;
        if (message.getPayload() instanceof String) {
            endOfTransaction = ((String)message.getPayload()).equals("commited transaction");
        }
        if(transactionDepth==0 && endOfTransaction==false){     // increase groupId if: no transaction (transactionDepth=0) because each message is in its own group
            groupId++;                                          // but don't increase if endOfTransaction (belongs to the same grouo)
        }
        System.out.println("TransactionCorrelationStrategy => groupId: " + groupId + " for message: " + message);
        Long grID = new Long(groupId);
        if(grID == Long.MAX_VALUE){
            groupId = 0L;
        }
        return new Long(groupId);
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public long getGroupId() {
        return groupId;
    }
}

