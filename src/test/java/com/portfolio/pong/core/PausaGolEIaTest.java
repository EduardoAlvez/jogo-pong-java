package com.portfolio.pong.core;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Testes da pausa de 1s após o gol e da escolha de bola da CPU quando há mais
 * de uma em campo (prêmio bola extra).
 */
public class PausaGolEIaTest {

    private static final double LARGURA = 800;
    private static final double ALTURA = 500;
    private static final double DT = 1.0 / 60.0;

    private Pong pong;

    @Before
    public void novoClassico2P() {
        pong = new Pong(LARGURA, ALTURA);
        pong.configurar(Pong.Modo.CLASSICO, Computador.Dificuldade.MEDIO, true, 99, 60);
        pong.iniciarPartida();
    }

    private void avancarAteSacar() {
        for (int i = 0; i < 200 && pong.isAguardandoSaque(); i++) {
            pong.atualizar(DT);
        }
    }

    /** Em 2P sem input a bola faz ping-pong eterno: colocamos na borda para sair. */
    private void forcarGol() {
        avancarAteSacar();
        pong.getRaqueteDireita().setY(0);
        pong.getBola().centralizar(790, ALTURA / 2.0, 1);
        int alvo = pong.getPontosEsquerda() + 1;
        for (int i = 0; i < 100 && pong.getPontosEsquerda() < alvo; i++) {
            pong.atualizar(DT);
        }
    }

    private void adicionarBolaExtra() throws Exception {
        Method m = Pong.class.getDeclaredMethod("adicionarBolaExtra");
        m.setAccessible(true);
        m.invoke(pong);
    }

    private Bola bolaUrgenteDaCpu() throws Exception {
        Method m = Pong.class.getDeclaredMethod("bolaMaisUrgenteParaCpu");
        m.setAccessible(true);
        return (Bola) m.invoke(pong);
    }

    // ------------------------------------------------------------- Pausa

    @Test
    public void pausaComecaNoGol() {
        forcarGol();
        assertEquals(1, pong.getPontosEsquerda());
        assertTrue("entra em pausa logo após o gol", pong.isAguardandoSaque());
    }

    @Test
    public void pausaDuraUmSegundo() {
        forcarGol();
        int quadros = 0;
        while (pong.isAguardandoSaque() && quadros < 200) {
            pong.atualizar(DT);
            quadros++;
        }
        assertEquals(1.0, Pong.PAUSA_APOS_GOL, 1e-9);
        assertTrue("a pausa tem que durar ~1s (60 quadros), veio " + quadros,
                quadros == 60 || quadros == 61);
    }

    @Test
    public void bolaFicaParadaDuranteAPausa() {
        forcarGol();
        double x = pong.getBola().getX();
        while (pong.isAguardandoSaque()) {
            double antes = pong.getBola().getX();
            pong.atualizar(DT);
            if (pong.isAguardandoSaque()) {
                assertEquals("bola não pode se mover na pausa", antes, pong.getBola().getX(), 1e-9);
            }
        }
        assertTrue("no fim da pausa o saque já recentralizou",
                Math.abs(pong.getBola().getX() - LARGURA / 2.0) < 1.0);
    }

    @Test
    public void saqueVoltaAVelocidadeBase() {
        forcarGol();
        avancarAteSacar();
        assertEquals(Bola.VELOCIDADE_BASE, pong.getBola().getVelocidade(), 1e-9);
        assertEquals(ALTURA / 2.0, pong.getBola().getY(), 1.0);
    }

    @Test
    public void entradaDoJogadorIgnoraAPausa() {
        forcarGol();
        int y = pong.getRaqueteEsquerda().getY();
        for (int i = 0; i < 30; i++) {
            pong.moverJogador(1, 1, DT);
        }
        assertEquals("raquete não se move durante a pausa", y, pong.getRaqueteEsquerda().getY());
        assertFalse("especial bloqueado na pausa", pong.usarEspecial(1));
    }

    @Test
    public void reiniciarZeraAPausa() {
        forcarGol();
        assertTrue(pong.isAguardandoSaque());
        pong.iniciarPartida();
        assertFalse(pong.isAguardandoSaque());
    }

    @Test
    public void golDeOuroNaoAbrePausa() {
        pong.configurar(Pong.Modo.TEMPO, Computador.Dificuldade.MEDIO, true, 7, 60);
        pong.iniciarPartida();
        forcarGol();
        // O cronômetro zerado no empate vira gol de ouro, que encerra na hora.
        for (int i = 0; i < 200 && !pong.isEncerrado(); i++) {
            pong.getCronometro().atualizar(1);
            pong.atualizar(DT);
        }
        assertTrue(pong.isEncerrado());
        assertFalse(pong.isAguardandoSaque());
    }

