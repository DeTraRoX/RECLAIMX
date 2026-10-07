package com.reclaimx.models;

public class Claim {
    private int id;
    private int itemId;
    private int claimantId;
    private String message;
    private String proof;
    private String status; // check status either pending , approved or rejected the claim of item 
    private String createdAt;
    private String reviewedAt;
    private Integer reviewedBy;

    // add item and user details in the temmplate for better efficiency
    private Item item;
    private User claimant;
    private User reviewer;

    public Claim() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getClaimantId() { return claimantId; }
    public void setClaimantId(int claimantId) { this.claimantId = claimantId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getProof() { return proof; }
    public void setProof(String proof) { this.proof = proof; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(String reviewedAt) { this.reviewedAt = reviewedAt; }

    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public User getClaimant() { return claimant; }
    public void setClaimant(User claimant) { this.claimant = claimant; }

    public User getReviewer() { return reviewer; }
    public void setReviewer(User reviewer) { this.reviewer = reviewer; }
}
