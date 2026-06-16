package com.example.qride.model;

import com.google.gson.annotations.SerializedName;

public class Post {

    public int id;
    public String name;
    public String avatar;
    public String title;
    public String content;
    public String location;

    @SerializedName("image_url")
    public String imageUrl;

    public int likes;
    public int comments;

    public Post() {}

    // Constructor
    public Post(int id, String name, String avatar, String title, String content, String location, String imageUrl, int likes, int comments) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.title = title;
        this.content = content;
        this.location = location;
        this.imageUrl = imageUrl;
        this.likes = likes;
        this.comments = comments;
    }

    // --- GETTERS ---
    public int getId() { return id; }
    public String getName() { return name; }
    public String getAvatar() { return avatar; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getLocation() { return location; }
    public String getImageUrl() { return imageUrl; }
    public int getLikes() { return likes; }
    public int getComments() { return comments; }

    // --- SETTERS (Thêm vào để tối ưu xử lý Like/Comment mượt mà trên UI) ---
    public void setLikes(int likes) { this.likes = likes; }
    public void setComments(int comments) { this.comments = comments; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setLocation(String location) { this.location = location; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}