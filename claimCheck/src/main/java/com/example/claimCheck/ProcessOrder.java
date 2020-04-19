package com.example.claimCheck;

public class ProcessOrder {
    public Delivery prepare(Order order){
        System.out.print("prepare order: " + order);
        Delivery delivery = new Delivery(order.getId(), "Av des Champs Elysées, Paris");
        System.out.println(" and return: " + delivery);
        return delivery;
    }
    public Delivery deliver(Delivery delivery){
        System.out.print("deliver delivery: " + delivery);
        delivery.setDone(true);
        System.out.println(" and return: " + delivery);
        return delivery;
    }
    public Bill charge(Delivery delivery, Order order){
        System.out.print("charge delivery: " + delivery + " for order:" + order);
        Bill bill = new Bill(order, 1000);
        System.out.println(" and return: " + bill);
        return bill;
    }
}
