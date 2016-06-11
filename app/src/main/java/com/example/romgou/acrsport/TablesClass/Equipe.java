package com.example.romgou.acrsport.TablesClass;

import java.util.UUID;

/**
 * Created by romgou on 26/04/2016.
 */
public class Equipe {
    private UUID id;
    private int numero;
    private  boolean abandon;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public boolean isAbandon() { return abandon; }

    public void setAbandon(boolean abandon) { this.abandon = abandon; }
}
