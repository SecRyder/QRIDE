package com.example.qride.model;

public class NotificationModel {
    private int id;
    private String title;
    private String message;
    private String timestamp;
    private boolean isRead;
    private String type;

    public NotificationModel(int id, String title, String message, String timestamp, boolean isRead, String type) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.type = type;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public String getType() { return type; }
}
