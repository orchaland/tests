package com.example.claimCheck;

public class Bill {

    Order order;
    float amount;

    public Bill(Order order, float amount) {
        this.order = order;
        this.amount = amount;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Bill{" +
                "order=" + order +
                ", amount=" + amount +
                '}';
    }
}
