package com.portfolio.pong.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testes da física da {@link Bola}: movimento, reflexões nas paredes e nas
 * raquetes (ângulo por ponto de contato), aceleração a cada rebatida e
 * detecção de passagem de lateral.
 */
public class BolaTest {

    private static final double LARGURA = 800;
    private static final double ALTURA = 500;

    private Bola bolaNova() {
        return new Bola(LARGURA, ALTURA);
    }

    @Test
    public void velocidadeInicialEhABase() {
        Bola b = bolaNova();
        b.centralizar(400, 250, 1);
        assertEquals(Bola.VELOCIDADE_BASE, b.getVelocidade(), 1e-6);
        assertTrue(b.getVx() > 0);
        assertEquals(0.0, b.getVy(), 1e-6);
    }

    @Test
    public void sacarParaEsquerdaInverteDirecao() {
        Bola b = bolaNova();
        b.centralizar(400, 250, -1);
        assertTrue(b.getVx() < 0);
    }

    @Test
    public void moverTransladaConformeVelocidade() {
        Bola b = bolaNova();
        b.centralizar(400, 250, 1);
        double x0 = b.getX();
        b.mover(0.1);
        assertEquals(x0 + Bola.VELOCIDADE_BASE * 0.1, b.getX(), 1e-6);
    }

    @Test
    public void rebaterParedeSuperiorEmpurraParaDentro() {
        Bola b = bolaNova();
        b.centralizar(400, b.getRaio(), 1);
        b.mover(0.001);
        assertTrue(b.rebaterParedes());
        assertEquals(b.getRaio(), b.getY(), 1e-6);
        assertTrue(b.getVy() >= 0);
    }

    @Test
    public void rebaterParedeInferiorEmpurraParaDentro() {
        Bola b = bolaNova();
        b.centralizar(400, ALTURA - b.getRaio(), -1);
        b.mover(0.001);
        assertTrue(b.rebaterParedes());
        assertEquals(ALTURA - b.getRaio(), b.getY(), 1e-6);
        assertTrue(b.getVy() <= 0);
    }

    @Test
    public void semParedeNaoBate() {
        Bola b = bolaNova();
        b.centralizar(400, 250, 1);
        assertFalse(b.rebaterParedes());
    }

    /** Posição do centro da bola sobreposto à raquete (x fora pelo direito),
     *  vinda do lado direito: dentro do raio de colisão, mas ainda colidindo. */
    private double posColisao(Raquete r, Bola b) {
        return r.getX() + Raquete.LARGURA + b.getRaio() - 0.5;
    }

    @Test
    public void rebaterNaRaqueteInverteDirecaoHorizontal() {
        Bola b = bolaNova();
        Raquete r = new Raquete(30, (int) ALTURA);
        b.centralizar(posColisao(r, b), 250, -1);
        boolean rebateu = b.rebaterNaRaquete(r);
        assertTrue(rebateu);
        assertTrue(b.getVx() > 0);
    }

    @Test
    public void rebaterNoCentroDaRaqueteSaiRaso() {
        Bola b = bolaNova();
        Raquete r = new Raquete(30, (int) ALTURA);
        b.centralizar(posColisao(r, b), r.getCentroY(), -1);
        b.rebaterNaRaquete(r);
        double angulo = Math.abs(Math.asin(b.getVy() / b.getVelocidade()));
        assertTrue("angulo deveria ser ~0, foi " + angulo, angulo < 0.1);
    }

    @Test
    public void rebaterNaBordaSaiComAnguloIngreme() {
        Bola b = bolaNova();
        Raquete r = new Raquete(30, (int) ALTURA);
        b.centralizar(posColisao(r, b), r.getY(), -1);
        b.rebaterNaRaquete(r);
        double angulo = Math.abs(Math.asin(b.getVy() / b.getVelocidade()));
        assertTrue("angulo deveria ser proximo do maximo, foi " + angulo,
                angulo >= Bola.ANGULO_MAXIMO * 0.8);
        assertEquals("nao deve ultrapassar o clamp de 60 graus",
                Bola.ANGULO_MAXIMO, angulo, 1e-6);
    }

