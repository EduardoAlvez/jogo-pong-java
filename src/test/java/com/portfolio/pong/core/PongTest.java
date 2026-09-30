package com.portfolio.pong.core;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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

    /**
     * Estaciona a bola no campo do jogador, para o especial ficar liberado.
     * Sem isto o especial dependeria de a bola ter parado no lado certo por
     * acaso ao fim dos 6000 quadros de {@link #carregarBateria}.
     */
    private void bolaNoLadoDo(int jogador) {
        double x = jogador == 1 ? LARGURA / 4.0 : LARGURA * 3.0 / 4.0;
        pong.getBola().centralizar(x, ALTURA / 2.0, jogador == 1 ? -1 : 1);
    }

    /** Cria a segunda bola (recurso do prêmio DUPLO), por reflexão. */
    private void adicionarBolaExtra() throws Exception {
        Method m = Pong.class.getDeclaredMethod("adicionarBolaExtra");
        m.setAccessible(true);
        m.invoke(pong);
    }

    /**
     * Move a bola preservando a velocidade. {@link #bolaNoLadoDo} usa
     * {@code centralizar}, que zera a velocidade na base — isso apagaria
     * justamente a velocidade dobrada que o segundo especial precisa capturar
     * para reproduzir o defeito.
     */
    private void moverBolaSemMudarVelocidade(Bola b, double x) throws Exception {
        Field f = Bola.class.getDeclaredField("x");
        f.setAccessible(true);
        f.setDouble(b, x);
    }

    /** Encerra o especial do jogador sem esperar os 5s de relógio. */
    private void encerrarEspecialDe(int jogador) throws Exception {
        Method m = Pong.class.getDeclaredMethod("encerrarEspecial", int.class);
        m.setAccessible(true);
        m.invoke(pong, jogador);
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
        bolaNoLadoDo(1);
        assertTrue(pong.usarEspecial(1));
        assertEquals("uso zera a bateria", 0, pong.getCargaEspecial(1));
        assertEquals("cada jogador tem a própria bateria",
                carga2Antes, pong.getCargaEspecial(2));
        assertFalse("sem carga, não usa de novo", pong.usarEspecial(1));
    }

    @Test
    public void especialNaoRecarregaComRabatidaDaPropriaEnquantoAtivo() {
        carregarBateria(1);
        bolaNoLadoDo(1);
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
        bolaNoLadoDo(1);
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
        // Sem bateria e sem bola no lado certo o uso nem sairia; este teste só
        // faz sentido com o especial realmente ativado.
        carregarBateria(1);
        bolaNoLadoDo(1);
        assertTrue("premissa: o especial ativou", pong.usarEspecial(1));

        for (int i = 0; i < (int) (Pong.DURACAO_ESPECIAL / DT) + 10; i++) {
            pong.atualizar(DT);
        }
        assertFalse(pong.isEspecial1Ativo());
    }

    // -------------------------------------------- Especial: bola no seu campo

    @Test
    public void especialNaoAtivaComBolaNoCampoDoAdversario() {
        carregarBateria(1);
        bolaNoLadoDo(2);
        assertFalse("bola no campo do adversário não ativa",
                pong.usarEspecial(1));
        assertEquals("bloqueado não gasta carga",
                Pong.CARGAS_PARA_ESPECIAL, pong.getCargaEspecial(1));
        assertFalse(pong.isEspecial1Ativo());
    }

    @Test
    public void especialAtivaComBolaNoProprioCampo() {
        carregarBateria(1);
        bolaNoLadoDo(1);
        assertTrue(pong.usarEspecial(1));
        assertTrue(pong.isEspecial1Ativo());
    }

    @Test
    public void especialAtivaComBolaNoMeioDoCampo() {
        carregarBateria(1);
        pong.getBola().centralizar(LARGURA / 2.0, ALTURA / 2.0, -1);
        assertTrue("o meio do campo é territory neutro e libera",
                pong.usarEspecial(1));
    }

    @Test
    public void especialDoJogadorDoisUsaComBolaNoCampoDireito() {
        carregarBateria(2);
        bolaNoLadoDo(2);
        assertTrue(pong.usarEspecial(2));
        assertTrue(pong.isEspecial2Ativo());
    }

    @Test
    public void especialDoJogadorDoisNaoUsaComBolaNoCampoEsquerdo() {
        carregarBateria(2);
        bolaNoLadoDo(1);
        assertFalse("campo do jogador 1 não libera o especial do 2",
                pong.usarEspecial(2));
        assertEquals(Pong.CARGAS_PARA_ESPECIAL, pong.getCargaEspecial(2));
    }

    @Test
    public void especialLiberaComQualquerBolaNoMeuLado() throws Exception {
        carregarBateria(1);
        adicionarBolaExtra();
        // Uma bola de cada lado: a que está comigo já basta.
        pong.getBolas().get(0).centralizar(LARGURA * 3.0 / 4.0, 100, -1);
        pong.getBolas().get(1).centralizar(LARGURA / 4.0, 400, 1);
        assertTrue(pong.usarEspecial(1));
    }

    @Test
    public void especialTravaSeTodasAsBolasEstaoNoCampoDoAdversario() throws Exception {
        carregarBateria(1);
        adicionarBolaExtra();
        pong.getBolas().get(0).centralizar(LARGURA * 3.0 / 4.0, 100, -1);
        pong.getBolas().get(1).centralizar(LARGURA * 0.6, 400, -1);
        assertFalse("nenhuma bola do meu lado", pong.usarEspecial(1));
        assertEquals(Pong.CARGAS_PARA_ESPECIAL, pong.getCargaEspecial(1));
    }

    // ------------------------------------------------ Especial: um por vez

    @Test
    public void especialDoJogadorDoisTravaComOEspecialDoJogadorUmAtivo() {
        carregarBateria(1);
        carregarBateria(2);
        bolaNoLadoDo(1);
        assertTrue(pong.usarEspecial(1));

        // Bola no lado do jogador 2, então só a exclusividade pode barrar.
        bolaNoLadoDo(2);
        assertFalse("um especial por vez", pong.usarEspecial(2));
        assertEquals("a bateria do 2 não é gasta enquanto o 1 usa",
                Pong.CARGAS_PARA_ESPECIAL, pong.getCargaEspecial(2));
        assertFalse(pong.isEspecial2Ativo());
    }

    @Test
    public void especialLiberaAposOEspecialDoOutroExpirar() throws Exception {
        carregarBateria(1);
        carregarBateria(2);
        bolaNoLadoDo(1);
        assertTrue(pong.usarEspecial(1));
        bolaNoLadoDo(2);
        assertFalse(pong.usarEspecial(2));

        encerrarEspecialDe(1);
        assertTrue("liberado assim que o do jogador 1 cai",
                pong.usarEspecial(2));
        assertTrue(pong.isEspecial2Ativo());
    }

    /**
     * Regressão dos dois bugs da sobreposição: o segundo especial era aceito e
     * capturava a velocidade já dobrada como base, então a bola subia para o
     * teto de 900 na virada e ficava presa no dobro da velocidade.
     */
    @Test
    public void bolaNaoRachaNemSobeParaOTetoComOsDoisQuerendoUsar() throws Exception {
        double base = Bola.VELOCIDADE_BASE;
        carregarBateria(1);
        carregarBateria(2);
        bolaNoLadoDo(1);
        assertTrue(pong.usarEspecial(1));
        pong.atualizar(DT);
        assertEquals("o especial dobra a velocidade", base * 2.0,
                pong.getBola().getVelocidade(), 0.01);

        // Move para o lado do jogador 2 sem zerar a velocidade: é a 480 que o
        // segundo especial ia capturar como se fosse a base.
        moverBolaSemMudarVelocidade(pong.getBola(), LARGURA * 3.0 / 4.0);
        assertFalse("o segundo especial não entra", pong.usarEspecial(2));

        // O do jogador 1 cai: sem especial ativo, a bola volta à base — e não ao
        // teto de 900, que era o que a base capturada pelo jogador 2 causava.
        encerrarEspecialDe(1);
        pong.atualizar(DT);
        assertEquals("a bola volta à base, não vai a 900", base,
                pong.getBola().getVelocidade(), 0.01);

        assertTrue(pong.usarEspecial(2));
        pong.atualizar(DT);
        assertEquals("o do jogador 2 dobra a partir da base, não do dobro",
                base * 2.0, pong.getBola().getVelocidade(), 0.01);
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