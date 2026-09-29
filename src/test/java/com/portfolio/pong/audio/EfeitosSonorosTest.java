package com.portfolio.pong.audio;

import org.junit.After;
import org.junit.Test;

import javax.sound.sampled.AudioSystem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes dos efeitos sonoros: interruptor global e arquivos {@code .wav}
 * presentes e decodificáveis pelo classpath (são recursos do jar).
 */
public class EfeitosSonorosTest {

    @After
    public void restaurarSom() {
        EfeitosSonoros.setLigado(true);
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
            try (var in = AudioSystem.getAudioInputStream(
                    EfeitosSonoros.class.getResource(caminhoSom(som)).openStream())) {
                assertTrue("formato inválido para " + som, in.getFrameLength() > 0);
            }
        }
    }

    private static String caminhoSom(EfeitosSonoros.Som som) {
        return "/sons/" + som.name().toLowerCase() + ".wav";
    }
}