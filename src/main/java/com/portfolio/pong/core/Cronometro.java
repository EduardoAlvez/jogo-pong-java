package com.portfolio.pong.core;

/**
 * Cronômetro do modo tempo: conta a duração da partida em segundos e expõe o
 * fator de "urgência" (0 → 1) usado para acelerar a bola conforme o tempo
 * esgota, no estilo futebol.
 *
 * @author Eduardo Alvez
 */
public class Cronometro {

    /** Duração curta (1 minuto). */
    public static final int DURACAO_1MIN = 60;

    /** Duração média (2 minutos). */
    public static final int DURACAO_2MIN = 120;

    /** Duração longa (3 minutos). */
    public static final int DURACAO_3MIN = 180;

    private final int duracaoSegundos;
    private double restante;

    /**
     * @param duracaoSegundos duração total (ex.: {@link #DURACAO_1MIN})
     */
    public Cronometro(int duracaoSegundos) {
        this.duracaoSegundos = duracaoSegundos;
        this.restante = duracaoSegundos;
    }

    /** Zera o cronômetro para o início da rodada. */
    public void reiniciar() {
        restante = duracaoSegundos;
    }

    /** Decrementa o tempo (não passa de zero). */
    public void atualizar(double dt) {
        if (restante > 0) {
            restante = Math.max(0.0, restante - dt);
        }
    }

    /** @return {@code true} quando o tempo acabou */
    public boolean acabou() {
        return restante <= 0;
    }

    /** @return segundos ainda restantes */
    public double getRestanteSegundos() {
        return restante;
    }

    /** @return duração total configurada */
    public int getDuracaoSegundos() {
        return duracaoSegundos;
    }

    /**
     * Fator de urgência no intervalo [0, 1]: 0 no início da partida e 1
     * quando o tempo zera. Usado para acelerar a bola.
     */
    public double fatorDeUrgencia() {
        return 1.0 - restante / duracaoSegundos;
    }

    /**
     * Formata um valor em segundos no formato "MM:SS" (arredonda para cima).
     *
     * @param segundos tempo em segundos
     * @return texto formatado (ex.: {@code "01:59"})
     */
    public static String formatar(double segundos) {
        int total = (int) Math.ceil(segundos);
        return String.format("%02d:%02d", total / 60, total % 60);
    }
}