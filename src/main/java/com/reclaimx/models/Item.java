package com.reclaimx.models;

public class Item {
    private int id;
    private int userId;
    private String title;
    private String description;
    private String category;
    private String color;
    private String brand;
    private String location;
    private String dateLostFound;
    private String image;
    private String type; // "lost" or "found"
    private String status; // "Lost", "Found", "Claimed", "Verified", "Returned", "Rejected", "Closed"
    private String createdAt;
    private String updatedAt;

    // Joined User Details for convenience in templates
    private String userName;
    private String userEmail;
    private String userPhone;

    public Item() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDateLostFound() { return dateLostFound; }
    public void setDateLostFound(String dateLostFound) { this.dateLostFound = dateLostFound; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
}
