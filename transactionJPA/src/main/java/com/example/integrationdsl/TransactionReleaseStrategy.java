package com.example.integrationdsl;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class TransactionReleaseStrategy implements ReleaseStrategy {

    @Autowired
    DirectChannel outputRetainingAggregatorChannel;

    boolean canRelease = true;
    int transactionDepth = 0;

    /**
     * Before all transactional methods
     * @param joinPoint
     */
    @Before("execution(public * *(..)) && @annotation(org.springframework.transaction.annotation.Transactional)")
    public void beginTransaction(JoinPoint joinPoint){
        canRelease = false;
        transactionDepth++;
        System.out.println("TransactionReleaseStrategy beginTransaction for " + joinPoint.getSignature().getName() + " => transactionDepth: " + transactionDepth + ", canRelease: " + canRelease);
    }

    /**
     * After all transactional methods send an "end of transaction" message to release the pending message inside the aggregrator
     * @param joinPoint
     */
    @AfterReturning("execution(public * *(..)) && @annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionCommited(JoinPoint joinPoint){
        // close the barrier to leave the last message alone ????
        transactionDepth--;
        if(transactionDepth == 0){                          // end of nested transactions
            canRelease = true;                              // enable aggregation of all pending messages (included inside the transaction)
        }

        new Thread(() -> {
            this.sendEndOfTransactionMessage("commited transaction");                  // new thread because the aggregator already uses the current thread
        }).start();

        System.out.println("TransactionReleaseStrategy commitTransaction for " + joinPoint.getSignature().getName() + " => transactionDepth: " + transactionDepth + ", canRelease: " + canRelease);
    }

    /**
     * Sends a message to the aggregator => thus it will release the messages
     */
    public void sendEndOfTransactionMessage(String releaseMessage){
        Message<String> message = MessageBuilder       // send a last message with to release the aggregator
                .withPayload(releaseMessage)
                .build();
        System.out.println("############# sendEndOfTransactionMessage : " + message);
        outputRetainingAggregatorChannel.send(message);           // to the aggregator
    }

    @AfterThrowing("execution(public * *(..)) && @annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionRolledBack(JoinPoint joinPoint){
    }

    @Override
    public boolean canRelease(MessageGroup group) {
        System.out.println("TransactionReleaseStrategy => canRelease = " + canRelease + " " + group);
        return canRelease;
    }
}
