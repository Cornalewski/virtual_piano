package com.example.virtual_piano;

public class Nota {
    public String nome;
    public boolean ligada;
    public boolean visivel;
    public boolean colcheia;
    public long tempoInicio;
    public long duracao;
    public boolean tocando;


    public Nota(String nome, boolean ligada, boolean visivel, long tempoInicio, long duracao) {
        this.nome = nome;
        this.ligada = ligada;
        this.visivel = visivel;
        this.tempoInicio = tempoInicio;
        this.duracao = duracao;
        this.tocando = false;
    }

}
