package com.example.romgou.acrsport.TablesClass;

import java.util.UUID;

/**
 * Created by romgou on 29/04/2016.
 */
public class PointChrono {
    private UUID id;
    private int ordre;
    private String nomPosition;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public String getNomPosition() {
        return nomPosition;
    }

    public void setNomPosition(String nomPosition) {
        this.nomPosition = nomPosition;
    }

    @Override
    public String toString() {
        return getOrdre() + " - " + getNomPosition();
    }
}
