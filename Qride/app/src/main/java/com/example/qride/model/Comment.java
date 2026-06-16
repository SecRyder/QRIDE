package com.example.qride.model;

public class Comment {
    private int id;
    private int user_id;
    private int post_id;
    private String content;
    private String name;    // Tên người comment (lấy từ JOIN users)
    private String avatar;  // Ảnh đại diện người comment (lấy từ JOIN users)

    // Getter và Setter
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}