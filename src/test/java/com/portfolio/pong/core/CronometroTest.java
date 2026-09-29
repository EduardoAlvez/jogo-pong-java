package com.portfolio.pong.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testes do {@link Cronometro}: contagem regressiva, formatação MM:SS e o
 * fator de urgência usado para acelerar a bola.
 */
public class CronometroTest {

    @Test
    public void iniciaComDuracaoCompleta() {
        Cronometro c = new Cronometro(Cronometro.DURACAO_2MIN);
        assertEquals(120, c.getDuracaoSegundos());
        assertEquals(120.0, c.getRestanteSegundos(), 1e-6);
        assertFalse(c.acabou());
    }

    @Test
    public void atualizarDebitaOTempo() {
        Cronometro c = new Cronometro(Cronometro.DURACAO_1MIN);
        c.atualizar(10.5);
        assertEquals(60 - 10.5, c.getRestanteSegundos(), 1e-6);
    }

    @Test
    public void tempoNaoPassaDeZero() {
        Cronometro c = new Cronometro(Cronometro.DURACAO_1MIN);
        c.atualizar(999);
        assertEquals(0.0, c.getRestanteSegundos(), 1e-6);
        assertTrue(c.acabou());
    }

    @Test
    public void reiniciarVoltaAoCompleto() {
        Cronometro c = new Cronometro(Cronometro.DURACAO_3MIN);
        c.atualizar(200);
        assertTrue(c.acabou());
        c.reiniciar();
        assertEquals(180.0, c.getRestanteSegundos(), 1e-6);
        assertFalse(c.acabou());
    }

    @Test
    public void fatorDeUrgenciaComecaEmZero() {
        Cronometro c = new Cronometro(120);
        assertEquals(0.0, c.fatorDeUrgencia(), 1e-6);
    }

    @Test
    public void fatorDeUrgenciaTerminaEmUm() {
        Cronometro c = new Cronometro(120);
        c.atualizar(120);
        assertEquals(1.0, c.fatorDeUrgencia(), 1e-6);
    }

    @Test
    public void fatorDeUrgenciaMeiaPartida() {
        Cronometro c = new Cronometro(120);
        c.atualizar(60);
        assertEquals(0.5, c.fatorDeUrgencia(), 1e-6);
    }

    @Test
    public void formatarSegundosEmMMSS() {
        assertEquals("02:05", Cronometro.formatar(125));
        assertEquals("02:00", Cronometro.formatar(119.1));
        assertEquals("00:00", Cronometro.formatar(0));
        assertEquals("03:00", Cronometro.formatar(180));
    }
}