package com.portfolio.pong.audio;

import org.junit.After;
import org.junit.Test;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Testes dos efeitos sonoros: interruptor global, arquivos {@code .wav} presentes
 * e decodificáveis pelo classpath (são recursos do jar) e, principalmente, a
 * leitura de áudio quando o recurso só existe <em>dentro</em> de um jar.
 *
 * <p>Esse último ponto é uma regressão real: o jogo é distribuído dentro de um
 * executável e o {@code AudioSystem} precisa de {@code mark/reset} para ler o
 * cabeçalho do WAV. O stream cru devolvido por uma entrada de jar não suporta
 * {@code mark}, e o jogo ficava mudo — sem erro visível — tanto no jar quanto
 * no {@code .exe}.
 */
public class EfeitosSonorosTest {

    @After
    public void restaurarSom() {
        EfeitosSonoros.setLigado(true);
        EfeitosSonoros.recarregar();
        EfeitosSonoros.trocarFabrica(null);
        EfeitosSonoros.definirMixers(null);
    }

    @Test
    public void interruptorGlobalLigaEDesliga() {
        EfeitosSonoros.setLigado(true);
        assertTrue(EfeitosSonoros.isLigado());
        EfeitosSonoros.setLigado(false);
        assertFalse(EfeitosSonoros.isLigado());
    }

    @Test
    public void sonsDesligadosNaoTocamNemQuebram() {
        EfeitosSonoros.setLigado(false);
        EfeitosSonoros.tocar(EfeitosSonoros.Som.REBATER);
        EfeitosSonoros.tocar(EfeitosSonoros.Som.GOL);
    }

    @Test
    public void todosOsWavsExistemNoClasspath() {
        for (EfeitosSonoros.Som som : EfeitosSonoros.Som.values()) {
            assertNotNull("recurso ausente: " + som,
                    EfeitosSonoros.class.getResource(caminhoSom(som)));
        }
    }

