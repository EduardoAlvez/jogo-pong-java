package com.portfolio.pong.ui;

import com.portfolio.pong.audio.EfeitosSonoros;
import com.portfolio.pong.core.Bola;
import com.portfolio.pong.core.Computador;
import com.portfolio.pong.core.Cronometro;
import com.portfolio.pong.core.Pong;
import com.portfolio.pong.core.Raquete;
import com.portfolio.pong.fx.Animacoes;
import com.portfolio.pong.fx.Particula;
import com.portfolio.pong.skin.CatalogoSkins;
import com.portfolio.pong.skin.Skin;

import javax.swing.AbstractAction;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface gráfica (Swing) do Pong: desenha o campo, o placar estilo
 * futebol, as animações da camada {@code fx}, o briefing inicial, a pausa e
 * a tela final com estatísticas.
 *
 * <p>O campo interno é fixo em {@value #FW}×{@value #FH} e é escalado
 * (letterbox) para caber no painel — o jogo continua redimensionável sem
 * alterar a física.</p>
 *
 * @author Eduardo Alvez
 */
public final class TelaPong {

    /** Largura fixa do campo interno (pixels). */
    public static final int FW = 800;

    /** Altura fixa do campo interno (pixels). */
    public static final int FH = 500;

    /** Altura da faixa do placar (pixels, em espaço do painel). */
    private static final int HUD_ALTURA = 66;

    /** Período do loop de jogo (ms) — ~60 FPS com passo fixo de {@value #DT}. */
    private static final long TICK_MS = 16;

    /** Passo de tempo fixo da simulação (segundos por tick). */
    private static final double DT = TICK_MS / 1000.0;

    private enum Fase { BRIEFING, CONTAGEM, JOGANDO, PAUSA, FIM }

    /** Botão desenhado manualmente (utilizado nos overlays e no briefing). */
    private static final class Botao {
        final String texto;
        final int x, y, w, h;
        final Runnable acao;
        boolean selecionado;
        boolean hover;
        boolean ativo = true;

        Botao(String texto, int x, int y, int w, int h, Runnable acao) {
            this.texto = texto;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.acao = acao;
        }

        boolean contem(int px, int py) {
            return ativo && px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    private final CatalogoSkins catalogo = new CatalogoSkins();
    private final List<Botao> botoes = new ArrayList<>();
    private final PongTela tabuleiro = new PongTela();
    private final Timer loop;

    private Pong pong;
    private Animacoes animacoes = new Animacoes();
    private Fase fase = Fase.BRIEFING;

    private boolean cima1;
    private boolean baixo1;
    private boolean cima2;
    private boolean baixo2;

    private double duracaoPartida;
    private double contagemRestante;
    private boolean golBanner;
    private double golBannerTempo;
    private int ultimoP1;
    private int ultimoP2;
    private boolean somLigado = true;

    // Configuração do briefing.
    private boolean doisJogadores;
    private Computador.Dificuldade dificuldade = Computador.Dificuldade.MEDIO;
    private Pong.Modo modo = Pong.Modo.TEMPO;
    private int minutos = 2;
    private int alvo = 7;
    private String skinsNome = "Clássico";

    private Botao somBotao;

    public TelaPong() {
        // Loop com passo fixo; o Timer do Swing mantém o repaint em ~60 FPS.
        loop = new Timer((int) TICK_MS, e -> tick());
        pong = null;

        tabuleiro.setPreferredSize(new Dimension(FW + 80, FH + HUD_ALTURA + 60));
        tabuleiro.configurarTeclado();
        tabuleiro.configurarMouse();
        tabuleiro.setBackground(new Color(0x0B0E14));
    }

    private JPanel getPainel() {
        return tabuleiro;
    }

    private static void som(EfeitosSonoros.Som s) {
        EfeitosSonoros.tocar(s);
    }

    // ---------------------------------------------------------------- Loop

    private void tick() {
        animacoes.atualizar(DT);

        switch (fase) {
            case BRIEFING:
            case PAUSA:
            case FIM:
                break;

            case CONTAGEM:
                contagemRestante -= DT;
                if (contagemRestante <= 0) {
                    fase = Fase.JOGANDO;
                    ultimoP1 = pong.getPontosEsquerda();
                    ultimoP2 = pong.getPontosDireita();
                }
                break;

            case JOGANDO:
                duracaoPartida += DT;
                moverJogadores();
                pong.atualizar(DT);

                if (pong.bateuParedeNoFrame()) {
                    som(EfeitosSonoros.Som.PAREDE);
                }
                if (pong.bateuRaqueteNoFrame()) {
                    som(EfeitosSonoros.Som.REBATER);
                }

                int p1 = pong.getPontosEsquerda();
                int p2 = pong.getPontosDireita();
                if (p1 != ultimoP1 || p2 != ultimoP2) {
                    som(EfeitosSonoros.Som.GOL);
                    golBanner = true;
                    golBannerTempo = 1.0;
                    animacoes.explosaoGol(pong.getBola().getX(), pong.getBola().getY());
                    ultimoP1 = p1;
                    ultimoP2 = p2;
                }

                if (pong.isBolaEmChamas()) {
                    Bola bola = pong.getBola();
                    animacoes.emitirFogo(bola.getX(), bola.getY());
                }

                if (pong.isEncerrado()) {
                    som(EfeitosSonoros.Som.VITORIA);
                    animacoes.confete(FW, new Color(0xFFD700), new Color(0xFF7F27),
                            skin().getCorBola(), skin().getCorRaquete1(), skin().getCorRaquete2());
                    fase = Fase.FIM;
                }

                if (golBanner) {
                    golBannerTempo -= DT;
                    if (golBannerTempo <= 0) {
                        golBanner = false;
                    }
                }
                break;
        }

        tabuleiro.repaint();
    }

    private void moverJogadores() {
        if (cima1 || baixo1) {
            pong.moverJogador(1, cima1 ? -1 : 1, DT);
        }
        if (cima2 || baixo2) {
            pong.moverJogador(2, cima2 ? -1 : 1, DT);
        }
    }

    // ------------------------------------------------------------ Ações

    private void iniciarPartida() {
        pong = new Pong(FW, FH);
        pong.configurar(modo, dificuldade, doisJogadores, alvo, minutos * 60);
        animacoes = new Animacoes();
        duracaoPartida = 0;
        ultimoP1 = 0;
        ultimoP2 = 0;
        golBanner = false;
        fase = Fase.CONTAGEM;
        contagemRestante = 3.75;
    }

    private void voltarAoBriefing() {
        som(EfeitosSonoros.Som.CLIQUE);
        pong = null;
        animacoes = new Animacoes();
        fase = Fase.BRIEFING;
    }

    private void alternarPausa() {
        if (fase == Fase.JOGANDO) {
            fase = Fase.PAUSA;
        } else if (fase == Fase.PAUSA) {
            fase = Fase.JOGANDO;
        }
    }

    private void usarEspecial(int jogador) {
        if (fase != Fase.JOGANDO || pong == null) {
            return;
        }
        // Em 1P vs CPU, o especial do jogador 2 pertence à raquete do computador.
        if (jogador == 2 && !pong.isDoisJogadores()) {
            return;
        }
        if (pong.usarEspecial(jogador)) {
            som(EfeitosSonoros.Som.ESPECIAL);
        }
    }

    private Skin skin() {
        return catalogo.obterSelecionada();
    }

    private void personalizarSkin() {
        Skin atual = skin();
        Color campo = escolherCor("Cor do campo", atual.getCorCampo());
        if (campo == null) {
            return;
        }
        Color linha = escolherCor("Cor da linha central", atual.getCorLinha());
        if (linha == null) {
            return;
        }
        Color r1 = escolherCor("Cor da raquete do jogador 1", atual.getCorRaquete1());
        if (r1 == null) {
            return;
        }
        Color r2 = escolherCor("Cor da raquete do jogador 2", atual.getCorRaquete2());
        if (r2 == null) {
            return;
        }
        Color bola = escolherCor("Cor da bola", atual.getCorBola());
        if (bola == null) {
            return;
        }
        String rotulo = (String) JOptionPane.showInputDialog(
                tabuleiro, "Símbolo/letra desenhado no centro da bola (opcional):",
                "Bola personalizada", JOptionPane.PLAIN_MESSAGE, null, null, atual.getRotuloBola());
        if (rotulo != null) {
            catalogo.aplicarPersonalizada(campo, linha, atual.getCorBorda(), r1, r2, bola, rotulo);
            skinsNome = Skin.PERSONALIZADA;
            som(EfeitosSonoros.Som.CLIQUE);
        }
    }

    private Color escolherCor(String titulo, Color inicial) {
        return JColorChooser.showDialog(tabuleiro, titulo, inicial);
    }

    // -------------------------------------------------------------- Painel

    private final class PongTela extends JPanel {

        PongTela() {
            setOpaque(true);
        }

        void configurarTeclado() {
            bind("p1-cima", KeyEvent.VK_W, () -> cima1 = true, () -> cima1 = false);
            bind("p1-baixo", KeyEvent.VK_S, () -> baixo1 = true, () -> baixo1 = false);
            bind("p2-cima", KeyEvent.VK_UP, () -> cima2 = true, () -> cima2 = false);
            bind("p2-baixo", KeyEvent.VK_DOWN, () -> baixo2 = true, () -> baixo2 = false);
            bind("especial1", KeyEvent.VK_Z, () -> usarEspecial(1), () -> { });
            bind("especial2", KeyEvent.VK_M, () -> usarEspecial(2), () -> { });
            bind("pausa", KeyEvent.VK_SPACE, () -> alternarPausa(), () -> { });
            bind("sair", KeyEvent.VK_ESCAPE, this::sairContextual, () -> { });
        }

        private void sairContextual() {
            if (fase == Fase.PAUSA || fase == Fase.FIM) {
                voltarAoBriefing();
            } else if (fase == Fase.JOGANDO) {
                fase = Fase.PAUSA;
            }
        }

        private void bind(String nome, int tecla, Runnable naPressao, Runnable naLiberacao) {
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(tecla, 0, false), nome);
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(tecla, 0, true), nome + "-solto");
            getActionMap().put(nome, new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    naPressao.run();
                }
            });
            getActionMap().put(nome + "-solto", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    naLiberacao.run();
                }
            });
        }

        void configurarMouse() {
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    ativarHover(e.getPoint().x, e.getPoint().y);
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    clicar(e.getPoint().x, e.getPoint().y);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            botoes.clear();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            desenharFundo(g2);

            if (fase != Fase.BRIEFING && pong != null) {
                desenharJogo(g2);
            }
            if (fase == Fase.BRIEFING) {
                desenharBriefing(g2);
            }
            if (fase == Fase.PAUSA) {
                desenharOverlay(g2, "PAUSADO", new String[]{"Retomar", "Reiniciar", "Início"},
                        new Runnable[]{ () -> alternarPausa(), () -> iniciarPartida(), () -> voltarAoBriefing() });
            }
            if (fase == Fase.FIM) {
                desenharFim(g2);
            }
            g2.dispose();
        }
    }

    // --------------------------------------------------------- Utilitários

    private void desenharFundo(Graphics2D g2) {
        int w = tabuleiro.getWidth();
        int h = tabuleiro.getHeight();
        Color topo = new Color(0x0B0E14);
        Color base = new Color(0x161C28);
        for (int y = 0; y < h; y++) {
            double t = (double) y / Math.max(1, h);
            g2.setColor(misturar(topo, base, t));
            g2.drawLine(0, y, w, y);
        }
    }

    private static Color misturar(Color a, Color b, double t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int gr = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, gr, bl);
    }

    /** Coordenadas/layout do campo mapeado para o painel (letterbox). */
    private double escala;
    private int ox;
    private int oy;

    private void mapearCampo() {
        int w = tabuleiro.getWidth();
        int h = tabuleiro.getHeight();
        double areaH = Math.max(1, h - HUD_ALTURA);
        escala = Math.min((double) w / FW, areaH / FH);
        ox = (int) ((w - FW * escala) / 2.0);
        oy = HUD_ALTURA + (int) ((areaH - FH * escala) / 2.0);
    }

    private int fx(double v) {
        return ox + (int) (v * escala);
    }

    private int fy(double v) {
        return oy + (int) (v * escala);
    }

    private double ex(double v) {
        return (v - ox) / escala;
    }

    private double ey(double v) {
        return (v - oy) / escala;
    }

    private void desenharJogo(Graphics2D g2) {
        mapearCampo();
        double tremoX = 0;
        double tremoY = 0;
        double tremolo = animacoes.getTremor();
        if (tremolo > 0.1) {
            tremoX = (Math.random() * 2 - 1) * tremolo;
            tremoY = (Math.random() * 2 - 1) * tremolo;
        }
        g2.translate(fx(0) + tremoX, fy(0) + tremoY);
        g2.scale(escala, escala);

        Skin s = skin();
        // gramado + borda
        g2.setColor(s.getCorCampo());
        g2.fillRect(0, 0, FW, FH);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(s.getCorBorda());
        g2.drawRect(0, 0, FW, FH);

        // marcas de queimado
        g2.setColor(new Color(s.getCorQueimado().getRed(), s.getCorQueimado().getGreen(),
                s.getCorQueimado().getBlue(), 160));
        for (Animacoes.MarcaQueimado m : animacoes.getMarcas()) {
            g2.fillOval((int) (m.getX() - m.getRaio()), (int) (m.getY() - m.getRaio()),
                    (int) (m.getRaio() * 2), (int) (m.getRaio() * 2));
        }

        // linhas do campo estilo futebol
        float alfaLinha = 0.55f;
        g2.setColor(new Color(s.getCorLinha().getRed(), s.getCorLinha().getGreen(),
                s.getCorLinha().getBlue(), (int) (255 * alfaLinha)));
        g2.setStroke(new BasicStroke(3f));
        desenharLinhaTracejada(g2, FW / 2.0, 8, FW / 2.0, FH - 8, 12, 12);
        g2.drawOval(FW / 2 - 62, FH / 2 - 62, 124, 124);
        g2.drawOval(FW / 2 - 8, FH / 2 - 8, 16, 16);

        // áreas de gol
        int areaGol = 70;
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(0, 0, areaGol, FH);
        g2.drawRect(FW - areaGol, 0, areaGol, FH);

        // raquetes (com brilho quando o especial está ativo)
        desenharRaquete(g2, pong.getRaqueteEsquerda(), s.getCorRaquete1(), pong.isEspecial1Ativo());
        desenharRaquete(g2, pong.getRaqueteDireita(), s.getCorRaquete2(), pong.isEspecial2Ativo());

        // partículas (abaixo da bola)
        for (Particula p : animacoes.getParticulas()) {
            int alpha = (int) (255 * p.getAlpha());
            Color cor = p.getCor();
            g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), alpha));
            g2.fillOval((int) (p.getX() - p.getRaio()), (int) (p.getY() - p.getRaio()),
                    (int) (p.getRaio() * 2), (int) (p.getRaio() * 2));
        }

        desenharBola(g2);

        // rótulo central (gol de ouro)
        if (pong.isGolDeOuro()) {
            String txt = "GOL DE OURO!";
            g2.setFont(new Font("Arial", Font.BOLD, 22));
            Color cinto = new Color(0xFFD700);
            int t = (int) (System.currentTimeMillis() / 120) % 2;
            g2.setColor(t == 0 ? cinto : Color.WHITE);
            int larg = g2.getFontMetrics().stringWidth(txt);
            g2.drawString(txt, (FW - larg) / 2, FH / 2 + 66);
        }

        g2.scale(1 / escala, 1 / escala);
        g2.translate(-(fx(0) + tremoX), -(fy(0) + tremoY));

        desenharHud(g2);

        if (fase == Fase.CONTAGEM) {
            desenharContagem(g2);
        }
        if (golBanner && fase == Fase.JOGANDO) {
            desenharGol(g2);
        }
    }

    private void desenharLinhaTracejada(Graphics2D g2, double x1, double y1, double x2, double y2,
                                        double traco, double vazio) {
        Stroke original = g2.getStroke();
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                new float[]{(float) traco, (float) vazio}, 0f));
        g2.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        g2.setStroke(original);
    }

    private void desenharRaquete(Graphics2D g2, Raquete r, Color cor, boolean especial) {
        if (especial) {
            g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 90));
            g2.fillRoundRect(r.getX() - 6, r.getY() - 6, Raquete.LARGURA + 12,
                    Raquete.ALTURA + 12, 16, 16);
        }
        g2.setColor(cor);
        g2.fillRoundRect(r.getX(), r.getY(), Raquete.LARGURA, Raquete.ALTURA, 10, 10);
    }

    private void desenharBola(Graphics2D g2) {
        Bola bola = pong.getBola();
        double r = bola.getRaio();
        int cx = (int) bola.getX();
        int cy = (int) bola.getY();
        Skin s = skin();

        if (pong.isBolaEmChamas()) {
            // aura de fogo + núcleo
            int aura = (int) (r * 4);
            for (int i = aura; i > 0; i -= 2) {
                double t = (double) i / aura;
                int alpha = (int) (70 * t);
                g2.setColor(new Color(0xFF, 0x7F, 0x27, alpha));
                g2.fillOval(cx - i, cy - i, i * 2, i * 2);
            }
            double pulso = 1 + 0.25 * Math.sin(System.currentTimeMillis() / 90.0);
            g2.setColor(new Color(0xFFD700));
            g2.fillOval(cx - (int) (r * pulso), cy - (int) (r * pulso),
                    (int) (2 * r * pulso), (int) (2 * r * pulso));
        } else {
            g2.setColor(s.getCorBola());
            g2.fillOval(cx - (int) r, cy - (int) r, (int) (2 * r), (int) (2 * r));
        }

        if (s.getRotuloBola() != null && !s.getRotuloBola().isBlank() && !pong.isBolaEmChamas()) {
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.setColor(new Color(0, 0, 0, 130));
            int larg = g2.getFontMetrics().stringWidth(s.getRotuloBola());
            g2.drawString(s.getRotuloBola(), cx - larg / 2, cy + 4);
        }
    }

    private void desenharHud(Graphics2D g2) {
        int w = tabuleiro.getWidth();
        int cx = w / 2;

        String esquerda = "P1";
        String direita = pong.isDoisJogadores() ? "P2" : "CPU";
        String placar = pong.getPontosEsquerda() + " : " + pong.getPontosDireita();
        String relogio = pong.getModo() == Pong.Modo.TEMPO
                ? Cronometro.formatar(pong.getCronometro().getRestanteSegundos())
                : "";

        g2.setColor(new Color(255, 255, 255, 40));
        int boxW = 320;
        g2.fillRoundRect(cx - boxW / 2, 6, boxW, HUD_ALTURA - 12, 18, 18);

        // rótulos dos jogadores
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.setColor(new Color(255, 255, 255, 200));
        g2.drawString(esquerda, cx - boxW / 2 + 22, 36);
        g2.drawString(direita, cx + boxW / 2 - 22 - g2.getFontMetrics().stringWidth(direita), 36);

        // placar
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        g2.setColor(Color.WHITE);
        int largPlacar = g2.getFontMetrics().stringWidth(placar);
        g2.drawString(placar, cx - largPlacar / 2, 40);

        // relógio (vermelho piscando nos últimos 10s)
        boolean ultimos = pong.getModo() == Pong.Modo.TEMPO
                && pong.getCronometro().getRestanteSegundos() <= 10;
        boolean pisca = ultimos && (System.currentTimeMillis() / 400) % 2 == 0;
        if (pong.getModo() == Pong.Modo.TEMPO) {
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            Color corRelogio = ultimos ? (pisca ? new Color(0xFF3B30) : new Color(0xFF8A80))
                    : new Color(255, 255, 255, 200);
            g2.setColor(corRelogio);
            int largRelogio = g2.getFontMetrics().stringWidth(relogio);
            g2.drawString(relogio, cx - largRelogio / 2, HUD_ALTURA - 12);
        } else {
            g2.setFont(new Font("Arial", Font.PLAIN, 13));
            g2.setColor(new Color(255, 255, 255, 170));
            String alvoTxt = "primeiro a " + pong.getAlvoPontos();
            int largAlvo = g2.getFontMetrics().stringWidth(alvoTxt);
            g2.drawString(alvoTxt, cx - largAlvo / 2, HUD_ALTURA - 12);
        }

        desenharIndicadoresEspecial(g2, cx);
    }

    private void desenharIndicadoresEspecial(Graphics2D g2, int cx) {
        Font f = new Font("Arial", Font.BOLD, 13);
        g2.setFont(f);
        posicaoIndicador(g2, cx, 1, true);
        posicaoIndicador(g2, cx, 2, pong.isDoisJogadores());
    }

    private void posicaoIndicador(Graphics2D g2, int cx, int jogador, boolean visivel) {
        boolean disponivel = jogador == 1 ? pong.isEspecial1Disponivel() : pong.isEspecial2Disponivel();
        boolean ativo = jogador == 1 ? pong.isEspecial1Ativo() : pong.isEspecial2Ativo();
        if (!visivel) {
            return;
        }
        String texto = "Z: 🔥" + (ativo ? " " + String.format("%.1f", pong.getTempoEspecialRestante()) : "");
        if (jogador == 2) {
            texto = "M: 🔥" + (ativo ? " " + String.format("%.1f", pong.getTempoEspecialRestante()) : "");
        }
        int larg = g2.getFontMetrics().stringWidth(texto) + 14;
        int x = jogador == 1 ? cx - 320 + 8 : cx + 320 - 8 - larg;
        int y = 6 + HUD_ALTURA - 24;
        Color cor = ativo ? new Color(0xFF7F27) : (disponivel ? new Color(255, 255, 255, 200)
                : new Color(255, 255, 255, 70));
        g2.setColor(new Color(255, 255, 255, 25));
        g2.fillRoundRect(x, y, larg, 22, 11, 11);
        g2.setColor(cor);
        g2.drawString(texto, x + 7, y + 15);
    }

    private void desenharContagem(Graphics2D g2) {
        desenharTextoCentro(g2, numeroContagem(), new Color(0xFFD700), FW * 0.34);
    }

    private String numeroContagem() {
        if (contagemRestante > 2.0) {
            return "3";
        }
        if (contagemRestante > 1.0) {
            return "2";
        }
        return contagemRestante > 0.9 ? "1" : "JÁ!";
    }

    private void desenharGol(Graphics2D g2) {
        double fracao = Math.min(1.0, golBannerTempo / 0.35);
        double escalaBanner = 1 + (1 - fracao) * 1.3;
        g2.translate(fx(FW / 2.0), fy(FH / 2.0));
        g2.scale(escalaBanner, escalaBanner);
        g2.setFont(new Font("Arial", Font.BOLD, (int) (FW * 0.16)));
        g2.setColor(new Color(0xFFD700));
        String txt = "GOL!";
        int larg = g2.getFontMetrics().stringWidth(txt);
        g2.drawString(txt, -larg / 2, (int) (FW * 0.05));
        g2.scale(1 / escalaBanner, 1 / escalaBanner);
        g2.translate(-fx(FW / 2.0), -fy(FH / 2.0));
    }

    private void desenharTextoCentro(Graphics2D g2, String texto, Color cor, double tamanho) {
        g2.setFont(new Font("Arial", Font.BOLD, (int) tamanho));
        g2.setColor(cor);
        int larg = g2.getFontMetrics().stringWidth(texto);
        g2.drawString(texto, fx(FW / 2.0) - larg / 2, fy(FH / 2.0) + (int) (tamanho * 0.35));
    }

    // ----------------------------------------------------------- Briefing

    private void desenharBriefing(Graphics2D g2) {
        int w = tabuleiro.getWidth();
        int h = tabuleiro.getHeight();

        // título
        g2.setFont(new Font("Arial", Font.BOLD, 54));
        g2.setColor(Color.WHITE);
        String titulo = "PONG";
        int titW = g2.getFontMetrics().stringWidth(titulo);
        g2.drawString(titulo, (w - titW) / 2, 72);

        // aura decorativa
        int centroX = w / 2;
        int centroY = h / 2 + 20;
        int raioDecor = Math.min(w, h) / 3;
        dibujaAura(g2, centroX, centroY, raioDecor, skin().getCorRaquete1());

        // card "glassmorphism" central
        int cardW = Math.min(w - 40, 560);
        int cardH = Math.min(h - 130, 430);
        int cardX = (w - cardW) / 2;
        int cardY = 96;
        g2.setColor(new Color(255, 255, 255, 26));
        g2.fill(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 24, 24));
        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(new Color(255, 255, 255, 90));
        g2.draw(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 24, 24));

        int pad = 26;
        int rotuloY = cardY + 30;

        // modo
        int colsModo = 2;
        String[] modos = {"Tempo", "Clássico"};
        int[] modosX = espacar(cardX + pad, cardW - pad * 2, colsModo, 8);
        rotuloY = secaoRotulo(g2, rotuloY, cardX + pad, "Modo");
        for (int i = 0; i < modos.length; i++) {
            final int idx = i;
            int x = modosX[i];
            int y = rotuloY + 12;
            boolean sel = (modo == Pong.Modo.TEMPO) == (i == 0);
            piscar(g2, modos[i], x, y, (cardW - pad * 2) / colsModo - 8, 26, sel,
                    () -> {
                        modo = idx == 0 ? Pong.Modo.TEMPO : Pong.Modo.CLASSICO;
                        som(EfeitosSonoros.Som.CLIQUE);
                    });
        }
        rotuloY += 58;

        // jogadores / dificuldade
        if (!doisJogadores) {
            rotuloY = secaoRotulo(g2, rotuloY, cardX + pad, "Dificuldade do computador");
            String[] niveis = {"Fácil", "Médio", "Difícil"};
            int[] xs = espacar(cardX + pad, cardW - pad * 2, niveis.length, 8);
            for (int i = 0; i < niveis.length; i++) {
                final int idx = i;
                int x = xs[i];
                boolean sel = dificuldade == Computador.Dificuldade.values()[idx];
                piscar(g2, niveis[i], x, rotuloY + 12, 120, 26, sel,
                        () -> {
                            dificuldade = Computador.Dificuldade.values()[idx];
                            doisJogadores = false;
                            som(EfeitosSonoros.Som.CLIQUE);
                        });
            }
            rotuloY += 58;
        }

        // segundo seletor: tempo ou alvo
        if (modo == Pong.Modo.TEMPO) {
            rotuloY = secaoRotulo(g2, rotuloY, cardX + pad, "Duração");
            String[] tempos = {"1 min", "2 min", "3 min"};
            int[] xs = espacar(cardX + pad, cardW - pad * 2, tempos.length, 8);
            for (int i = 0; i < tempos.length; i++) {
                final int idx = i;
                int x = xs[i];
                boolean sel = minutos == (idx + 1);
                piscar(g2, tempos[i], x, rotuloY + 12, 120, 26, sel,
                        () -> {
                            minutos = idx + 1;
                            som(EfeitosSonoros.Som.CLIQUE);
                        });
            }
            rotuloY += 58;
        } else {
            rotuloY = secaoRotulo(g2, rotuloY, cardX + pad, "Quem chegar primeiro a");
            String[] alvos = {"5", "7", "10"};
            int[] xs = espacar(cardX + pad, cardW - pad * 2, alvos.length, 8);
            for (int i = 0; i < alvos.length; i++) {
                final int idx = i;
                int x = xs[i];
                boolean sel = alvo == new int[]{5, 7, 10}[idx];
                piscar(g2, alvos[i], x, rotuloY + 12, 120, 26, sel,
                        () -> {
                            alvo = new int[]{5, 7, 10}[idx];
                            som(EfeitosSonoros.Som.CLIQUE);
                        });
            }
            rotuloY += 58;
        }

        // 2P local: torna o seletor de dificuldade/direto em botão 2P
        int y2p = rotuloY + 4;
        boolean sel2p = doisJogadores;
        piscar(g2, "2 jogadores locais", cardX + pad, y2p, 190, 26, sel2p,
                () -> {
                    doisJogadores = !doisJogadores;
                    som(EfeitosSonoros.Som.CLIQUE);
                });
        rotuloY += 50;

        // skins
        rotuloY = secaoRotulo(g2, rotuloY, cardX + pad, "Skin");
        String[] nomes = catalogo.getNomes();
        desenharCombo(g2, nomes, cardX + pad, rotuloY + 4, 170, 26, skinsNome,
                delta -> cicloSkin(nomes, delta));
        piscar(g2, "Personalizar...", cardX + pad + 180, rotuloY + 4, 130, 26, false,
                this::personalizarSkin);
        rotuloY += 62;

        // som + jogar
        somLigado = EfeitosSonoros.isLigado();
        piscar(g2, somLigado ? "🔊 Som" : "🔇 Som", cardX + pad, rotuloY + 4, 110, 26, somLigado,
                () -> {
                    somLigado = !somLigado;
                    EfeitosSonoros.setLigado(somLigado);
                });
        Botao jogar = piscar(g2, "JOGAR ▶", cardX + cardW - pad - 150, rotuloY + 4, 150, 34, false,
                () -> {
                    som(EfeitosSonoros.Som.CLIQUE);
                    catalogo.selecionar(skinsNome);
                    iniciarPartida();
                });
        jogar.selecionado = true;
        jogar.ativo = true;

        rotuloY += 58;
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(255, 255, 255, 150));
        String controles = "W/S — Jogador 1 · ↑/↓ — Jogador 2 · Z/M — Bola de fogo · Espaço — Pausa";
        int cw = g2.getFontMetrics().stringWidth(controles);
        g2.drawString(controles, (w - cw) / 2, h - 18);
    }

    private int secaoRotulo(Graphics2D g2, int y, int x, String texto) {
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(new Color(255, 255, 255, 200));
        g2.drawString(texto, x, y);
        return y + 8;
    }

    private int[] espacar(int x, int largura, int n, int folga) {
        int[] xs = new int[n];
        int passo = (largura - folga * (n - 1)) / n;
        for (int i = 0; i < n; i++) {
            xs[i] = x + i * (passo + folga);
        }
        return xs;
    }

    private interface ComboCiclo {
        void ir(int delta);
    }

    /** Seletor de skin: nome centralizado entre setas ◀ ▶ clicáveis. */
    private void desenharCombo(Graphics2D g2, String[] nomes, int x, int y, int w, int h,
                               String atual, ComboCiclo aoMudar) {
        g2.setColor(new Color(255, 255, 255, 32));
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(new Color(255, 255, 255, 100));
        g2.drawRoundRect(x, y, w, h, 12, 12);

        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.setColor(Color.WHITE);
        int larg = g2.getFontMetrics().stringWidth(atual);
        g2.drawString(atual, x + (w - larg) / 2, y + h / 2 + 5);

        novoBotao("◀", x, y, 26, h, () -> aoMudar.ir(-1)).selecionado = false;
        novoBotao("▶", x + w - 26, y, 26, h, () -> aoMudar.ir(+1)).selecionado = false;
        desenharBotao(g2, botoes.get(botoes.size() - 2));
        desenharBotao(g2, botoes.get(botoes.size() - 1));
    }

    private Botao piscar(Graphics2D g2, String texto, int x, int y, int w, int h, boolean selecionado,
                         Runnable acao) {
        Botao b = novoBotao(texto, x, y, w, h, acao);
        b.selecionado = selecionado;
        desenharBotao(g2, b);
        return b;
    }

    private Botao novoBotao(String texto, int x, int y, int w, int h, Runnable acao) {
        Botao b = new Botao(texto, x, y, w, h, acao);
        botoes.add(b);
        return b;
    }

    private void desenharBotao(Graphics2D g2, Botao b) {
        Color fundo = b.selecionado ? new Color(0xFFD700)
                : (b.hover ? new Color(255, 255, 255, 70) : new Color(255, 255, 255, 32));
        g2.setColor(fundo);
        g2.fillRoundRect(b.x, b.y, b.w, b.h, 12, 12);
        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(b.selecionado ? new Color(0xFFD700) : new Color(255, 255, 255, 100));
        g2.drawRoundRect(b.x, b.y, b.w, b.h, 12, 12);

        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.setColor(b.selecionado ? new Color(0x161C28) : Color.WHITE);
        int larg = g2.getFontMetrics().stringWidth(b.texto);
        g2.drawString(b.texto, b.x + (b.w - larg) / 2, b.y + b.h / 2 + 5);
    }

    private void cicloSkin(String[] nomes, int delta) {
        int i = java.util.Arrays.asList(nomes).indexOf(skinsNome);
        i = (i + delta + nomes.length) % nomes.length;
        skinsNome = nomes[i];
        catalogo.selecionar(skinsNome);
        som(EfeitosSonoros.Som.CLIQUE);
    }

    private void desenharOverlay(Graphics2D g2, String titulo, String[] textos, Runnable[] acoes) {
        int w = tabuleiro.getWidth();
        int h = tabuleiro.getHeight();
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, 0, w, h);

        int cardW = 320;
        int cardH = 210;
        int cardX = (w - cardW) / 2;
        int cardY = (h - cardH) / 2;
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fill(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 22, 22));
        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(new Color(255, 255, 255, 110));
        g2.draw(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 22, 22));

        g2.setFont(new Font("Arial", Font.BOLD, 26));
        g2.setColor(Color.WHITE);
        int largT = g2.getFontMetrics().stringWidth(titulo);
        g2.drawString(titulo, cardX + (cardW - largT) / 2, cardY + 44);

        int y = cardY + 68;
        for (int i = 0; i < textos.length && i < acoes.length; i++) {
            Botao b = novoBotao(textos[i], cardX + 40, y, cardW - 80, 32, acoes[i]);
            b.selecionado = i == 0;
            desenharBotao(g2, b);
            y += 42;
        }
    }

    private void desenharFim(Graphics2D g2) {
        int w = tabuleiro.getWidth();
        int h = tabuleiro.getHeight();
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, w, h);

        int cardW = Math.min(w - 60, 430);
        int cardH = Math.min(h - 80, 330);
        int cardX = (w - cardW) / 2;
        int cardY = Math.max(30, (h - cardH) / 2);
        g2.setColor(new Color(255, 255, 255, 32));
        g2.fill(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 24, 24));
        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(new Color(255, 255, 255, 110));
        g2.draw(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 24, 24));

        String vencedor = vencedorTexto();
        g2.setFont(new Font("Arial", Font.BOLD, 26));
        g2.setColor(new Color(0xFFD700));
        int lv = g2.getFontMetrics().stringWidth(vencedor);
        g2.drawString(vencedor, cardX + (cardW - lv) / 2, cardY + 48);

        int y = cardY + 86;
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        g2.setColor(Color.WHITE);
        String[] linhas = linhasFim();
        for (String linha : linhas) {
            int ll = g2.getFontMetrics().stringWidth(linha);
            g2.drawString(linha, cardX + (cardW - ll) / 2, y);
            y += 28;
        }

        // botões
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        int yB = cardY + cardH - 72;
        Botao deNovo = novoBotao("Jogar de novo", cardX + 40, yB, 150, 34,
                () -> iniciarPartida());
        deNovo.selecionado = true;
        desenharBotao(g2, deNovo);
        novoBotao("Início", cardX + cardW - 190, yB, 150, 34, this::voltarAoBriefing);
        desenharBotao(g2, botoes.get(botoes.size() - 1));
    }

    private String vencedorTexto() {
        if (pong.getVencedor() == 1) {
            return "Jogador 1 venceu!";
        }
        return pong.isDoisJogadores() ? "Jogador 2 venceu!" : "Computador venceu!";
    }

    private String[] linhasFim() {
        String placar = "Placar final:  " + pong.getPontosEsquerda() + " x " + pong.getPontosDireita();
        String duracao = "Duração:  " + Cronometro.formatar(duracaoPartida);
        String troca = "Melhor troca:  " + pong.getMelhorTroca() + " rebatidas";
        String vel = "Velocidade máxima:  " + Math.round(pong.getVelocidadeMaxima()) + " px/s";
        return new String[]{placar, duracao, troca, vel};
    }

    // -------------------------------------------------------- Interação

    private void ativarHover(int px, int py) {
        boolean mudou = false;
        for (Botao b : botoes) {
            boolean hover = b.contem(px, py);
            if (b.hover != hover) {
                b.hover = hover;
                mudou = true;
            }
        }
        if (mudou) {
            tabuleiro.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            tabuleiro.repaint();
        }
    }

    private void clicar(int px, int py) {
        for (Botao b : botoes) {
            if (b.contem(px, py)) {
                b.acao.run();
                return;
            }
        }
    }

    private void dibujaAura(Graphics2D g2, int cx, int cy, int raio, Color cor) {
        for (int r = raio; r > 0; r -= 8) {
            double t = (double) r / raio;
            g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), (int) (14 * t)));
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);
        }
    }

    // ------------------------------------------------------------ Launcher

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("PONG");
            TelaPong jogo = new TelaPong();
            frame.setContentPane(jogo.getPainel());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    jogo.loop.start();
                    jogo.getPainel().requestFocusInWindow();
                }
            });
        });
    }
}