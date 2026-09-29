package com.portfolio.pong.fx;

import java.awt.Color;

/**
 * Uma partícula do motor de efeitos: ponto com posição, velocidade, cor,
 * tamanho e vida finita com fade (alfa cai junto com a vida restante).
 *
 * <p>Núcleo puro (sem Swing): a interface desenha usando
 * {@link #getAlpha()} para o fade.</p>
 *
 * @author Eduardo Alvez
 */
public class Particula {

    private double x;
    private double y;
    private final double vx;
    private final double vy;
    private final Color cor;
    private final double raio;
    private final double vidaTotal;
    private double vidaRestante;

    /**
     * @param x      posição inicial
     * @param y      posição inicial
     * @param vx     velocidade componente horizontal (px/s)
     * @param vy     velocidade componente vertical (px/s)
     * @param cor    cor da partícula
     * @param raio   tamanho em pixels
     * @param vida   duração em segundos
     */
    public Particula(double x, double y, double vx, double vy, Color cor, double raio, double vida) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.cor = cor;
        this.raio = raio;
        this.vidaTotal = Math.max(0.0, vida);
        this.vidaRestante = this.vidaTotal;
    }

    /** Move a partícula e consome a vida. */
    public void atualizar(double dt) {
        x += vx * dt;
        y += vy * dt;
        if (vidaRestante > 0) {
            vidaRestante = Math.max(0.0, vidaRestante - dt);
        }
    }

    /** @return {@code true} enquanto a partícula ainda está viva */
    public boolean estaViva() {
        return vidaRestante > 0;
    }

    /** @return alfa de fade no intervalo (0, 1] */
    public double getAlpha() {
        return vidaTotal <= 0 ? 0.0 : vidaRestante / vidaTotal;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Color getCor() {
        return cor;
    }

    public double getRaio() {
        return raio;
    }

    public double getVidaRestante() {
        return vidaRestante;
    }
}