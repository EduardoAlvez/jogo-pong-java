package com.portfolio.pong.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testes da {@link Raquete}: movimentos com limites do campo, centralização e
 * identificação da lateral.
 */
public class RaqueteTest {

    private static final int ALTURA = 500;

    private Raquete raqueteEm(int x) {
        return new Raquete(x, ALTURA);
    }

    @Test
    public void criaCentralizada() {
        Raquete r = raqueteEm(30);
        assertEquals((ALTURA - Raquete.ALTURA) / 2, r.getY());
    }

    @Test
    public void moverCimaRespeitaLimiteSuperior() {
        Raquete r = raqueteEm(30);
        r.setY(10);
        r.moverCima(30);
        assertEquals(0, r.getY());
    }

    @Test
    public void moverBaixoRespeitaLimiteInferior() {
        Raquete r = raqueteEm(30);
        r.setY(ALTURA - Raquete.ALTURA - 10);
        r.moverBaixo(50);
        assertEquals(ALTURA - Raquete.ALTURA, r.getY());
    }

    @Test
    public void moverCimaMovimentaNormalmente() {
        Raquete r = raqueteEm(30);
        int y = r.getY();
        r.moverCima(25);
        assertEquals(y - 25, r.getY());
    }

    @Test
    public void setYClampaParaDentro() {
        Raquete r = raqueteEm(30);
        r.setY(-5);
        assertEquals(0, r.getY());
        r.setY(10000);
        assertEquals(ALTURA - Raquete.ALTURA, r.getY());
    }

    @Test
    public void centralizarVoltaAoMeio() {
        Raquete r = raqueteEm(30);
        r.setY(10);
        r.centralizar();
        assertEquals((ALTURA - Raquete.ALTURA) / 2, r.getY());
    }

    @Test
    public void centroVerticalExposto() {
        Raquete r = raqueteEm(30);
        assertEquals(r.getY() + Raquete.ALTURA / 2.0, r.getCentroY(), 1e-6);
    }

    @Test
    public void esquerdaIdentificaLadoPelaMetade() {
        assertTrue(raqueteEm(30).isEsquerda(800));
        assertFalse(raqueteEm(700).isEsquerda(800));
    }

    @Test
    public void xFixo() {
        assertEquals(45, raqueteEm(45).getX());
    }

    @Test
    public void alturaCampoExposta() {
        assertEquals(ALTURA, raqueteEm(30).getAlturaCampo());
    }
}