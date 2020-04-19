package com.example.idempotence;

public class ProcessOrder {
    public Order confirm(Order order){
        System.out.println("confirm order: " + order);
        return order;
    }
    public Order closeOrder(Order order){
        System.out.println("close order: " + order);
        return order;
    }
    public Order discard(Order order){
        System.out.println("discard order: " + order);
        return order;
    }
}
