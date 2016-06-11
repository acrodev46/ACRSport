package com.example.romgou.acrsport.TablesClass;

import java.util.Date;
import java.util.UUID;

/**
 * Created by romgou on 26/04/2016.
 */
public class SuiviTemps {
    private UUID id;
    private UUID equipeId;
    private Date datePassage;
    private UUID pointChronoId;
    private String userId;
    private String longitude;
    private String latitude;

    public SuiviTemps (SuiviTemps s){
        this.id = s.getId();
        this.equipeId = s.getEquipeId();
        this.datePassage = s.getDatePassage();
        this.pointChronoId = s.getPointChronoId();
        this.userId = s.getUserId();
        this.longitude = s.getLongitude();
        this.latitude = s.getLatitude();
    }

    public SuiviTemps() {
        this.id = null;
        this.equipeId = null;
        this.datePassage = null;
        this.pointChronoId = null;
        this.userId = null;
        this.longitude = null;
        this.latitude = null;
    }

    public String getLatitude() { return latitude; }

    public void setLatitude(String latitude) { this.latitude = latitude; }

    public String getLongitude() { return longitude; }

    public void setLongitude(String longitude) { this.longitude = longitude; }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getDatePassage() {
        return datePassage;
    }

    public void setDatePassage(Date datePassage) {
        this.datePassage = datePassage;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEquipeId() {
        return equipeId;
    }

    public void setEquipeId(UUID equipeId) {
        this.equipeId = equipeId;
    }

    public UUID getPointChronoId() { return pointChronoId; }

    public void setPointChronoId(UUID pointChronoId) { this.pointChronoId = pointChronoId; }
}
