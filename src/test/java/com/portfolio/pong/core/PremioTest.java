package com.portfolio.pong.core;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testes dos prêmios do campo ({@link Premio}) e dos efeitos que eles aplicam
 * em {@link Pong}: levas de 1 a 3, validade de 9s, sorteio por modo, coleta
 * pela bola e a duração de cada um dos 8 efeitos.
 */
public class PremioTest {

    private static final double LARGURA = 800;
    private static final double ALTURA = 500;
    private static final double DT = 1.0 / 60.0;

    private Pong pong;

    @Before
    public void novoClassico2P() {
        pong = new Pong(LARGURA, ALTURA);
        pong.configurar(Pong.Modo.CLASSICO, Computador.Dificuldade.MEDIO, true, 5, 120);
        pong.iniciarPartida();
    }

    // ------------------------------------------------------------- Helpers

    private void aplicarPremio(Premio.Tipo tipo, int coletor) throws Exception {
        Method m = Pong.class.getDeclaredMethod("aplicarPremio", Premio.Tipo.class, int.class);
        m.setAccessible(true);
        m.invoke(pong, tipo, coletor);
    }

    /** Consome a pausa pós-gol para os quadros de teste não sumirem nela. */
    private void avancarPausaSeHouver() {
        for (int i = 0; i < 200 && pong.isAguardandoSaque(); i++) {
            pong.atualizar(DT);
        }
    }

    private void criarLeva() throws Exception {
        Method m = Pong.class.getDeclaredMethod("criarLeva");
        m.setAccessible(true);
        m.invoke(pong);
    }

    // ------------------------------------------------------------ Constantes

    @Test
    public void premioExpiraEm9Segundos() {
        Premio p = new Premio(Premio.Tipo.TURBO, 400, 250);
        assertEquals(9.0, Premio.TEMPO_EXPIRA, 1e-9);
        assertEquals(9.0, p.getTempoRestante(), 1e-9);
        for (int i = 0; i < 540; i++) {
            p.atualizar(DT);
        }
        assertTrue("o prêmio deve sumir antes de 9s", p.acabou());
    }

    @Test
    public void premioGuardaTipoEPosicao() {
        Premio p = new Premio(Premio.Tipo.CONGELA, 321, 123);
        assertEquals(Premio.Tipo.CONGELA, p.getTipo());
        assertEquals(321.0, p.getX(), 1e-9);
        assertEquals(123.0, p.getY(), 1e-9);
        assertTrue(Premio.Tipo.CONGELA.getRotulo().length() > 0);
        assertTrue(Premio.Tipo.CONGELA.getCorRgb() != 0);
    }

    @Test
    public void asOitoDuracoesSaoAsCombinadas() {
        assertEquals(10.0, Pong.DURACAO_INVERTE, 1e-9);
        assertEquals(4.0, Pong.DURACAO_CONGELA, 1e-9);
        assertEquals(10.0, Pong.DURACAO_TURBO, 1e-9);
        assertEquals(6.0, Pong.DURACAO_CHAMAS, 1e-9);
        assertEquals(10.0, Pong.DURACAO_ENCOLHE, 1e-9);
        assertEquals(5.0, Pong.DURACAO_ESPECIAL, 1e-9);
        assertEquals(10, Pong.BONUS_TEMPO);
    }

    // ------------------------------------------------------------ Sorteio

    @Test
    public void sorteioTem8Tipos() {
        assertEquals(8, Premio.Tipo.values().length);
    }

    @Test
    public void inverteNaoSorteiaEm1P() {
        pong.configurar(Pong.Modo.CLASSICO, Computador.Dificuldade.MEDIO, false, 5, 120);
        List<Premio.Tipo> pool = pong.tiposDePremio();
        assertFalse("contra a CPU o Inverter some", pool.contains(Premio.Tipo.INVERTE));
        assertTrue(pool.contains(Premio.Tipo.CONGELA));
    }