    // ------------------------------------------------------- IA com 2 bolas

    @Test
    public void cpuSegueABolaPrincipalComUmaSo() throws Exception {
        Bola b = pong.getBola();
        b.centralizar(400, 250, 1);
        assertSame(b, bolaUrgenteDaCpu());
    }

    @Test
    public void cpuIgnoraBolaIndoEmbora() throws Exception {
        adicionarBolaExtra();
        Bola principal = pong.getBolas().get(0);
        Bola extra = pong.getBolas().get(1);
        principal.centralizar(300, 100, -1);
        extra.centralizar(600, 400, 1);
        assertSame("bola indo embora não é ameaça", extra, bolaUrgenteDaCpu());
    }

    @Test
    public void cpuEscolheOBolaMaisProximaDaRaquete() throws Exception {
        adicionarBolaExtra();
        Bola principal = pong.getBolas().get(0);
        Bola extra = pong.getBolas().get(1);
        principal.centralizar(420, 100, 1);
        extra.centralizar(700, 400, 1);
        assertSame(extra, bolaUrgenteDaCpu());
        // Inverte as posições: a escolha tem que mudar.
        principal.centralizar(700, 100, 1);
        extra.centralizar(420, 400, 1);
        assertSame(principal, bolaUrgenteDaCpu());
    }

    @Test
    public void semAmeacaCpuVoltaABolaPrincipal() throws Exception {
        adicionarBolaExtra();
        Bola principal = pong.getBolas().get(0);
        Bola extra = pong.getBolas().get(1);
        principal.centralizar(300, 100, -1);
        extra.centralizar(200, 400, -1);
        assertSame(principal, bolaUrgenteDaCpu());
    }

    /**
     * O bug original: a CPU só via a bola principal, então com duas bolas em
     * alturas opostas ela deixava a bola iminente passar.
     */
    @Test
    public void cpuDevolveABolaIminenteComDuasBolas() throws Exception {
        Pong jogo = new Pong(LARGURA, ALTURA);
        jogo.configurar(Pong.Modo.CLASSICO, Computador.Dificuldade.MEDIO, false, 99, 60);
        jogo.iniciarPartida();
        Method add = Pong.class.getDeclaredMethod("adicionarBolaExtra");
        add.setAccessible(true);
        add.invoke(jogo);
        for (int i = 0; i < 200 && jogo.isAguardandoSaque(); i++) {
            jogo.atualizar(DT);
        }
        Bola longe = jogo.getBolas().get(0);
        Bola iminente = jogo.getBolas().get(1);
        longe.centralizar(380, 100, 1);
        iminente.centralizar(500, 400, 1);

        boolean rebateu = false;
        for (int f = 0; f < 120; f++) {
            jogo.atualizar(DT);
            if (iminente.getVx() < 0) {
                rebateu = true;
                break;
            }
            if (jogo.getPontosEsquerda() > 0) {
                break;
            }
        }
        assertTrue("a CPU precisa defender a bola que chega primeiro", rebateu);
    }

    /** A IA antiga (só a bola principal) perde a bola iminente — controle do bug. */
    @Test
    public void iaDeUmaBolaSoPerdeABolaIminente() throws Exception {
        Pong jogo = new Pong(LARGURA, ALTURA);
        jogo.configurar(Pong.Modo.CLASSICO, Computador.Dificuldade.MEDIO, false, 99, 60);
        jogo.iniciarPartida();
        Method add = Pong.class.getDeclaredMethod("adicionarBolaExtra");
        add.setAccessible(true);
        add.invoke(jogo);
        Field campo = Pong.class.getDeclaredField("computador");
        campo.setAccessible(true);
        Computador interno = (Computador) campo.get(jogo);
        campo.set(jogo, new Computador(jogo.getRaqueteDireita(), Computador.Dificuldade.MEDIO) {
            @Override
            public void atualizar(Bola b, double dt) {
                interno.atualizar(jogo.getBolas().get(0), dt);
            }
        });
        for (int i = 0; i < 200 && jogo.isAguardandoSaque(); i++) {
            jogo.atualizar(DT);
        }
        Bola longe = jogo.getBolas().get(0);
        Bola iminente = jogo.getBolas().get(1);
        longe.centralizar(380, 100, 1);
        iminente.centralizar(500, 400, 1);

        boolean rebateu = false;
        for (int f = 0; f < 120; f++) {
            jogo.atualizar(DT);
            if (iminente.getVx() < 0) {
                rebateu = true;
                break;
            }
            if (jogo.getPontosEsquerda() > 0) {
                break;
            }
        }
        assertFalse("este teste só faz sentido se a IA de 1 bola falhar aqui", rebateu);
        assertNotNull(jogo.getRaqueteDireita());
    }
}