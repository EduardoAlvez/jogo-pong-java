package com.portfolio.pong.core;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes de integração da partida ({@link Pong}): modos, gols/sacada alternada,
 * vitória por pontos e por tempo, gol de ouro, especial de bola em chamas,
 * rampa de tempo e estatísticas.
 */
public class PongTest {

    private static final double LARGURA = 800;
    private static final double ALTURA = 500;
    private static final double DT = 1.0 / 60.0;

    private Pong pong;

    @Before
    public void novoClassico2P() {
        pong = new Pong(LARGURA, ALTURA);
        pong.configurar(Pong.Modo.CLASSICO, Computador.Dificuldade.MEDIO, true, 5, 120);
    }

    /** Deixa a pausa pós-gol terminar, para o saque acontecer. */
    private void avancarAteSacar() {
        for (int i = 0; i < 200 && pong.isAguardandoSaque(); i++) {
            pong.atualizar(DT);
        }
    }

    /**
     * Reenvia a bola na direção da raquete do jogador até completar
     * {@code quadros}, garantindo rebatidas (cada uma vale +1 de bateria).
     */
    private void jogarRebatendo(int jogador, int quadros) {
        Raquete alvo = jogador == 1 ? pong.getRaqueteEsquerda() : pong.getRaqueteDireita();
        int dir = jogador == 1 ? -1 : 1;
        for (int i = 0; i < quadros; i++) {
            if (pong.isAguardandoSaque()) {
                avancarAteSacar();
            }
            if (Math.signum(pong.getBola().getVx()) != dir) {
                alvo.setY((int) (ALTURA / 2.0 - Raquete.ALTURA / 2.0));
                pong.getBola().centralizar(LARGURA / 2.0, ALTURA / 2.0, dir);
            }
            pong.atualizar(DT);
        }
    }

    /** Enche a bateria do especial do jogador (3 cargas). */
    private void carregarBateria(int jogador) {
        jogarRebatendo(jogador, 6000);
        assertEquals("a bateria deveria ter encherdo",
                Pong.CARGAS_PARA_ESPECIAL, pong.getCargaEspecial(jogador));
    }

    /** Bola disparada em linha reta para a direita, com a raquete direita
     *  afastada do caminho: sai pela lateral e o jogador da esquerda marca. */
    private void forcarGolDaEsquerda() {
        avancarAteSacar();
        pong.getRaqueteDireita().setY(0);
        pong.getBola().centralizar(LARGURA / 2.0, ALTURA / 2.0, 1);
        int alvo = pong.getPontosEsquerda() + 1;
        for (int i = 0; i < 500 && pong.getPontosEsquerda() < alvo; i++) {
            pong.atualizar(DT);
        }
        avancarAteSacar();
    }

    // ------------------------------------------------------------ Setup

    @Test
    public void configurarIniciaZerado() {
        assertEquals(0, pong.getPontosEsquerda());
        assertEquals(0, pong.getPontosDireita());
        assertEquals(800, pong.getLarguraCampo(), 1e-6);
        assertEquals(500, pong.getAlturaCampo(), 1e-6);
        assertFalse(pong.isEncerrado());
        assertEquals(Pong.Modo.CLASSICO, pong.getModo());
    }

    @Test
    public void doisJogadoresNaoCriaCPU() {
        assertTrue(pong.isDoisJogadores());
    }

    @Test
    public void versusComputadorCriaCPU() {
        pong.configurar(Pong.Modo.CLASSICO, Computador.Dificuldade.DIFICIL, false, 7, 120);
        assertFalse(pong.isDoisJogadores());
        assertEquals(Computador.Dificuldade.DIFICIL, pong.getDificuldade());
    }

    @Test
    public void camposDeDimensaoSaoExpostos() {
        assertEquals(LARGURA, pong.getLarguraCampo(), 1e-6);
        assertEquals(ALTURA, pong.getAlturaCampo(), 1e-6);
    }

    // ------------------------------------------------------------- Gols

    @Test
    public void bolaQuePassaPelaDireitaMarcaParaEsquerda() {
        forcarGolDaEsquerda();
        assertEquals(1, pong.getPontosEsquerda());
        assertEquals(0, pong.getPontosDireita());
        assertFalse(pong.isEncerrado());
    }

    @Test
    public void quemSofreuOSaca() {
        forcarGolDaEsquerda();
        // P2 sofreu o gol e saca → bola vai para a esquerda.
        assertTrue("sacada deve sair para a esquerda", pong.getBola().getVx() < 0);
    }

    @Test
    public void golDaDireitaSacaParaDireita() {
        avancarAteSacar();
        // Bola para fora pela esquerda → ponto do jogador 2.
        pong.getRaqueteEsquerda().setY(0);
        pong.getBola().centralizar(LARGURA / 2.0, ALTURA / 2.0, -1);
        int alvo = pong.getPontosDireita() + 1;
        for (int i = 0; i < 500 && pong.getPontosDireita() < alvo; i++) {
            pong.atualizar(DT);
        }
        avancarAteSacar();
        assertEquals(1, pong.getPontosDireita());
        assertTrue("P1 sofreu, P2 saca para a direita", pong.getBola().getVx() > 0);
    }

