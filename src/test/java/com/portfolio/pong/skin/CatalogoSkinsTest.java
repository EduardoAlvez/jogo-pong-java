package com.portfolio.pong.skin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Testes do {@link CatalogoSkins}: presets, seleção, conversão de cores e
 * persistência da skin personalizada em {@code user.home}.
 */
public class CatalogoSkinsTest {

    private String homeOriginal;

    @Before
    public void usarHomeTemporaria() {
        homeOriginal = System.getProperty("user.home");
        Path tmp = Paths.get(System.getProperty("java.io.tmpdir"), "pong-skin-test-" + System.nanoTime());
        System.setProperty("user.home", tmp.toString());
    }

    @After
    public void restaurarHome() {
        if (homeOriginal != null) {
            System.setProperty("user.home", homeOriginal);
        }
        // Limpa o arquivo de preferências criado pelo teste.
        try {
            Files.deleteIfExists(caminhoArquivo());
        } catch (Exception ignorada) {
            // best-effort
        }
    }

    private Path caminhoArquivo() {
        return Paths.get(System.getProperty("user.home"), ".jogo-pong-skin.properties");
    }

    private void prepararArquivoParaEscrita() throws Exception {
        Files.createDirectories(caminhoArquivo().getParent());
    }

    @Test
    public void catalogoOfereceSeisPresets() {
        CatalogoSkins c = new CatalogoSkins();
        assertEquals(6, c.getPresets().size());
    }

    @Test
    public void nomesIncluemPresetsEPersonalizada() {
        CatalogoSkins c = new CatalogoSkins();
        String[] nomes = c.getNomes();
        assertEquals(7, nomes.length);
        assertEquals(Skin.PERSONALIZADA, nomes[nomes.length - 1]);
    }

    @Test
    public void padraoInicialEhClassico() {
        CatalogoSkins c = new CatalogoSkins();
        Skin s = c.obterSelecionada();
        assertEquals("Clássico", s.getNome());
        assertFalse(s.hasScanlines());
    }

    @Test
    public void retoPossuiScanlines() {
        CatalogoSkins c = new CatalogoSkins();
        c.selecionar("Retrô");
        assertTrue(c.obterSelecionada().hasScanlines());
    }

    @Test
    public void selecionarNomeInvalidoIgnora() {
        CatalogoSkins c = new CatalogoSkins();
        c.selecionar("Inexistente");
        assertEquals("Clássico", c.obterSelecionada().getNome());
    }

    @Test
    public void selecionarComNullIgnora() {
        CatalogoSkins c = new CatalogoSkins();
        c.selecionar(null);
        assertEquals("Clássico", c.obterSelecionada().getNome());
    }

    @Test
    public void toHexEparseColorSaoInversos() {
        Color cor = new Color(0x12, 0x5A, 0xB3);
        assertEquals("#125AB3", CatalogoSkins.toHex(cor));
        assertEquals(cor, CatalogoSkins.parseColor("#125AB3"));
        assertEquals(cor, CatalogoSkins.parseColor("125AB3"));
    }

    @Test
    public void parseColorRejeitaInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> CatalogoSkins.parseColor(null));
        assertThrows(IllegalArgumentException.class, () -> CatalogoSkins.parseColor(""));
        assertThrows(IllegalArgumentException.class, () -> CatalogoSkins.parseColor("#ff"));
        assertThrows(IllegalArgumentException.class, () -> CatalogoSkins.parseColor("#ZZZZZZ"));
    }

    @Test
    public void aplicarPersonalizadaPersisteEReabre() {
        CatalogoSkins c = new CatalogoSkins();
        c.aplicarPersonalizada(Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE,
                Color.MAGENTA, Color.YELLOW, "B");

        assertTrue("arquivo deve ter sido gravado", Files.exists(caminhoArquivo()));

        // Novo catálogo (nova execução) lê os valores salvos.
        CatalogoSkins d = new CatalogoSkins();
        Skin s = d.obterSelecionada();
        assertEquals(Skin.PERSONALIZADA, s.getNome());
        assertEquals(Color.RED, s.getCorCampo());
        assertEquals(Color.BLUE, s.getCorLinha());
        assertEquals(Color.MAGENTA, s.getCorRaquete2());
        assertEquals("B", s.getRotuloBola());
    }

    @Test
    public void personalizadaSemConfiguracaoCaiEmClassico() {
        CatalogoSkins c = new CatalogoSkins();
        c.selecionar(Skin.PERSONALIZADA);
        Skin s = c.obterSelecionada();
        assertNotNull(s);
        assertEquals("Personalizada", s.getNome());
        // Cores são os padrões (clássico) por não haver personalização salva.
        assertEquals(new Color(0x1E7A3C), s.getCorCampo());
    }

    @Test
    public void arquivoCorrompidoCaiEmClassico() throws Exception {
        prepararArquivoParaEscrita();
        Files.write(caminhoArquivo(), "caca!!! ".getBytes());
        CatalogoSkins c = new CatalogoSkins();
        assertEquals("Clássico", c.obterSelecionada().getNome());
    }

    @Test
    public void arquivoComCorHexInvalidaUsaFallback() throws Exception {
        prepararArquivoParaEscrita();
        Files.write(caminhoArquivo(),
                ("skin.selecionada=Personalizada\ncustom.campo=XYZ\ncustom.rotulo=A\n").getBytes());
        CatalogoSkins c = new CatalogoSkins();
        Skin s = c.obterSelecionada();
        assertEquals(new Color(0x1E7A3C), s.getCorCampo());
    }
}