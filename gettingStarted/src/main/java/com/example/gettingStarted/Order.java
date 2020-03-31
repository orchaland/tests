package com.example.gettingStarted;

import java.io.Serializable;

public class Order implements Serializable {

    String product;
    int id;

    public Order(String product, int id) {
        this.product = product;
        this.id = id;
    }

    public Order() {
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Order{" +
                "product='" + product + '\'' +
                ", id=" + id +
                '}';
    }
}
