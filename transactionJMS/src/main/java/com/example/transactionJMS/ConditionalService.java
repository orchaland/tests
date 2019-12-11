package com.example.transactionJMS;

public class ConditionalService {

    static int count = 0;

    public Object beforeSendingJMS(Object student) throws Exception {
        System.out.println("----------- beforeSendingJMS ----------" + student + " " + Thread.currentThread().getId());
			/*if(student != null){
				throw new Exception();
			}*/
        return student;
    }

    public Object jmsErrorFlow(Object student) throws Exception {
        System.out.println("----------- jmsErrorFlow attempt " + count + "----------" + student + " " + Thread.currentThread().getId());
        count++;
        if(student != null){
            //throw new Exception();
        }
        return student;
    }

    public Object jmsReplyChannelFlow(Object student) throws Exception {
        System.out.println("----------- jmsReplyChannelFlow attempt " + count + "----------" + student + " " + Thread.currentThread().getId());
        count++;
        if(student != null){
            //throw new Exception();
        }
        return student;
    }

}