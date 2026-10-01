package com.portfolio.pong.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * Testes do registro de botões e do highlight do mouse.
 *
 * <p>Rodam sem display: {@link Botaos} e {@link Botao} não dependem de Swing,
 * que é a razão de terem sido extraídos de {@code TelaPong}.</p>
 *
 * <p><b>O teste que importa é {@link #highlightSobreviveARepintura}.</b> Ele
 * reproduz o defeito real: o ponteiro marca o botão, a pintura recria a lista
 * com instâncias novas, e o destaque precisa continuar lá. Com o highlight
 * guardado dentro de cada instância de {@code Botao} — como estava antes — este
 * teste falha, porque a instância nova nasce com o estado zerado.</p>
 *
 * @author Eduardo Alvez
 */
public class BotaosTest {

    private final Botaos botoes = new Botaos();

    // ---------------------------------------------------------------- identidade

    @Test
    public void chaveDependeDaGeometria() {
        Botao a = new Botao("JOGAR", 10, 20, 150, 34, () -> { });
        Botao b = new Botao("JOGAR", 10, 20, 150, 34, () -> { });
        assertEquals(a.chave(), b.chave());
    }

    @Test
    public void chaveMudaComAPosicao() {
        Botao a = new Botao("JOGAR", 10, 20, 150, 34, () -> { });
        Botao b = new Botao("JOGAR", 11, 20, 150, 34, () -> { });
        assertFalse(a.chave().equals(b.chave()));
    }

    @Test
    public void chaveIgnoraOTexto() {
        // O botão de som alterna "🔊 Som" / "🔇 Sem" no MESMO lugar. Se o texto
        // entrasse na chave, o highlight perderia no instante do clique.
        Botao ligado = new Botao("🔊 Som", 40, 400, 110, 26, () -> { });
        Botao desligado = new Botao("🔇 Som", 40, 400, 110, 26, () -> { });
        assertEquals(ligado.chave(), desligado.chave());
    }

    // ------------------------------------------------------------------ hit test

    @Test
    public void contemPontoDentro() {
        Botao b = new Botao("JOGAR", 100, 100, 150, 34, () -> { });
        assertTrue(b.contem(150, 110));
    }

    @Test
    public void contemAsBordas() {
        Botao b = new Botao("JOGAR", 100, 100, 150, 34, () -> { });
        assertTrue(b.contem(100, 100));
        assertTrue(b.contem(250, 134));
    }

    @Test
    public void naoContemPontoFora() {
        Botao b = new Botao("JOGAR", 100, 100, 150, 34, () -> { });
        assertFalse(b.contem(99, 110));
        assertFalse(b.contem(150, 135));
    }

    @Test
    public void botaoInativoNuncaEhEncontrado() {
        Botao b = new Botao("JOGAR", 100, 100, 150, 34, () -> { });
        b.ativo = false;
        assertFalse(b.contem(150, 110));
    }

    // -------------------------------------------------------------- hover isolado

    @Test
    public void moverParaMarcaOBotaoSobOPonteiro() {
        Botao b = botoes.adicionar(new Botao("JOGAR", 100, 100, 150, 34, () -> { }));
        assertTrue(botoes.moverPara(150, 110));
        assertTrue(botoes.temHighlight(b));
    }

    @Test
    public void moverParaForaDeTodosDesmarca() {
        Botao b = botoes.adicionar(new Botao("JOGAR", 100, 100, 150, 34, () -> { }));
        botoes.moverPara(150, 110);
        assertTrue(botoes.moverPara(10, 10));
        assertFalse(botoes.temHighlight(b));
    }

    @Test
    public void moverDentroDoMesmoBotaoNaoMudaOEstado() {
        // Evita repintar a tela a cada pixel de movimento dentro do mesmo botão.
        botoes.adicionar(new Botao("JOGAR", 100, 100, 150, 34, () -> { }));
        assertTrue(botoes.moverPara(150, 110));
        assertFalse(botoes.moverPara(151, 111));
        assertFalse(botoes.moverPara(200, 120));
    }

    @Test
    public void nenhumBotaoNaoTemHighlight() {
        botoes.adicionar(new Botao("JOGAR", 100, 100, 150, 34, () -> { }));
        botoes.moverPara(10, 10);
        assertNull(botoes.chaveHighlight());
    }

    // ------------------------------------------------- hover simultâneo (regra 4)

    @Test
    public void doisBotoesEmCamposDiferentes() {
        // Cenário simultâneo: as duas setas do seletor de skin, lado a lado.
        Botao esquerda = botoes.adicionar(new Botao("◀", 200, 300, 26, 26, () -> { }));
        Botao direita = botoes.adicionar(new Botao("▶", 500, 300, 26, 26, () -> { }));

        botoes.moverPara(210, 310);
        assertTrue(botoes.temHighlight(esquerda));
        assertFalse(botoes.temHighlight(direita));

        botoes.moverPara(510, 310);
        assertFalse(botoes.temHighlight(esquerda));
        assertTrue(botoes.temHighlight(direita));
    }

    @Test
    public void botoesSobrepostosRespeitamALista() {
        // Sobreposição deliberada: quem está por último na lista vence, como no
        // desenho. Sem isso, dois retângulos iguais dependeriam da ordem de
        // varredura e o clique poderia acionar o botão errado.
        Botao embaixo = botoes.adicionar(new Botao("fundo", 100, 100, 200, 50, () -> { }));
        Botao cima = botoes.adicionar(new Botao("cima", 100, 100, 200, 50, () -> { }));
        assertEquals(cima, botoes.sob(150, 120));
        botoes.moverPara(150, 120);
        // Os dois são geometricamente iguais, então o highlight não consegue
        // distingui-los: é o preço da identidade geométrica, e o motivo de
        // sobreposição ser proibida no desenho.
        assertTrue(botoes.temHighlight(cima));
        assertTrue(embaixo.contem(150, 120));
        assertEquals(cima.chave(), embaixo.chave());
    }

    // -------------------------------------- reversão: highlight some (regra 4)

    @Test
    public void highlightDesapareceQuandoOMouseSai() {
        Botao b = botoes.adicionar(new Botao("PAUSA", 300, 200, 160, 32, () -> { }));
        botoes.moverPara(350, 210);
        assertTrue(botoes.temHighlight(b));

        botoes.moverPara(0, 0);
        assertFalse(botoes.temHighlight(b));
        assertNull(botoes.chaveHighlight());
    }

    @Test
    public void highlightVoltaQuandoOMouseVolta() {
        Botao b = botoes.adicionar(new Botao("PAUSA", 300, 200, 160, 32, () -> { }));
        botoes.moverPara(350, 210);
        botoes.moverPara(0, 0);
        assertTrue(botoes.moverPara(350, 210));
        assertTrue(botoes.temHighlight(b));
    }

    // ------------------------------------- o defeito: sobreviver à repinturação

    @Test
    public void highlightSobreviveARepintura() {
        Botao antes = botoes.adicionar(new Botao("JOGAR ▶", 40, 400, 150, 34, () -> { }));
        botoes.moverPara(100, 410);
        assertTrue(botoes.temHighlight(antes));

        // O que a pintura faz a cada quadro: limpa a lista e recria os botões
        // como instâncias novas. É aqui que o highlight morria.
        botoes.limpar();
        Botao depois = botoes.adicionar(new Botao("JOGAR ▶", 40, 400, 150, 34, () -> { }));

        assertNotNull(depois);
        assertTrue("o highlight tem de sobreviver à repinturação",
                botoes.temHighlight(depois));
        assertFalse(antes == depois); // instancia nova, de fato
    }

    @Test
    public void highlightNaoVazaParaOVizinho() {
        botoes.moverPara(0, 0);
        botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));

        botoes.moverPara(120, 110);
        botoes.limpar();
        Botao naRepintura = botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        Botao vizinho = botoes.adicionar(new Botao("B", 160, 100, 50, 20, () -> { }));

        assertTrue(botoes.temHighlight(naRepintura));
        assertFalse(botoes.temHighlight(vizinho));
    }

    @Test
    public void repinturacaoSemMouseNaoDeixaHighlight() {
        botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        assertNull(botoes.chaveHighlight());
        botoes.limpar();
        Botao novo = botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        assertFalse(botoes.temHighlight(novo));
    }

    // -------------------------------------------------------------- clique

    @Test
    public void acionarExecutaABacao() {
        AtomicInteger cliques = new AtomicInteger();
        botoes.adicionar(new Botao("JOGAR", 100, 100, 150, 34, cliques::incrementAndGet));
        assertTrue(botoes.acionar(150, 110));
        assertEquals(1, cliques.get());
    }

    @Test
    public void acionarForaDeTodosNaoFazNada() {
        AtomicInteger cliques = new AtomicInteger();
        botoes.adicionar(new Botao("JOGAR", 100, 100, 150, 34, cliques::incrementAndGet));
        assertFalse(botoes.acionar(10, 10));
        assertEquals(0, cliques.get());
    }

    @Test
    public void acionarExecutaUmaVezPorClique() {
        AtomicInteger cliques = new AtomicInteger();
        botoes.adicionar(new Botao("JOGAR", 100, 100, 150, 34, cliques::incrementAndGet));
        botoes.acionar(150, 110);
        assertEquals("o clique não pode disparar duas ações", 1, cliques.get());
    }

    @Test
    public void acionarNaoMoveOHover() {
        // Clicar não pode alterar o estado do mouse: o ponteiro continua onde está.
        AtomicInteger cliques = new AtomicInteger();
        Botao b = botoes.adicionar(new Botao("JOGAR", 100, 100, 150, 34, cliques::incrementAndGet));
        botoes.moverPara(150, 110);
        botoes.acionar(150, 110);
        assertTrue(botoes.temHighlight(b));
    }

    @Test
    public void acionarEmBotaoInativoNaoFazNada() {
        AtomicInteger cliques = new AtomicInteger();
        Botao b = botoes.adicionar(new Botao("JOGAR", 100, 100, 150, 34, cliques::incrementAndGet));
        b.ativo = false;
        assertFalse(botoes.acionar(150, 110));
        assertEquals(0, cliques.get());
    }

    // --------------------------------------------------------------- lista

    @Test
    public void limparEsvaziaARegistro() {
        botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        botoes.adicionar(new Botao("B", 160, 100, 50, 20, () -> { }));
        assertEquals(2, botoes.tamanho());
        botoes.limpar();
        assertEquals(0, botoes.tamanho());
    }

    @Test
    public void adicionarDevolveOMesmoBotao() {
        Botao b = new Botao("A", 100, 100, 50, 20, () -> { });
        assertTrue(b == botoes.adicionar(b));
    }

    @Test
    public void listaVaziaNaoQuebraMoverPara() {
        assertFalse(botoes.moverPara(100, 100));
        assertNull(botoes.chaveHighlight());
        assertNull(botoes.sob(100, 100));
        assertFalse(botoes.acionar(100, 100));
    }

    /**
     * Três repinturações seguidas do seletor de skin: o "◀" continua em
     * destaque e o "▶" nunca rouba o highlight dele.
     */
    @Test
    public void seletorDeSkinRepinturadoVariasVezes() {
        // O ponteiro é posicionado UMA vez, antes do laço. Chamar moverPara a
        // cada quadro reconstituiria o highlight e o teste passaria mesmo com o
        // defeito presente — foi exatamente o que aconteceu na primeira versão
        // deste teste, e a mutação não o derrubou.
        botoes.limpar();
        botoes.adicionar(new Botao("◀", 300, 250, 26, 26, () -> { }));
        botoes.adicionar(new Botao("▶", 500, 250, 26, 26, () -> { }));
        botoes.moverPara(310, 260);

        for (int quadro = 0; quadro < 3; quadro++) {
            botoes.limpar();
            Botao anterior = botoes.adicionar(new Botao("◀", 300, 250, 26, 26, () -> { }));
            Botao proximo = botoes.adicionar(new Botao("▶", 500, 250, 26, 26, () -> { }));

            assertTrue("quadro " + quadro + ": o ◀ deve continuar em destaque sem novo mouseMoved",
                    botoes.temHighlight(anterior));
            assertFalse("quadro " + quadro + ": o ▶ não pode roubar o destaque",
                    botoes.temHighlight(proximo));
        }
    }
}