    @Test
    public void todosOsWavsSaoDecodificaveis() throws Exception {
        for (EfeitosSonoros.Som som : EfeitosSonoros.Som.values()) {
            try (AudioInputStream in = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(url(som).openStream()))) {
                assertTrue("formato inválido para " + som, in.getFrameLength() > 0);
            }
        }
    }

    /**
     * Reproduz a condição que fazia o executável ficar mudo, usando o caminho de
     * leitura do próprio jogo.
     *
     * <p>Um {@code file:} URL já devolve um {@link BufferedInputStream}, e por
     * isso rodar a suíte com os recursos soltos em disco <em>nunca</em>
     * reproduz o defeito — o teste passaria mesmo com o bug presente. A
     * diferença só aparece com a WAV de dentro de um jar, que é como o jogo é
     * distribuído. Aqui a WAV é passada direto para
     * {@link EfeitosSonoros#carregarDe(URL, String)}, então remover o
     * {@code BufferedInputStream} do código faz este teste falhar.
     */
    @Test
    public void wavDentroDeJarSoCarregaPeloCaminhoDoJogo() throws Exception {
        URL urlDeJar = somDentroDeJar();
        assertEquals("o teste só é fiel com um recurso de jar", "jar", urlDeJar.getProtocol());
        try (InputStream cru = urlDeJar.openStream()) {
            assertFalse("um stream de jar não aceita mark: é o que quebrava o som", cru.markSupported());
        }

        List<Clip> abertos = new ArrayList<>();
        EfeitosSonoros.definirMixers(List.of(new DublêsDeAudio.MixerFalso("saida")));
        EfeitosSonoros.trocarFabrica((mixer, formato) -> new DublêsDeAudio.ClipFalso(abertos));

        assertNotNull("a WAV do jar tem de carregar: é o que o executável faz",
                EfeitosSonoros.carregarDe(urlDeJar, "rebater.wav"));
        assertEquals("só um clip deve ter sido aberto", 1, abertos.size());
    }

    /**
     * O mesmo teste para os sete efeitos, já que qualquer um deles deixaria o
     * jogo mudo se o caminho de leitura estivesse errado.
     */
    @Test
    public void todosOsWavsDoJarCarregamPeloCaminhoDoJogo() throws Exception {
        List<Clip> abertos = new ArrayList<>();
        EfeitosSonoros.definirMixers(List.of(new DublêsDeAudio.MixerFalso("saida")));
        EfeitosSonoros.trocarFabrica((mixer, formato) -> new DublêsDeAudio.ClipFalso(abertos));

        for (EfeitosSonoros.Som som : EfeitosSonoros.Som.values()) {
            assertNotNull("não carregou do jar: " + som,
                    EfeitosSonoros.carregarDe(somDentroDeJarDe(som), som.name()));
        }
        assertEquals("um clip por efeito", EfeitosSonoros.Som.values().length, abertos.size());
    }

    /**
     * Com um mixer quebrado, o jogo não pode ficar mudo em silêncio: ele deve
     * registrar o motivo e expor o estado para a UI avisar o jogador.
     */
    @Test
    public void mixerQuebradoRegistraOEstadoEOTMotivo() throws Exception {
        EfeitosSonoros.trocarFabrica((mixer, formato) -> {
            throw new LineUnavailableException("dispositivo removido");
        });
        EfeitosSonoros.definirMixers(List.of(mixerFalso("saida")));

        EfeitosSonoros.tocar(EfeitosSonoros.Som.REBATER);
        aguardarCarga();

        assertTrue("a UI precisa saber que o áudio não está disponível",
                EfeitosSonoros.isAudioIndisponivel());
        assertTrue("o motivo da falha não pode ser engolido em silêncio",
                EfeitosSonoros.getUltimoErro().contains("dispositivo removido"));
    }

    /**
     * Quando o primeiro mixer falha, o próximo precisa receber chance real: o
     * stream da tentativa anterior já foi consumido e não pode ser reaproveitado.
     */
    @Test
    public void caiParaOMixerSeguinteQuandoOPrimeiroFalha() throws Exception {
        List<Clip> abertos = new ArrayList<>();
        EfeitosSonoros.definirMixers(List.of(mixerFalso("quebrado"), mixerFalso("bom")));
        EfeitosSonoros.trocarFabrica((mixer, formato) -> {
            if ("quebrado".equals(nome(mixer))) {
                throw new LineUnavailableException("sem linha");
            }
            return new DublêsDeAudio.ClipFalso(abertos);
        });

        EfeitosSonoros.tocar(EfeitosSonoros.Som.GOL);
        aguardarCarga();

        assertFalse("não deve declarar o áudio indisponível se um mixer serviu",
                EfeitosSonoros.isAudioIndisponivel());
        assertEquals("só o clip do mixer bom deve ter sido aberto", 1, abertos.size());
    }

    /**
     * Uma linha que abre e aceita {@code start()} mas nunca fica ativa é o
     * sintoma clássico de dispositivo removido pelo Windows. O jogo não pode
     * aceitar essa linha como se estivesse funcionando.
     */
    @Test
    public void linhaQueNaoProduzAudioNaoEDeclaradaComoValida() throws Exception {
        List<Clip> abertos = new ArrayList<>();
        EfeitosSonoros.definirMixers(List.of(mixerFalso("morto")));
        EfeitosSonoros.trocarFabrica((mixer, formato) -> new DublêsDeAudio.ClipFalso(abertos, false));

        EfeitosSonoros.tocar(EfeitosSonoros.Som.PAREDE);
        aguardarCarga();

        assertTrue("uma linha muda não pode ser aceita como áudio válido",
                EfeitosSonoros.isAudioIndisponivel());
        assertTrue("o motivo deve dizer que a linha não produziu áudio",
                EfeitosSonoros.getUltimoErro().contains("sem audio"));
    }

    /**
     * Espera a carga terminar. A abertura de linhas acontece numa thread
     * própria, para não travar a EDT, então o teste precisa dar tempo.
     */
    private static void aguardarCarga() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (!EfeitosSonoros.isCarregando()) {
                return;
            }
            Thread.sleep(20);
        }
        fail("a carga de áudio não terminou em 2 segundos");
    }

    private static String nome(Mixer mixer) {
        return mixer.getMixerInfo().getName();
    }

    private static URL url(EfeitosSonoros.Som som) {
        return EfeitosSonoros.class.getResource(caminhoSom(som));
    }

    private static String caminhoSom(EfeitosSonoros.Som som) {
        return "/sons/" + som.name().toLowerCase() + ".wav";
    }

    private static URL somDentroDeJar() throws Exception {
        return somDentroDeJarDe(EfeitosSonoros.Som.REBATER);
    }

    /**
     * Grava um {@code .wav} do projeto em um jar temporário e devolve uma URL com
     * protocolo {@code jar}, que é o que o jogo vê ao ser executado.
     *
     * <p>Não basta apontar para o arquivo do jar: uma URL {@code file:} seria
     * devolvida como {@code file:} e o teste passaria sem reproduzir o defeito.
     *
     * @param som efeito a colocar no jar
     * @return URL com protocolo {@code jar}
     * @throws Exception se o jar não puder ser escrito
     */
    private static URL somDentroDeJarDe(EfeitosSonoros.Som som) throws Exception {
        byte[] wav;
        try (InputStream in = EfeitosSonoros.class.getResourceAsStream(caminhoSom(som))) {
            assertNotNull("o wav de origem não está no classpath: " + som, in);
            wav = in.readAllBytes();
        }

        java.nio.file.Path jar = java.nio.file.Files.createTempFile("pong-sons", ".jar");
        jar.toFile().deleteOnExit();
        try (JarOutputStream saida = new JarOutputStream(java.nio.file.Files.newOutputStream(jar))) {
            saida.putNextEntry(new ZipEntry(caminhoSom(som).substring(1)));
            saida.write(wav);
            saida.closeEntry();
        }
        return new URL("jar:" + jar.toUri().toURL() + "!" + caminhoSom(som));
    }

    /** Cria um mixer identificável, sem linha real. */
    private static Mixer mixerFalso(String nome) {
        return new DublêsDeAudio.MixerFalso(nome);
    }
}
