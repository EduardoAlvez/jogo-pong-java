package com.portfolio.pong.core;

/**
 * Prêmio que surge no campo para ser coletado pela bola (estilo itens do
 * Mario). Permanece no campo por {@link #TEMPO_EXPIRA} segundos e some quando
 * expira.
 *
 * <p>Núcleo puro (sem Swing): as cores são expostas como inteiros RGB e a
 * interface as converte em {@link java.awt.Color}.</p>
 *
 * @author Eduardo Alvez
 */
public class Premio {

    /** Raio do prêmio (círculo que a bola precisa encostar), em pixels. */
    public static final double RAIO = 16.0;

    /** Tempo em que o prêmio permanece no campo antes de desaparecer (s). */
    public static final double TEMPO_EXPIRA = 9.0;

    /** Tipos de prêmio disponíveis no campo. */
    public enum Tipo {
        /** Inverte os controles do adversário por 5s. */
        INVERTE("↕", 0x9B59B6),
        /** Trava a raquete do adversário por 3s. */
        CONGELA("❄", 0x4FC3F7),
        /** Acelera a raquete do coletor em +40% por 5s. */
        TURBO("⚡", 0xF1C40F),
        /** Deixa a bola em chamas por 3s sem gastar o especial. */
        CHAMAS("🔥", 0xFF7F27),
        /** Acrescenta 10s ao cronômetro (modo tempo). */
        TEMPO("⏱", 0x2ECC71),
        /** Encolhe a raquete do adversário por 5s. */
        ENCOLHE("▮", 0xE74C3C),
        /** Adiciona uma segunda bola até o próximo gol. */
        DUPLO("🔵", 0x3498DB),
        /** Sorteia um novo prêmio na hora da coleta. */
        CORINGA("?", 0xF39C12);

        private final String rotulo;
        private final int corRgb;

        Tipo(String rotulo, int corRgb) {
            this.rotulo = rotulo;
            this.corRgb = corRgb;
        }

        public String getRotulo() {
            return rotulo;
        }

        public int getCorRgb() {
            return corRgb;
        }
    }

    private final Tipo tipo;
    private final double x;
    private final double y;
    private double tempoRestante;
    private double tempoDeVida;

    public Premio(Tipo tipo, double x, double y) {
        this.tipo = tipo;
        this.x = x;
        this.y = y;
        this.tempoRestante = TEMPO_EXPIRA;
    }

    /** Decrementa o tempo de vida e conta o tempo já vivido (entrada/saída). */
    public void atualizar(double dt) {
        tempoRestante = Math.max(0.0, tempoRestante - dt);
        tempoDeVida += dt;
    }

    /** @return {@code true} quando o tempo de expiração se esgotou */
    public boolean acabou() {
        return tempoRestante <= 0.0;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /** @return segundos restantes antes do prêmio sumir */
    public double getTempoRestante() {
        return tempoRestante;
    }

    /** @return segundos desde que o prêmio surgiu (para a animação de entrada) */
    public double getTempoDeVida() {
        return tempoDeVida;
    }
}