    @Test
    public void vitoriaPorPontosEncerraPartida() {
        for (int g = 0; g < 5; g++) {
            forcarGolDaEsquerda();
        }
        assertTrue(pong.isEncerrado());
        assertEquals(1, pong.getVencedor());
        assertEquals(5, pong.getPontosEsquerda());
    }

    @Test
    public void sacadaAlternaADirecao() {
        forcarGolDaEsquerda();
        forcarGolDaEsquerda();
        assertEquals(2, pong.getPontosEsquerda());
    }

    // --------------------------------------------------------------- Tempo

    @Test
    public void modoTempoCriaCronometro() {
        pong.configurar(Pong.Modo.TEMPO, Computador.Dificuldade.MEDIO, true, 7, 120);
        assertNotNull(pong.getCronometro());
        assertEquals(Pong.Modo.TEMPO, pong.getModo());
    }

    @Test
    public void tempoZeradoComVantagemEncerra() {
        pong.configurar(Pong.Modo.TEMPO, Computador.Dificuldade.MEDIO, true, 7, 60);
        forcarGolDaEsquerda();
        int pontos = pong.getPontosEsquerda();
        // Envelhece o cronômetro até o fim.
        pong.getCronometro().atualizar(600);
        for (int i = 0; i < 60; i++) {
            pong.atualizar(DT);
        }
        assertTrue(pong.isEncerrado());
        assertEquals(1, pong.getVencedor());
        assertEquals(pontos, pong.getPontosEsquerda());
    }

    @Test
    public void tempoZeradoEmpatadoFicaEmGolDeOuro() {
        pong.configurar(Pong.Modo.TEMPO, Computador.Dificuldade.MEDIO, true, 7, 60);
        pong.getCronometro().atualizar(600);
        for (int i = 0; i < 60; i++) {
            pong.atualizar(DT);
        }
        assertFalse("empate deve continuar em gol de ouro", pong.isEncerrado());
        assertTrue(pong.isGolDeOuro());
    }

    @Test
    public void golDeOuroDecideNaHora() {
        pong.configurar(Pong.Modo.TEMPO, Computador.Dificuldade.MEDIO, true, 7, 60);
        pong.getCronometro().atualizar(600);
        for (int i = 0; i < 60; i++) {
            pong.atualizar(DT);
        }
        assertTrue(pong.isGolDeOuro());
        forcarGolDaEsquerda();
        assertTrue(pong.isEncerrado());
        assertEquals(1, pong.getVencedor());
    }

    @Test
    public void rampaDeTempoAceleraABola() {
        pong.configurar(Pong.Modo.TEMPO, Computador.Dificuldade.MEDIO, true, 7, 60);
        pong.getCronometro().atualizar(55);
        pong.atualizar(DT);
        double velocidade = pong.getBola().getVelocidade();
        assertTrue("bola deve acelerar com o tempo esgotando",
                velocidade > Bola.VELOCIDADE_BASE * 1.5);
    }

    // ----------------------------------------------------------- Especial

    @Test
    public void especialExigeBateriaCheiaERecarregaNaRabatida() {
        // Começa sem carga: só libera depois de 3 rebatidas da própria raquete.
        assertEquals(0, pong.getCargaEspecial(1));
        assertFalse("sem bateria, não usa", pong.usarEspecial(1));

        carregarBateria(1);
        int carga2Antes = pong.getCargaEspecial(2);
        assertTrue(pong.usarEspecial(1));
        assertEquals("uso zera a bateria", 0, pong.getCargaEspecial(1));
        assertEquals("cada jogador tem a própria bateria",
                carga2Antes, pong.getCargaEspecial(2));
        assertFalse("sem carga, não usa de novo", pong.usarEspecial(1));
    }

    @Test
    public void especialNaoRecarregaComRabatidaDaPropriaEnquantoAtivo() {
        carregarBateria(1);
        assertTrue(pong.usarEspecial(1));
        assertEquals(0, pong.getCargaEspecial(1));

        // Mesmo continued rebatendo na própria raquete, a carga não sobe.
        jogarRebatendo(1, (int) (Pong.DURACAO_ESPECIAL / DT));
        assertEquals("não carrega durante o próprio especial", 0, pong.getCargaEspecial(1));
        assertTrue(pong.isEspecial1Ativo());
    }

    @Test
    public void especialAtivaBolaEmChamasEDecai() {
        assertFalse(pong.isBolaEmChamas());
        carregarBateria(1);
        assertTrue(pong.usarEspecial(1));
        assertTrue(pong.isEspecial1Ativo());
        assertTrue(pong.isBolaEmChamas());

        // Passa o tempo além da duração do especial (5s). Reenvia a bola a cada
        // quadro para a velocidade não subir por rebatida e falsear a checagem.
        int quadros = (int) (Pong.DURACAO_ESPECIAL / DT) + 30;
        for (int i = 0; i < quadros; i++) {
            if (pong.isAguardandoSaque()) {
                avancarAteSacar();
            }
            pong.getBola().centralizar(LARGURA / 2.0, ALTURA / 2.0, 1);
            pong.atualizar(DT);
        }
        assertFalse(pong.isEspecial1Ativo());
        assertFalse(pong.isBolaEmChamas());
    }

