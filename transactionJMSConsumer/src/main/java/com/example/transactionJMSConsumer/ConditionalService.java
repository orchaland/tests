package com.example.transactionJMSConsumer;

public class ConditionalService {

    static int count = 0;

    public String messageReceived(String student) throws Exception {
        System.out.println("----------- messageReceived attempt " + count + "----------" + student + " " + Thread.currentThread().getId());
        student = student + " received";
        count++;
        if(student != null){
            //throw new Exception();
        }
        return student;
    }

    public Object fileWriting1(Object student) throws Exception {
        System.out.println("----------- fileWriting1 attempt " + count + "----------" + student + " " + Thread.currentThread().getId());
        count++;
        if(student != null){
            //throw new Exception();
        }
        return student;
    }

    public Object fileWriting2(Object student) throws Exception {
        System.out.println("----------- fileWriting2 ----------" + student + " " + Thread.currentThread().getId());
			/*if(student.getGender() == Gender.FEMALE){
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

}