    @Test
    public void inverteSorteiaEm2P() {
        List<Premio.Tipo> pool = pong.tiposDePremio();
        assertTrue("em 2P o Inverter aparece", pool.contains(Premio.Tipo.INVERTE));
    }

    @Test
    public void tempoNaoSorteiaNoClassico() {
        List<Premio.Tipo> pool = pong.tiposDePremio();
        assertFalse("no clássico não entra o +10s", pool.contains(Premio.Tipo.TEMPO));
    }

    @Test
    public void tempoSorteiaNoModoTempo() {
        pong.configurar(Pong.Modo.TEMPO, Computador.Dificuldade.MEDIO, true, 5, 60);
        assertTrue(pong.tiposDePremio().contains(Premio.Tipo.TEMPO));
    }

    // ------------------------------------------------------------- Levas

    @Test
    public void primeiraLevaSurgeAos8Segundos() {
        for (int i = 0; i < 8.0 / DT - 2; i++) {
            pong.atualizar(DT);
        }
        assertTrue("antes de 8s não deveria ter prêmio",
                pong.getPremios().isEmpty());
        for (int i = 0; i < 30; i++) {
            pong.atualizar(DT);
        }
        assertFalse("aos 8s tem que ter prêmio", pong.getPremios().isEmpty());
    }

    @Test
    public void levaTrazDe1A3Premios() throws Exception {
        boolean viuUm = false;
        boolean viuMais = false;
        for (int tentativa = 0; tentativa < 200; tentativa++) {
            pong.getPremios().clear();
            criarLeva();
            int n = pong.getPremios().size();
            assertTrue("leva não pode vir vazia", n >= 1);
            assertTrue("leva não passa de " + Pong.MAX_PREMIO_POR_LEVA,
                    n <= Pong.MAX_PREMIO_POR_LEVA);
            if (n == 1) {
                viuUm = true;
            } else {
                viuMais = true;
            }
        }
        assertTrue("tem que existir leva de 1", viuUm);
        assertTrue("tem que existir leva com 2 ou 3", viuMais);
    }

    @Test
    public void premiosDaLevaNaoSeSobrepoem() throws Exception {
        for (int tentativa = 0; tentativa < 60; tentativa++) {
            pong.getPremios().clear();
            criarLeva();
            List<Premio> ps = pong.getPremios();
            for (int i = 0; i < ps.size(); i++) {
                for (int j = i + 1; j < ps.size(); j++) {
                    double d = Math.hypot(ps.get(i).getX() - ps.get(j).getX(),
                            ps.get(i).getY() - ps.get(j).getY());
                    assertTrue("prêmios sobrepostos (dist " + d + ")", d >= 2 * Premio.RAIO);
                }
            }
        }
    }

    @Test
    public void premioSomeDepoisDe9Segundos() throws Exception {
        criarLeva();
        assertFalse(pong.getPremios().isEmpty());
        for (int i = 0; i < 10.0 / DT; i++) {
            pong.atualizar(DT);
        }
        assertTrue("prêmio deveria ter expirado", pong.getPremios().isEmpty());
    }

    @Test
    public void naoSurgePremioNoGolDeOuro() {
        pong.configurar(Pong.Modo.TEMPO, Computador.Dificuldade.MEDIO, true, 5, 60);
        pong.iniciarPartida();
        pong.getCronometro().atualizar(600);
        pong.getRaqueteDireita().setY(0);
        pong.getBola().centralizar(LARGURA / 2.0, ALTURA / 2.0, 1);
        for (int i = 0; i < 400 && !pong.isEncerrado(); i++) {
            pong.atualizar(DT);
            assertTrue("não pode surgir prêmio no gol de ouro", pong.getPremios().isEmpty());
        }
    }

