package com.portfolio.pong.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Gerencia os efeitos sonoros do jogo. Carrega os arquivos {@code .wav} do
 * classpath de forma preguiçosa (apenas no primeiro uso), guarda-os em memória
 * para reuso e permite silenciar tudo com um único interruptor global.
 *
 * @author Eduardo Alves
 * @author Eduardo Alvez
 */
public final class EfeitosSonoros {

    /** Cada efeito sonoro disponível e o caminho do arquivo no classpath. */
    public enum Som {
        REBATER("/sons/rebater.wav"),
        PAREDE("/sons/parede.wav"),
        GOL("/sons/gol.wav"),
        ESPECIAL("/sons/especial.wav"),
        PREMIO("/sons/premio.wav"),
        CLIQUE("/sons/clique.wav"),
        VITORIA("/sons/vitoria.wav");

        Som(String caminho) {
            this.caminho = caminho;
        }

        private final String caminho;
    }

    /** Cache dos clipes já carregados, indexados pelo efeito. */
    private static final Map<Som, Clip> CLIPS = new HashMap<Som, Clip>();

    /** Interruptor global de som (ligado por padrão). */
    private static boolean ligado = true;

    /** Construtor privado: classe de utilitários, não deve ser instanciada. */
    private EfeitosSonoros() {
    }

    /** @return {@code true} se o som está ligado no momento */
    public static boolean isLigado() {
        return ligado;
    }

    /**
     * Liga ou desliga todos os efeitos sonoros.
     *
     * @param ativo {@code true} para ativar o som, {@code false} para silenciar
     */
    public static void setLigado(boolean ativo) {
        ligado = ativo;
    }

    /**
     * Toca um efeito sonoro. Se o clip ainda não foi carregado, ele é lido do
     * classpath na primeira chamada e mantido em cache para as próximas. Som
     * desligado ou clip com problema fazem a chamada ser ignorada.
     *
     * @param som o efeito sonoro a ser tocado
     */
    public static void tocar(Som som) {
        if (!ligado) {
            return;
        }
        Clip clip = CLIPS.get(som);
        if (clip == null) {
            clip = carregar(som.caminho);
            if (clip == null) {
                return;
            }
            CLIPS.put(som, clip);
        }
        clip.setFramePosition(0);
        clip.start();
    }

    /**
     * Lê e prepara um clip de áudio a partir do recurso no classpath. O ganho é
     * reduzido para −9 dB para que os sons não soem estridentes.
     *
     * @param caminho caminho do recurso dentro de {@code /sons/}
     * @return clip pronto para tocar, ou {@code null} se não for possível carregar
     */
    private static Clip carregar(String caminho) {
        try {
            URL url = EfeitosSonoros.class.getResource(caminho);
            if (url == null) {
                return null;
            }
            AudioInputStream fluxo = AudioSystem.getAudioInputStream(url.openStream());
            Clip clip = AudioSystem.getClip();
            clip.open(fluxo);
            try {
                FloatControl ganho = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                ganho.setValue(-9.0f);
            } catch (Exception ignorada) {
                // Ganho indisponível não impede o som de tocar.
            }
            return clip;
        } catch (Exception ignorada) {
            return null;
        }
    }
}