package com.portfolio.pong.fx;

import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testes do motor de efeitos {@link Animacoes}: partículas com vida finita,
 * marcas de queimado, explosão/tremor e limpeza.
 */
public class AnimacoesTest {

    private static final double DT = 1.0 / 60.0;

    @Test
    public void emitirFogoCriaParticulaEMarcaDeQueimado() {
        Animacoes a = new Animacoes();
        a.emitirFogo(100, 100);
        assertEquals(1, a.getParticulas().size());
        assertEquals(1, a.getMarcas().size());
    }

    @Test
    public void fogoAcumulaMarcasPersistentes() {
        Animacoes a = new Animacoes();
        for (int i = 0; i < 10; i++) {
            a.emitirFogo(100, 100);
        }
        assertEquals(10, a.getMarcas().size());
        // Marcas não somem com o tempo.
        for (int i = 0; i < 120; i++) {
            a.atualizar(DT);
        }
        assertEquals(10, a.getMarcas().size());
    }

    @Test
    public void particulaEmiteComVidaMasExpira() {
        Particula p = new Particula(0, 0, 0, 0, Color.RED, 3, 0.5);
        assertTrue(p.estaViva());
        for (int i = 0; i < (int) (0.5 / DT) + 5; i++) {
            p.atualizar(DT);
        }
        assertFalse(p.estaViva());
    }

    @Test
    public void particulaExpiraEDesapareceDoMotor() {
        Animacoes a = new Animacoes();
        a.emitirFogo(100, 100);
        for (int i = 0; i < (int) (0.5 / DT) + 20; i++) {
            a.atualizar(DT);
        }
        assertTrue(a.getParticulas().isEmpty());
    }

    @Test
    public void alfaComegaCheioEVaiAZero() {
        Particula p = new Particula(0, 0, 0, 0, Color.WHITE, 3, 1.0);
        assertEquals(1.0, p.getAlpha(), 1e-6);
        p.atualizar(0.5);
        assertEquals(0.5, p.getAlpha(), 1e-6);
    }

    @Test
    public void explosaoDeGolGeraParticulasETremor() {
        Animacoes a = new Animacoes();
        a.explosaoGol(300, 250);
        assertTrue("explosão deve gerar muitas partículas",
                a.getParticulas().size() >= 20);
        assertTrue(a.getTremor() > 0);
    }

    @Test
    public void tremorDecaiAteZero() {
        Animacoes a = new Animacoes();
        a.adicionarTremor(12);
        for (int i = 0; i < 200; i++) {
            a.atualizar(DT);
        }
        assertEquals(0.0, a.getTremor(), 1e-6);
    }

    @Test
    public void confeteEncheATela() {
        Animacoes a = new Animacoes();
        a.confete(800, Color.RED, Color.GREEN, Color.BLUE);
        assertTrue(a.getParticulas().size() >= 50);
    }

    @Test
    public void limparZeraTudo() {
        Animacoes a = new Animacoes();
        a.emitirFogo(100, 100);
        a.explosaoGol(300, 250);
        a.adicionarTremor(10);
        assertTrue(!a.getParticulas().isEmpty());
        a.limpar();
        assertTrue(a.getParticulas().isEmpty());
        assertTrue(a.getMarcas().isEmpty());
        assertEquals(0.0, a.getTremor(), 1e-6);
    }

    @Test
    public void limiteDeParticulasRespeitado() {
        Animacoes a = new Animacoes();
        for (int i = 0; i < Animacoes.LIMITE_PARTICULAS + 50; i++) {
            a.emitirFogo(i % 800, 100);
        }
        assertTrue(a.getParticulas().size() <= Animacoes.LIMITE_PARTICULAS);
    }
}