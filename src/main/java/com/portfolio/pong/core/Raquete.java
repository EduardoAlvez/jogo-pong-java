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

    /** Altura mínima da raquete (prêmio "encolher"), em pixels. */
    public static final int ALTURA_MINIMA = 60;

    private final int x;
    private int y;
    private int altura = ALTURA;
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
        y = Math.min(alturaCampo - altura, y + passos);
        return y;
    }

    /** Define a posição vertical, limitada aos limites do campo. */
    public int setY(int novoY) {
        y = Math.max(0, Math.min(alturaCampo - altura, novoY));
        return y;
    }

    /** Centraliza a raquete verticalmente (usado nos resets). */
    public void centralizar() {
        y = (alturaCampo - altura) / 2;
    }

    /** @return posição da borda esquerda da raquete */
    public int getX() {
        return x;
    }

    /** @return posição da borda superior da raquete */
    public int getY() {
        return y;
    }

    /** @return altura atual da raquete (pode estar encolhida) */
    public int getAltura() {
        return altura;
    }

    /**
     * Define a altura da raquete, preservando o centro vertical (usado pelo
     * prêmio "encolher"). O valor é limitado entre {@link #ALTURA_MINIMA} e
     * {@link #ALTURA}.
     *
     * @param novaAltura nova altura em pixels
     */
    public void setAltura(int novaAltura) {
        double centro = getCentroY();
        altura = Math.max(ALTURA_MINIMA, Math.min(ALTURA, novaAltura));
        y = (int) Math.round(centro - altura / 2.0);
        setY(y);
    }

    /** @return centro vertical da raquete */
    public double getCentroY() {
        return y + altura / 2.0;
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