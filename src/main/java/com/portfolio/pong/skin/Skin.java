package com.portfolio.pong.skin;

import java.awt.Color;

/**
 * Dados visuais de um tema do Pong: cores do campo, raquetes, bola e opções
 * de acabamento (rótulo na bola e scanlines de CRT).
 *
 * <p>Camada de dados: não desenha nada, apenas descreve como desenhar.</p>
 *
 * @author Eduardo Alvez
 */
public class Skin {

    /** Nome exibido no seletor de skin. */
    public static final String PERSONALIZADA = "Personalizada";

    private final String nome;
    private final Color corCampo;
    private final Color corLinha;
    private final Color corBorda;
    private final Color corRaquete1;
    private final Color corRaquete2;
    private final Color corBola;
    private final Color corQueimado;
    private final String rotuloBola;
    private final boolean scanlines;

    /**
     * @param nome         nome da skin
     * @param corCampo     cor do fundo do campo
     * @param corLinha     cor da linha central e do círculo de centro
     * @param corBorda     cor das bordas (paredes)
     * @param corRaquete1  cor da raquete do jogador 1
     * @param corRaquete2  cor da raquete do jogador 2
     * @param corBola      cor da bola
     * @param corQueimado  cor da marca de queimado deixada pela bola em chamas
     * @param rotuloBola   símbolo/texto desenhado no centro da bola (vazio = nenhum)
     * @param scanlines    se {@code true}, renderiza com linhas de CRT (skin Retrô)
     */
    public Skin(String nome, Color corCampo, Color corLinha, Color corBorda,
                Color corRaquete1, Color corRaquete2, Color corBola,
                Color corQueimado, String rotuloBola, boolean scanlines) {
        this.nome = nome;
        this.corCampo = corCampo;
        this.corLinha = corLinha;
        this.corBorda = corBorda;
        this.corRaquete1 = corRaquete1;
        this.corRaquete2 = corRaquete2;
        this.corBola = corBola;
        this.corQueimado = corQueimado;
        this.rotuloBola = rotuloBola;
        this.scanlines = scanlines;
    }

    public String getNome() {
        return nome;
    }

    public Color getCorCampo() {
        return corCampo;
    }

    public Color getCorLinha() {
        return corLinha;
    }

    public Color getCorBorda() {
        return corBorda;
    }

    public Color getCorRaquete1() {
        return corRaquete1;
    }

    public Color getCorRaquete2() {
        return corRaquete2;
    }

    public Color getCorBola() {
        return corBola;
    }

    public Color getCorQueimado() {
        return corQueimado;
    }

    public String getRotuloBola() {
        return rotuloBola;
    }

    public boolean hasScanlines() {
        return scanlines;
    }
}