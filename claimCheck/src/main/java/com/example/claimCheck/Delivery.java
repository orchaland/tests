package com.example.claimCheck;

public class Delivery {

    int orderId;
    String address;
    boolean done = false;

    public Delivery(int orderId, String address) {
        this.orderId = orderId;
        this.address = address;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    @Override
    public String toString() {
        return "Delivery{" +
                "orderId=" + orderId +
                ", address='" + address + '\'' +
                ", done=" + done +
                '}';
    }
}
