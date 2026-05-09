package com.example.qride.thanhtoan;

public class Transaction {
    public int id;
    public String type;
    public long amount;
    public String description;
    public String created_at;
    public Integer rental_id;

    public Transaction(int id, String type, long amount, String description, String created_at, Integer rental_id) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.created_at = created_at;
        this.rental_id = rental_id;
    }
}