    @Test
    public void bolaFicaEmChamasPorVelocidadeAlta() {
        pong.getBola().ajustarVelocidade(Pong.LIMIAR_CHAMAS + 10);
        assertTrue("velocidade acima do limiar deve acender a bola",
                pong.isBolaEmChamas());
    }

    @Test
    public void bolaSaiDasChamasAposVelocidadeBaixa() {
        pong.getBola().ajustarVelocidade(Pong.LIMIAR_CHAMAS + 10);
        assertTrue(pong.isBolaEmChamas());
        pong.getBola().redefinirVelocidade(1);
        assertFalse(pong.isBolaEmChamas());
    }

    @Test
    public void especialExpiraMesmoSemComerOTempo() {
        pong.usarEspecial(1);
        for (int i = 0; i < (int) (Pong.DURACAO_ESPECIAL / DT) + 10; i++) {
            pong.atualizar(DT);
        }
        assertFalse(pong.isEspecial1Ativo());
    }

    // ----------------------------------------------------------- Física

    @Test
    public void moveJogadorDentroDosLimites() {
        Raquete r1 = pong.getRaqueteEsquerda();
        r1.setY(0);
        pong.moverJogador(1, -1, 1.0);
        assertEquals(0, r1.getY());

        r1.setY((int) (ALTURA - Raquete.ALTURA));
        pong.moverJogador(1, 1, 1.0);
        assertEquals((int) (ALTURA - Raquete.ALTURA), r1.getY());
    }

    @Test
    public void bolaRebatidaPelaRaqueteNãoMarcaGol() {
        // Bola na direção da raquete direita com a raquete no caminho.
        Raquete r2 = pong.getRaqueteDireita();
        r2.centralizar();
        pong.getBola().centralizar(LARGURA / 2.0, ALTURA / 2.0, 1);
        for (int i = 0; i < 30; i++) {
            pong.atualizar(DT);
        }
        assertEquals(0, pong.getPontosEsquerda());
        assertEquals(0, pong.getPontosDireita());
        assertFalse(pong.isEncerrado());
    }

    // --------------------------------------------------------- Estatísticas

    @Test
    public void estatisticasDeTrocaERebatidaSaoContadas() {
        // Rali entre as duas raquetes em 2P (ninguém move): a bola quica e a
        // melhor troca deve crescer acima de uma única rebatida.
        pong.getBola().centralizar(LARGURA / 2.0, ALTURA / 2.0, 1);
        for (int i = 0; i < 2000 && !pong.isEncerrado(); i++) {
            pong.atualizar(DT);
        }
        assertTrue("melhor troca deve acumular rebatidas (foi " + pong.getMelhorTroca() + ")",
                pong.getMelhorTroca() >= 2);
    }

    @Test
    public void velocidadeMaximaRegistrada() {
        pong.getBola().ajustarVelocidade(Pong.LIMIAR_CHAMAS + 20);
        pong.atualizar(DT);
        assertTrue(pong.getVelocidadeMaxima() >= Pong.LIMIAR_CHAMAS + 20);
    }

    @Test
    public void iniciarPartidaZeraAsEstatisticas() {
        forcarGolDaEsquerda();
        pong.getBola().ajustarVelocidade(700);
        pong.atualizar(DT);
        assertTrue(pong.getVelocidadeMaxima() >= 700);

        pong.iniciarPartida();
        assertEquals(0, pong.getPontosEsquerda());
        assertEquals(0, pong.getPontosDireita());
        assertEquals(0, pong.getMelhorTroca());
        assertEquals(0.0, pong.getVelocidadeMaxima(), 1e-9);
        assertFalse(pong.isGolDeOuro());
        assertFalse(pong.isEncerrado());
    }

    @Test
    public void encerradoTravaAtualizacao() {
        for (int g = 0; g < 5; g++) {
            forcarGolDaEsquerda();
        }
        assertTrue(pong.isEncerrado());
        int pontos = pong.getPontosEsquerda();
        forcarGolDaEsquerda();
        assertEquals("nenhum gol após o fim", pontos, pong.getPontosEsquerda());
    }

    @Test
    public void configurarReposicionaEPermiteNovaPartida() {
        for (int g = 0; g < 5; g++) {
            forcarGolDaEsquerda();
        }
        assertTrue(pong.isEncerrado());
        pong.configurar(Pong.Modo.CLASSICO, Computador.Dificuldade.FACIL, true, 10, 120);
        assertFalse(pong.isEncerrado());
        assertEquals(0, pong.getPontosEsquerda());
    }
}