    @Test
    public void rebatidaEmpurraBolaParaForaDaRaquete() {
        Bola b = bolaNova();
        Raquete r = new Raquete(30, (int) ALTURA);
        b.centralizar(posColisao(r, b), r.getCentroY(), -1);
        double antes = b.getX();
        b.rebaterNaRaquete(r);
        assertTrue("bola deve sair do corpo da raquete", b.getX() > antes);
    }

    @Test
    public void rebatidaAceleraASeguinte() {
        Bola b = bolaNova();
        Raquete r = new Raquete(30, (int) ALTURA);
        b.centralizar(posColisao(r, b), r.getCentroY(), -1);
        double velocidade = b.getVelocidade();
        b.rebaterNaRaquete(r);
        assertTrue("velocidade deve aumentar", b.getVelocidade() > velocidade);
        assertEquals(velocidade * Bola.ACELERACAO_POR_REBATIDA, b.getVelocidade(), 1e-6);
    }

    @Test
    public void aceleracaoNaoUltrapassaOTeto() {
        Bola b = bolaNova();
        Raquete r = new Raquete(30, (int) ALTURA);
        b.centralizar(30 + Raquete.LARGURA + b.getRaio(), r.getCentroY(), -1);
        // Ajusta para uma velocidade absurdamente alta; uma rebatida deve
        // aplicar o teto ({@code VELOCIDADE_MAXIMA}) graças ao clamp.
        b.ajustarVelocidade(Bola.VELOCIDADE_MAXIMA * 10);
        b.rebaterNaRaquete(r);
        assertEquals("rebatida deve respeitar o teto de velocidade",
                Bola.VELOCIDADE_MAXIMA, b.getVelocidade(), 1e-6);
    }

    @Test
    public void redefinirVelocidadeZeraAceleracaoAcumulada() {
        Bola b = bolaNova();
        b.centralizar(100, 100, 1);
        b.ajustarVelocidade(Bola.VELOCIDADE_MAXIMA);
        b.redefinirVelocidade(1);
        assertEquals(Bola.VELOCIDADE_BASE, b.getVelocidade(), 1e-6);
    }

    @Test
    public void ajustarVelocidadePreservaDirecao() {
        Bola b = bolaNova();
        b.centralizar(400, 250, 1);
        Bola b2 = new Bola(LARGURA, ALTURA);
        b2.centralizar(400, 250, 1);
        b.ajustarVelocidade(400);
        double angulo = Math.atan2(b.getVy(), b.getVx());
        double angulo2 = Math.atan2(b2.getVy(), b2.getVx());
        assertEquals(angulo2, angulo, 1e-6);
        assertEquals(400.0, b.getVelocidade(), 1e-6);
    }

    @Test
    public void bolaImediatamenteRasaAjustaParaFrente() {
        Bola b = bolaNova();
        b.centralizar(400, 250, -1);
        b.ajustarVelocidade(500);
        assertEquals(500.0, b.getVelocidade(), 1e-6);
    }

    @Test
    public void saiuPelaEsquerda() {
        Bola b = bolaNova();
        b.centralizar(-1 - b.getRaio(), 250, -1);
        assertTrue(b.saiuPelaEsquerda());
        assertFalse(b.saiuPelaDireita());
    }

    @Test
    public void saiuPelaDireita() {
        Bola b = bolaNova();
        b.centralizar(LARGURA + b.getRaio() + 1, 250, 1);
        assertTrue(b.saiuPelaDireita());
        assertFalse(b.saiuPelaEsquerda());
    }

    @Test
    public void bolaNaoColideSeVindoNaDirecaoOposta() {
        Bola b = bolaNova();
        Raquete r = new Raquete(30, (int) ALTURA);
        b.centralizar(posColisao(r, b), r.getCentroY(), 1);
        assertFalse(b.rebaterNaRaquete(r));
    }

    @Test
    public void getRaioExposto() {
        assertEquals(Bola.RAIO, bolaNova().getRaio(), 1e-6);
    }
}