    @Test
    public void bolaColetaOPrêmio() throws Exception {
        criarLeva();
        Premio alvo = pong.getPremios().get(0);
        // A bola parte de cima do prêmio, caindo em cima dele.
        pong.getBola().centralizar(alvo.getX(), alvo.getY() - Premio.RAIO - 4, 1);
        int antes = pong.getPremios(1) + pong.getPremios(2);
        for (int i = 0; i < 30 && pong.getPremios(1) + pong.getPremios(2) == antes; i++) {
            pong.atualizar(DT);
        }
        assertEquals("a coleta tem que contar", antes + 1,
                pong.getPremios(1) + pong.getPremios(2));
        assertTrue("o prêmio coletado some do campo",
                pong.getPremios().stream().noneMatch(p -> p == alvo));
    }

    // ------------------------------------------------------------ Efeitos

    @Test
    public void inverteTrocaOsControlesDoAdversario() throws Exception {
        aplicarPremio(Premio.Tipo.INVERTE, 1);
        assertEquals(Pong.DURACAO_INVERTE, pong.getTempoInvertido(2), 1e-9);
        assertEquals(0.0, pong.getTempoInvertido(1), 1e-9);
        for (int i = 0; i < Pong.DURACAO_INVERTE / DT; i++) {
            avancarPausaSeHouver();
            pong.atualizar(DT);
        }
        assertEquals(0.0, pong.getTempoInvertido(2), 1e-9);
    }

    @Test
    public void congelaTravaARaqueteAdversaria() throws Exception {
        aplicarPremio(Premio.Tipo.CONGELA, 1);
        assertEquals(Pong.DURACAO_CONGELA, pong.getTempoCongelado(2), 1e-9);
        int y = pong.getRaqueteDireita().getY();
        pong.moverJogador(2, 1, DT);
        assertEquals("raquete congelada não move", y, pong.getRaqueteDireita().getY());
    }

    @Test
    public void turboDeixaARaqueteMaisRapida() throws Exception {
        aplicarPremio(Premio.Tipo.TURBO, 1);
        assertEquals(Pong.DURACAO_TURBO, pong.getTempoTurbo(1), 1e-9);
        int antes = pong.getRaqueteEsquerda().getY();
        pong.moverJogador(1, 1, DT);
        int comTurbo = Math.abs(pong.getRaqueteEsquerda().getY() - antes);

        for (int i = 0; i < Pong.DURACAO_TURBO / DT + 10; i++) {
            avancarPausaSeHouver();
            pong.atualizar(DT);
        }
        assertEquals(0.0, pong.getTempoTurbo(1), 1e-9);
        int antes2 = pong.getRaqueteEsquerda().getY();
        pong.moverJogador(1, 1, DT);
        int semTurbo = Math.abs(pong.getRaqueteEsquerda().getY() - antes2);
        assertTrue("turbo tem que ser mais rápido (" + comTurbo + " vs " + semTurbo + ")",
                comTurbo > semTurbo);
    }

    @Test
    public void chamasNaoGastamABateriaDoEspecial() throws Exception {
        aplicarPremio(Premio.Tipo.CHAMAS, 1);
        assertEquals(Pong.DURACAO_CHAMAS, pong.getTempoChamas(1), 1e-9);
        assertEquals(0, pong.getCargaEspecial(1));
        assertFalse(pong.isEspecial1Ativo());
        assertTrue(pong.isBolaEmChamas());
    }

    @Test
    public void encolheReduzARaqueteAdversaria() throws Exception {
        aplicarPremio(Premio.Tipo.ENCOLHE, 1);
        assertEquals(Pong.DURACAO_ENCOLHE, pong.getTempoEncolhido(2), 1e-9);
        assertEquals(60, pong.getRaqueteDireita().getAltura());
        assertEquals(Raquete.ALTURA, pong.getRaqueteEsquerda().getAltura());
        for (int i = 0; i < Pong.DURACAO_ENCOLHE / DT + 10; i++) {
            avancarPausaSeHouver();
            pong.atualizar(DT);
        }
        assertEquals(Raquete.ALTURA, pong.getRaqueteDireita().getAltura());
    }

