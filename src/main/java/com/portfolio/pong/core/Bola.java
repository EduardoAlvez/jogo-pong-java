package com.portfolio.pong.core;

/**
 * Bola do Pong: controle de posição, velocidade, reflexões e aceleração.
 *
 * <p>Física clássica do arcade: o ângulo de saída da raquete depende do ponto
 * de contato (borda = ângulo íngreme, centro = reto), com um clamp para
 * nunca devolver um ângulo que trave o jogo. A velocidade sobe a cada
 * rebatida ({@value #ACELERACAO_POR_REBATIDA}), limitada por um teto.</p>
 *
 * <p>Núcleo puro (sem Swing): posição em coordenadas contínuas.</p>
 *
 * @author Eduardo Alvez
 */
public class Bola {

    /** Raio da bola, em pixels. */
    public static final double RAIO = 8.0;

    /** Velocidade inicial/base, em pixels por segundo. */
    public static final double VELOCIDADE_BASE = 240.0;

    /** Teto de velocidade, em pixels por segundo. */
    public static final double VELOCIDADE_MAXIMA = 800.0;

    /** Fator de aceleração a cada rebatida na raquete (+6%). */
    public static final double ACELERACAO_POR_REBATIDA = 1.06;

    /** Ângulo máximo de reflexão em relação à horizontal (±60°). */
    public static final double ANGULO_MAXIMO = Math.PI / 3.0;

    private final double larguraCampo;
    private final double alturaCampo;
    private final double raio;

    private double x;
    private double y;
    private double vx;
    private double vy;

    /**
     * @param larguraCampo largura do campo (gol nas laterais)
     * @param alturaCampo  altura do campo (paredes no topo e na base)
     */
    public Bola(double larguraCampo, double alturaCampo) {
        this.larguraCampo = larguraCampo;
        this.alturaCampo = alturaCampo;
        this.raio = RAIO;
    }

    /**
     * Centraliza a bola e saca na horizontal, na direção informada.
     *
     * @param x        posição inicial (geralmente o centro do campo)
     * @param y        posição inicial (geralmente o centro do campo)
     * @param direcao  {@code +1} para a direita, {@code -1} para a esquerda
     */
    public void centralizar(double x, double y, int direcao) {
        this.x = x;
        this.y = y;
        redefinirVelocidade(direcao);
    }

    /** Reinicia na velocidade base (sem aceleração acumulada de rebatidas). */
    public void redefinirVelocidade(int direcao) {
        double vel = VELOCIDADE_BASE;
        vx = direcao * vel;
        vy = 0;
    }

    /** Translada a posição da bola conforme a velocidade. */
    public void mover(double dt) {
        x += vx * dt;
        y += vy * dt;
    }

    /**
     * Aplica a reflexão nas paredes superior e inferior.
     *
     * @return {@code true} se bateu em alguma parede (para efeitos sonoros)
     */
    public boolean rebaterParedes() {
        boolean bateu = false;
        if (y - raio <= 0) {
            y = raio;
            vy = Math.abs(vy);
            bateu = true;
        } else if (y + raio >= alturaCampo) {
            y = alturaCampo - raio;
            vy = -Math.abs(vy);
            bateu = true;
        }
        return bateu;
    }

    /**
     * Troca a reflexão na raquete, caso a bola colida com ela vindo na
     * direção certa. O ângulo de saída é definido pelo ponto de contato e a
     * velocidade acelera em {@value #ACELERACAO_POR_REBATIDA}, com teto.
     *
     * @param raquete raquete a ser verificada
     * @return {@code true} se houve rebatida (para contagem de trocas)
     */
    public boolean rebaterNaRaquete(Raquete raquete) {
        boolean raqueteEsquerda = raquete.isEsquerda(larguraCampo);
        boolean vindoNaDirecaoCerta = (raqueteEsquerda && vx < 0) || (!raqueteEsquerda && vx > 0);
        if (!vindoNaDirecaoCerta || !colideComRaquete(raquete)) {
            return false;
        }

        double contacto = (y - raquete.getY()) / Raquete.ALTURA;
        contacto = Math.max(0.0, Math.min(1.0, contacto));
        double angulo = (contacto - 0.5) * 2.0 * ANGULO_MAXIMO;

        double velocidade = Math.min(VELOCIDADE_MAXIMA, Math.hypot(vx, vy) * ACELERACAO_POR_REBATIDA);
        int direcao = raqueteEsquerda ? 1 : -1;
        vx = direcao * velocidade * Math.cos(angulo);
        vy = velocidade * Math.sin(angulo);

        // Anti-sticking: empurra a bola para fora da raquete no rebote.
        if (raqueteEsquerda) {
            x = raquete.getX() + Raquete.LARGURA + raio;
        } else {
            x = raquete.getX() - raio;
        }
        return true;
    }

    /**
     * Ajusta a velocidade da bola preservando o ângulo atual. Usado pela
     * rampa de tempo (bola acelera conforme o tempo esgota).
     *
     * @param novaVelocidade nova velocidade (px/s)
     */
    public void ajustarVelocidade(double novaVelocidade) {
        double velocidade = Math.hypot(vx, vy);
        if (velocidade < 1e-6) {
            vx = novaVelocidade;
            vy = 0;
            return;
        }
        double fator = novaVelocidade / velocidade;
        vx *= fator;
        vy *= fator;
    }

    /** @return ponto mais à esquerda da bola */
    public double getX() {
        return x;
    }

    /** @return ponto mais alto da bola */
    public double getY() {
        return y;
    }

    /** @return velocidade componente horizontal */
    public double getVx() {
        return vx;
    }

    /** @return velocidade componente vertical */
    public double getVy() {
        return vy;
    }

    /** @return velocidade atual (módulo do vetor) */
    public double getVelocidade() {
        return Math.hypot(vx, vy);
    }

    /** @return raio da bola */
    public double getRaio() {
        return raio;
    }

    /** @return {@code true} se a bola cruzou completamente a lateral esquerda */
    public boolean saiuPelaEsquerda() {
        return x - raio <= 0;
    }

    /** @return {@code true} se a bola cruzou completamente a lateral direita */
    public boolean saiuPelaDireita() {
        return x + raio >= larguraCampo;
    }

    private boolean colideComRaquete(Raquete raquete) {
        double cx = Math.max(raquete.getX(),
                Math.min(x, raquete.getX() + Raquete.LARGURA));
        double cy = Math.max(raquete.getY(),
                Math.min(y, raquete.getY() + Raquete.ALTURA));
        double dx = x - cx;
        double dy = y - cy;
        return (dx * dx + dy * dy) <= raio * raio;
    }
}