package com.portfolio.pong.core;

/**
 * Raquete do Pong: desliza verticalmente dentro dos limites do campo.
 * Núcleo puro (sem Swing) — a interface apenas lê posição e desenha.
 *
 * @author Eduardo Alvez
 */
public class Raquete {

    /** Largura da raquete, em pixels. */
    public static final int LARGURA = 14;

    /** Altura da raquete, em pixels. */
    public static final int ALTURA = 90;

    private final int x;
    private int y;
    private final int alturaCampo;

    /**
     * Cria uma raquete em uma lateral fixa, centralizada verticalmente.
     *
     * @param x posição da borda esquerda da raquete
     * @param alturaCampo altura do campo (limite inferior do movimento)
     */
    public Raquete(int x, int alturaCampo) {
        this.x = x;
        this.alturaCampo = alturaCampo;
        this.y = (alturaCampo - ALTURA) / 2;
    }

    /** Move a raquete para cima, respeitando o limite superior. */
    public int moverCima(int passos) {
        y = Math.max(0, y - passos);
        return y;
    }

    /** Move a raquete para baixo, respeitando o limite inferior do campo. */
    public int moverBaixo(int passos) {
        y = Math.min(alturaCampo - ALTURA, y + passos);
        return y;
    }

    /** Define a posição vertical, limitada aos limites do campo. */
    public int setY(int novoY) {
        y = Math.max(0, Math.min(alturaCampo - ALTURA, novoY));
        return y;
    }

    /** Centraliza a raquete verticalmente (usado nos resets). */
    public void centralizar() {
        y = (alturaCampo - ALTURA) / 2;
    }

    /** @return posição da borda esquerda da raquete */
    public int getX() {
        return x;
    }

    /** @return posição da borda superior da raquete */
    public int getY() {
        return y;
    }

    /** @return centro vertical da raquete */
    public double getCentroY() {
        return y + ALTURA / 2.0;
    }

    /** @return altura do campo onde a raquete se move */
    public int getAlturaCampo() {
        return alturaCampo;
    }

    /** Raquete esquerda? Verdadeiro quando fica na metade esquerda do campo. */
    public boolean isEsquerda(double larguraCampo) {
        return x < larguraCampo / 2.0;
    }
}