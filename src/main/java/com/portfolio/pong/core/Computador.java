package com.portfolio.pong.core;

/**
 * Inteligência artificial do Pong: controla a raquete oponente de forma
 * "batedível" — persegue uma posição prevista, mas com velocidade limitada,
 * reação atrasada e uma zona morta que evita tremer quando já alinhado.
 *
 * <p>A dificuldade controla três parâmetros: velocidade máxima de
 * deslocamento, tempo de reação e margem da zona morta.</p>
 *
 * @author Eduardo Alvez
 */
public class Computador {

    /** Nível de dificuldade da IA. */
    public enum Dificuldade {
        /** Lento, distraído e impreciso. */
        FACIL(200.0, 0.35, 30, "Fácil"),
        /** Meio termo equilibrado. */
        MEDIO(320.0, 0.25, 18, "Médio"),
        /** Rápido, atento e quase perfeito. */
        DIFICIL(430.0, 0.15, 8, "Difícil");

        private final double velocidadeMaxima;
        private final double reacaoSegundos;
        private final int zonaMorta;
        private final String rotulo;

        Dificuldade(double velocidadeMaxima, double reacaoSegundos, int zonaMorta, String rotulo) {
            this.velocidadeMaxima = velocidadeMaxima;
            this.reacaoSegundos = reacaoSegundos;
            this.zonaMorta = zonaMorta;
            this.rotulo = rotulo;
        }

        public double getVelocidadeMaxima() {
            return velocidadeMaxima;
        }

        public double getReacaoSegundos() {
            return reacaoSegundos;
        }

        public int getZonaMorta() {
            return zonaMorta;
        }

        public String getRotulo() {
            return rotulo;
        }
    }

    private final Raquete raquete;
    private final Dificuldade dificuldade;
    private double alvoY;
    private double tempoAteProximaReacao;

    /**
     * @param raquete    raquete que a IA comanda
     * @param dificuldade nível da IA
     */
    public Computador(Raquete raquete, Dificuldade dificuldade) {
        this.raquete = raquete;
        this.dificuldade = dificuldade;
        this.alvoY = raquete.getCentroY();
        this.tempoAteProximaReacao = dificuldade.reacaoSegundos;
    }

    /**
     * Atualiza a IA: recomputa o alvo quando a "reação" acaba e move a
     * raquete em direção ao alvo (respeitando velocidade máxima e zona morta).
     *
     * @param bola bola atual do jogo
     * @param dt   tempo decorrido em segundos
     */
    public void atualizar(Bola bola, double dt) {
        tempoAteProximaReacao -= dt;
        if (tempoAteProximaReacao <= 0) {
            alvoY = preverAlvo(bola);
            tempoAteProximaReacao = dificuldade.reacaoSegundos;
        }

        double centroRaquete = raquete.getCentroY();
        double dif = alvoY - raquete.getCentroY();

        // Zona morta: já está perto o suficiente, não mexe (evita tremor).
        if (Math.abs(centroRaquete - alvoY) <= dificuldade.zonaMorta) {
            return;
        }

        int direcao = dif > 0 ? 1 : -1;
        double deslocamento = Math.min(Math.abs(dif), dificuldade.velocidadeMaxima * dt);
        raquete.setY((int) Math.round(raquete.getY() + direcao * deslocamento));
    }

    /** @return a raquete controlada pela IA */
    public Raquete getRaquete() {
        return raquete;
    }

    /** @return nível de dificuldade da IA */
    public Dificuldade getDificuldade() {
        return dificuldade;
    }

    /** Prevê a altura onde a bola atravessará a linha da raquete. */
    private double preverAlvo(Bola bola) {
        double campoLimite = raquete.getAlturaCampo() - Raquete.ALTURA;

        if (bola.getVx() > 0) {
            // Bola se aproximando (IA é a raquete direita).
            double distancia = raquete.getX() - bola.getX();
            double tempo = distancia / bola.getVx();
            double previsao = bola.getY() + bola.getVy() * tempo;
            return Math.max(0, Math.min(campoLimite, previsao));
        }
        return campoLimite / 2.0;
    }
}