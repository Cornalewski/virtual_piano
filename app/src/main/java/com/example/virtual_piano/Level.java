package com.example.virtual_piano;

public class Level {
    public String partituraPath;
    public int numero;
    public boolean desbloqueado;
    public int estrelas;

    public Level(int numero, String partituraPath, boolean desbloqueado, int estrelas) {
        this.numero = numero;
        this.partituraPath = partituraPath;
        this.desbloqueado = desbloqueado;
        this.estrelas = estrelas;
    }

    public int getNumero() {
        return numero;
    }

    public String getPartituraPath() {
        return partituraPath;
    }

    public boolean isDesbloqueado() {
        return desbloqueado;
    }

    public void setDesbloqueado(boolean desbloqueado) {
        this.desbloqueado = desbloqueado;
    }

    public int getEstrelas() {
        return estrelas;
    }

    public void setEstrelas(int estrelas) {
        this.estrelas = estrelas;
    }
}
