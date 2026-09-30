package com.portfolio.pong.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Testes da IA {@link Computador}: parâmetros por dificuldade, perseguição
 * com velocidade limitada, zona morta (sem tremor) e respeito aos limites.
 */
public class ComputadorTest {

    private static final int LARGURA = 800;
    private static final int ALTURA = 500;

    private Computador computadorDo(Computador.Dificuldade d, com.portfolio.pong.core.Raquete r) {
        return new Computador(r, d);
    }

    private Raquete raqueteDireita() {
        return new Raquete(LARGURA - 30 - Raquete.LARGURA, ALTURA);
    }

    @Test
    public void dificuldadeExpõeParametros() {
        assertEquals(200.0, Computador.Dificuldade.FACIL.getVelocidadeMaxima(), 1e-6);
        assertEquals(0.35, Computador.Dificuldade.FACIL.getReacaoSegundos(), 1e-6);
        assertEquals(30, Computador.Dificuldade.FACIL.getZonaMorta());
        assertEquals("Fácil", Computador.Dificuldade.FACIL.getRotulo());

        assertEquals(320.0, Computador.Dificuldade.MEDIO.getVelocidadeMaxima(), 1e-6);
        assertEquals("Médio", Computador.Dificuldade.MEDIO.getRotulo());

        assertEquals(430.0, Computador.Dificuldade.DIFICIL.getVelocidadeMaxima(), 1e-6);
        assertEquals("Difícil", Computador.Dificuldade.DIFICIL.getRotulo());
    }

    @Test
    public void criadaComRaqueteExposta() {
        Raquete r = raqueteDireita();
        Computador c = computadorDo(Computador.Dificuldade.MEDIO, r);
        assertEquals(r, c.getRaquete());
        assertEquals(Computador.Dificuldade.MEDIO, c.getDificuldade());
    }

    @Test
    public void seMoveEmDirecaoAoAlvo() {
        Raquete r = raqueteDireita();
        r.setY(0);
        // DIFICIL tem zona morta de 8px, permitindo chegar praticamente ao centro.
        Computador c = computadorDo(Computador.Dificuldade.DIFICIL, r);

        // Alvo no centro: a bola vem em direção à raquete em linha reta.
        Bola b = new Bola(LARGURA, ALTURA);
        b.centralizar(r.getX() - 200, ALTURA / 2.0, 1);

        for (int i = 0; i < 30; i++) {
            c.atualizar(b, 0.3);
        }
        double centro = r.getCentroY();
        assertTrue("raquete deve se aproximar do centro", Math.abs(centro - ALTURA / 2.0) < 10);
    }

    @Test
    public void naoUltrapassaOsLimites() {
        Raquete r = raqueteDireita();
        r.setY(0);
        Computador c = computadorDo(Computador.Dificuldade.MEDIO, r);
        Bola b = new Bola(LARGURA, ALTURA);
        for (int i = 0; i < 200; i++) {
            c.atualizar(b, 0.2);
        }
        assertTrue(r.getY() >= 0);
        assertTrue(r.getY() <= ALTURA - Raquete.ALTURA);
    }

    @Test
    public void zonaMortaEvitaTremor() {
        Raquete r = raqueteDireita();
        Computador c = computadorDo(Computador.Dificuldade.FACIL, r);
        // A bola cruza exatamente na altura da raquete (alvo = centro atual):
        // a zona morta (30px) deve impedir oscilação.
        Bola b = new Bola(LARGURA, ALTURA);
        b.centralizar(r.getX() - 100, r.getCentroY(), 1);
        // Primeiro passa o atraso de reação (0.35s no Fácil), senão o alvo
        // ainda é o antigo e a raquete se move para lá.
        c.atualizar(b, 0.35);
        int y = r.getY();
        c.atualizar(b, 0.1);
        assertEquals("raquete deve ficar parada dentro da zona morta", y, r.getY());
    }

    @Test
    public void velocidadeDeDeslocamentoRespeitaODt() {
        Raquete r = raqueteDireita();
        r.setY(0);
        Computador c = computadorDo(Computador.Dificuldade.FACIL, r);
        Bola b = new Bola(LARGURA, ALTURA);
        b.centralizar(r.getX() - 100, ALTURA, 1);
        // Com FACIL (200 px/s), uma reação de 0.5s desloca no máximo 100px,
        // muito menos que os ~410px de distância até o alvo.
        c.atualizar(b, 0.5);
        int deslocado = Math.abs(r.getY() - 0);
        assertTrue("deslocamento limitado pela velocidade", deslocado >= 50 && deslocado <= 101);
    }
}