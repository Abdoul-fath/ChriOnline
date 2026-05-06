package model;

import java.util.Date;

public class AdminChallenge {
    private int id;
    private int userId;
    private String challengeValue;
    private Date expiresAt;
    private boolean usedFlag;
    private Date createdAt;
    
    public AdminChallenge() {}
    
    public AdminChallenge(int userId, String challengeValue, Date expiresAt) {
        this.userId = userId;
        this.challengeValue = challengeValue;
        this.expiresAt = expiresAt;
        this.usedFlag = false;
        this.createdAt = new Date();
    }
    
    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getChallengeValue() { return challengeValue; }
    public void setChallengeValue(String challengeValue) { this.challengeValue = challengeValue; }
    
    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
    
    public boolean isUsedFlag() { return usedFlag; }
    public void setUsedFlag(boolean usedFlag) { this.usedFlag = usedFlag; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}