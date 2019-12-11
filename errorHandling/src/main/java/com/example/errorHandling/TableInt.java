package com.example.errorHandling;

import javax.persistence.*;

@Entity
public class TableInt {

    int i;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    long id;

    public TableInt(int i) {
        this.i = i;
    }

    public TableInt() {
    }


    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "TableInt{" +
                "i=" + i +
                ", id=" + id +
                '}';
    }
}