    @Test
    public void bonusDeTempoSoNoModoTempo() throws Exception {
        pong.configurar(Pong.Modo.TEMPO, Computador.Dificuldade.MEDIO, true, 5, 60);
        // Gasta 10s para abrir espaço no cronômetro (que vai até 60s).
        pong.getCronometro().atualizar(10);
        double antes = pong.getCronometro().getRestanteSegundos();
        assertEquals(50.0, antes, 0.5);
        aplicarPremio(Premio.Tipo.TEMPO, 1);
        assertEquals(antes + Pong.BONUS_TEMPO, pong.getCronometro().getRestanteSegundos(), 0.5);
    }

    @Test
    public void bonusDeTempoParaNoTetoDaPartida() throws Exception {
        pong.configurar(Pong.Modo.TEMPO, Computador.Dificuldade.MEDIO, true, 5, 60);
        for (int i = 0; i < 10; i++) {
            aplicarPremio(Premio.Tipo.TEMPO, 1);
        }
        assertEquals("não passa da duração da partida",
                60.0, pong.getCronometro().getRestanteSegundos(), 0.5);
    }

    @Test
    public void bolaExtraFicaAteOGol() throws Exception {
        assertEquals(1, pong.getBolas().size());
        aplicarPremio(Premio.Tipo.DUPLO, 1);
        assertEquals(2, pong.getBolas().size());
        aplicarPremio(Premio.Tipo.DUPLO, 1);
        assertEquals("respeita o limite de 2", 2, pong.getBolas().size());

        // Gol removes as extras.
        pong.getRaqueteDireita().setY(0);
        pong.getBola().centralizar(LARGURA / 2.0, ALTURA / 2.0, 1);
        int alvo = pong.getPontosEsquerda() + 1;
        for (int i = 0; i < 500 && pong.getPontosEsquerda() < alvo; i++) {
            pong.atualizar(DT);
        }
        for (int i = 0; i < 200 && pong.isAguardandoSaque(); i++) {
            pong.atualizar(DT);
        }
        assertEquals(1, pong.getBolas().size());
    }

    @Test
    public void coringaNuncaRepeteOCoringa() throws Exception {
        for (int i = 0; i < 30; i++) {
            aplicarPremio(Premio.Tipo.CORINGA, 1);
            assertTrue("o coringa tem que aplicar um efeito real",
                    pong.getTempoInvertido(1) > 0 || pong.getTempoInvertido(2) > 0
                            || pong.getTempoCongelado(1) > 0 || pong.getTempoCongelado(2) > 0
                            || pong.getTempoTurbo(1) > 0 || pong.getTempoTurbo(2) > 0
                            || pong.getTempoChamas(1) > 0 || pong.getTempoChamas(2) > 0
                            || pong.getTempoEncolhido(1) > 0 || pong.getTempoEncolhido(2) > 0
                            || pong.getBolas().size() == 2);
            pong.iniciarPartida();
        }
    }

    @Test
    public void reiniciarZeraOsEfeitos() throws Exception {
        aplicarPremio(Premio.Tipo.INVERTE, 1);
        aplicarPremio(Premio.Tipo.CONGELA, 1);
        aplicarPremio(Premio.Tipo.TURBO, 1);
        aplicarPremio(Premio.Tipo.ENCOLHE, 1);
        pong.iniciarPartida();
        assertEquals(0.0, pong.getTempoInvertido(1), 1e-9);
        assertEquals(0.0, pong.getTempoCongelado(1), 1e-9);
        assertEquals(0.0, pong.getTempoTurbo(1), 1e-9);
        assertEquals(0.0, pong.getTempoEncolhido(1), 1e-9);
        assertEquals(Raquete.ALTURA, pong.getRaqueteDireita().getAltura());
        assertTrue(pong.getPremios().isEmpty());
        assertFalse(pong.isAguardandoSaque());
    }

    @Test
    public void premioTemRaioDeColisaoPositivo() {
        assertTrue(Premio.RAIO > 0);
    }
}