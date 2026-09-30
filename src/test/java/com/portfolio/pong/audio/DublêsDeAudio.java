package com.portfolio.pong.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import java.util.List;

/**
 * Dublês de {@link Mixer} e {@link Clip} para testar a escolha de dispositivo de
 * saída e a validação dos clipes sem depender do áudio da máquina de quem roda
 * a suíte.
 *
 * <p>O dublê mais importante é o {@link ClipFalso}: ele abre e aceita
 * {@code start()}, como uma linha presa a um dispositivo que o Windows já
 * removes. Só com esse comportamento é possível testar que o jogo não aceita
 * uma linha que não produz som — que era o defeito original.
 */
final class DublêsDeAudio {

    private DublêsDeAudio() {
    }

    /**
     * Mixer que só existe para ser identificado. Nenhum método abre linha de
     * verdade: os testes injetam a abertura por {@code trocarFabrica}.
     */
    static final class MixerFalso implements Mixer {

        private final String nome;

        MixerFalso(String nome) {
            this.nome = nome;
        }

        @Override
        public Mixer.Info getMixerInfo() {
            return new InfoFalso(nome);
        }

        @Override
        public boolean isLineSupported(Line.Info p0) {
            return true;
        }

        @Override
        public Line getLine(Line.Info p0) {
            throw new UnsupportedOperationException("dublê não abre linhas de verdade");
        }

        @Override
        public int getMaxLines(Line.Info p0) {
            return 0;
        }

        @Override
        public Line.Info getLineInfo() {
            return new Line.Info(null);
        }

        @Override
        public Control getControl(Control.Type control) {
            throw new IllegalArgumentException("sem controles");
        }

        @Override
        public Control[] getControls() {
            return new Control[0];
        }

        @Override
        public boolean isControlSupported(Control.Type control) {
            return false;
        }

        @Override
        public void addLineListener(LineListener listener) {
        }

        @Override
        public void removeLineListener(LineListener listener) {
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public void open() {
            throw new UnsupportedOperationException("dublê não abre linhas de verdade");
        }

        @Override
        public Line.Info[] getSourceLineInfo() {
            return new Line.Info[0];
        }

        @Override
        public Line.Info[] getSourceLineInfo(Line.Info p0) {
            return new Line.Info[0];
        }

        @Override
        public Line[] getSourceLines() {
            return new Line[0];
        }

        @Override
        public Line.Info[] getTargetLineInfo() {
            return new Line.Info[0];
        }

        @Override
        public Line.Info[] getTargetLineInfo(Line.Info p0) {
            return new Line.Info[0];
        }

        @Override
        public Line[] getTargetLines() {
            return new Line[0];
        }

        @Override
        public boolean isSynchronizationSupported(Line[] p0, boolean p1) {
            return false;
        }

        @Override
        public void synchronize(Line[] p0, boolean p1) {
            throw new UnsupportedOperationException("dublê não sincroniza");
        }

        @Override
        public void unsynchronize(Line[] p0) {
            throw new UnsupportedOperationException("dublê não sincroniza");
        }
    }

    /**
     * {@link Mixer.Info} é uma classe abstrata: a única forma de instanciá-la é
     * por subclasse, então é daqui que sai o nome do mixer de teste.
     */
    static final class InfoFalso extends Mixer.Info {

        InfoFalso(String nome) {
            super(nome, "mixer usado nos testes", "JUnit", "1.0");
        }
    }

    /**
     * Clip que aceita ser aberto e tocado sem tocar nada. Se registra na lista
     * recebida para que o teste possa contar quantas linhas foram abertas.
     *
     * <p>Construído com {@code produzAudio = false}, ele representa uma linha
     * presa a um dispositivo removido: abre, aceita {@code start()} e fica
     * inativa sem lançar exceção.
     */
    static final class ClipFalso implements Clip {

        private final List<Clip> registro;
        private final boolean produzAudio;
        private boolean aberto;
        private boolean tocando;

        ClipFalso(List<Clip> registro) {
            this(registro, true);
        }

        ClipFalso(List<Clip> registro, boolean produzAudio) {
            this.registro = registro;
            this.produzAudio = produzAudio;
            registro.add(this);
        }

        @Override
        public void open(AudioInputStream p0) {
            aberto = true;
        }

        @Override
        public void open(AudioFormat p0, byte[] p1, int p2, int p3) {
            aberto = true;
        }

        @Override
        public void open() {
            aberto = true;
        }

        @Override
        public void start() {
            if (!aberto) {
                throw new IllegalStateException("clip fechado");
            }
            tocando = produzAudio;
        }

        @Override
        public void stop() {
            tocando = false;
        }

        @Override
        public void loop(int count) {
            tocando = false;
        }

        @Override
        public boolean isActive() {
            return tocando;
        }

        @Override
        public boolean isRunning() {
            return tocando;
        }

        @Override
        public void close() {
            aberto = false;
            tocando = false;
        }

        @Override
        public boolean isOpen() {
            return aberto;
        }

        @Override
        public AudioFormat getFormat() {
            return new AudioFormat(44100f, 16, 1, true, false);
        }

        @Override
        public void setFramePosition(int frames) {
        }

        @Override
        public void setMicrosecondPosition(long micros) {
        }

        @Override
        public void setLoopPoints(int start, int end) {
        }

        @Override
        public int getFramePosition() {
            return 0;
        }

        @Override
        public long getLongFramePosition() {
            return 0L;
        }

        @Override
        public long getMicrosecondPosition() {
            return 0L;
        }

        @Override
        public int getFrameLength() {
            return 0;
        }

        @Override
        public long getMicrosecondLength() {
            return 0L;
        }

        @Override
        public int getBufferSize() {
            return 0;
        }

        @Override
        public int available() {
            return 0;
        }

        @Override
        public float getLevel() {
            return produzAudio ? 0.5f : 0f;
        }

        @Override
        public void flush() {
        }

        @Override
        public void drain() {
        }

        @Override
        public Line.Info getLineInfo() {
            return new Line.Info(null);
        }

        @Override
        public boolean isControlSupported(Control.Type control) {
            return false;
        }

        @Override
        public Control getControl(Control.Type control) {
            throw new IllegalArgumentException("sem controles");
        }

        @Override
        public Control[] getControls() {
            return new Control[0];
        }

        @Override
        public void addLineListener(LineListener listener) {
        }

        @Override
        public void removeLineListener(LineListener listener) {
        }
    }
}
