package com.reclaimx.models;

public class Notification {
    private int id;
    private int userId;
    private String message;
    private String type; // 'claim_submitted', 'claim_approved', 'claim_rejected', 'returned', 'system'
    private Integer relatedItemId;
    private boolean isRead;
    private String createdAt;

    // Joined item details if needed
    private Item relatedItem;

    public Notification() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getRelatedItemId() { return relatedItemId; }
    public void setRelatedItemId(Integer relatedItemId) { this.relatedItemId = relatedItemId; }

    public boolean isIsRead() { return isRead; }
    public boolean getIsRead() { return isRead; }
    public void setIsRead(boolean isRead) { this.isRead = isRead; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Item getRelatedItem() { return relatedItem; }
    public void setRelatedItem(Item relatedItem) { this.relatedItem = relatedItem; }
}
