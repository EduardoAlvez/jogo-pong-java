package com.portfolio.pong.core;

/**
 * Estado da partida de Pong: orquestra raquetes, bola, IA, cronômetro,
 * placar, o especial de bola em chamas e as estatísticas da partida.
 *
 * <p>Núcleo puro (sem Swing): recebe apenas {@code dt} em segundos e expõe
 * dados para a interface desenhar. A regra de "bola em chamas" é decidida
 * aqui por velocidade (rampa de tempo) ou pelo uso do especial — a
 * renderização fica por conta da camada gráfica.</p>
 *
 * @author Eduardo Alvez
 */
public class Pong {

    /** Modo de partida. */
    public enum Modo {
        /** Partida cronometrada estilo futebol (1/2/3 min), com gol de ouro. */
        TEMPO,
        /** Partida clássica: primeiro a atingir o placar-alvo. */
        CLASSICO
    }

    /** Velocidade (px/s) acima da qual a bola pega fogo sozinha. */
    public static final double LIMIAR_CHAMAS = 540.0;

    /** Duração do especial de bola em chamas, em segundos. */
    public static final double DURACAO_ESPECIAL = 5.0;

    /** Velocidade de deslocamento das raquetes dos jogadores (px/s). */
    public static final double VELOCIDADE_RAQUETE = 440.0;

    /** Fator extra aplicado à velocidade quando o tempo chega a zero. */
    public static final double FATOR_URGENCIA = 0.8;

    private final double larguraCampo;
    private final double alturaCampo;

    private final Raquete raqueteEsquerda;
    private final Raquete raqueteDireita;
    private final Bola bola;
    private Computador computador;

    private Modo modo = Modo.CLASSICO;
    private Computador.Dificuldade dificuldadeCPU = Computador.Dificuldade.MEDIO;
    private int alvoPontos = 7;
    private Cronometro cronometro;

    private int pontosEsquerda;
    private int pontosDireita;
    private boolean sacaEsquerda = true;

    private boolean especial1Ativo;
    private boolean especial2Ativo;
    private double tempoEspecialRestante;
    private boolean especial1Disponivel = true;
    private boolean especial2Disponivel = true;

    private boolean golDeOuro;
    private boolean encerrado;
    private int vencedor;

    private double velocidadeMaxima;
    private int melhorTroca;
    private int trocaAtual;

    private boolean bateuParedeNoFrame;
    private boolean bateuRaqueteNoFrame;

    /**
     * @param larguraCampo largura do campo em pixels
     * @param alturaCampo  altura do campo em pixels
     */
    public Pong(double larguraCampo, double alturaCampo) {
        this.larguraCampo = larguraCampo;
        this.alturaCampo = alturaCampo;
        this.raqueteEsquerda = new Raquete(30, (int) alturaCampo);
        this.raqueteDireita = new Raquete((int) (larguraCampo - 30 - Raquete.LARGURA), (int) alturaCampo);
        this.bola = new Bola(larguraCampo, alturaCampo);
        this.cronometro = new Cronometro(Cronometro.DURACAO_2MIN);
    }

    /**
     * Configura (e inicia) uma partida.
     *
     * @param modo          modo da partida
     * @param dificuldadeCPU dificuldade da IA (ignorada no modo 2P)
     * @param doisJogadores se {@code true}, dispensa a IA
     * @param alvoPontos    placar-alvo no modo clássico (5/7/10)
     * @param duracaoSegundos duração para o modo tempo (60/120/180)
     */
    public void configurar(Modo modo, Computador.Dificuldade dificuldadeCPU, boolean doisJogadores,
                           int alvoPontos, int duracaoSegundos) {
        this.modo = modo;
        this.dificuldadeCPU = dificuldadeCPU;
        this.alvoPontos = alvoPontos;
        this.cronometro = new Cronometro(duracaoSegundos);
        this.computador = doisJogadores ? null : new Computador(raqueteDireita, dificuldadeCPU);
        iniciarPartida();
    }

    /** Inicia/reinicia a partida com o estado zerado. */
    public void iniciarPartida() {
        pontosEsquerda = 0;
        pontosDireita = 0;
        sacaEsquerda = true;
        especial1Ativo = false;
        especial2Ativo = false;
        tempoEspecialRestante = 0;
        especial1Disponivel = true;
        especial2Disponivel = true;
        golDeOuro = false;
        encerrado = false;
        vencedor = 0;
        velocidadeMaxima = 0;
        melhorTroca = 0;
        trocaAtual = 0;
        raqueteEsquerda.centralizar();
        raqueteDireita.centralizar();
        if (computador != null) {
            computador = new Computador(raqueteDireita, dificuldadeCPU);
        }
        cronometro.reiniciar();
        sacar();
    }

    /**
     * Avança a simulação em um passo de tempo.
     *
     * @param dt tempo decorrido em segundos
     */
    public void atualizar(double dt) {
        if (encerrado) {
            return;
        }

        // Especiais em andamento expiram.
        if (tempoEspecialRestante > 0) {
            tempoEspecialRestante -= dt;
            if (tempoEspecialRestante <= 0) {
                tempoEspecialRestante = 0;
                especial1Ativo = false;
                especial2Ativo = false;
            }
        }

        // IA acompanha a bola.
        if (computador != null) {
            computador.atualizar(bola, dt);
        }

        // Física da bola: move, rebate nas paredes e nas raquetes.
        bateuParedeNoFrame = false;
        bateuRaqueteNoFrame = false;
        bola.mover(dt);
        bateuParedeNoFrame = bola.rebaterParedes();
        if (bola.rebaterNaRaquete(raqueteEsquerda)) {
            bateuRaqueteNoFrame = true;
            registrarRebatida();
        }
        if (bola.rebaterNaRaquete(raqueteDireita)) {
            bateuRaqueteNoFrame = true;
            registrarRebatida();
        }
        velocidadeMaxima = Math.max(velocidadeMaxima, bola.getVelocidade());

        // Rampa de tempo: bola acelera conforme o cronômetro esgota.
        if (modo == Modo.TEMPO) {
            cronometro.atualizar(dt);
            double alvo = Bola.VELOCIDADE_BASE * (1.0 + FATOR_URGENCIA * cronometro.fatorDeUrgencia());
            bola.ajustarVelocidade(Math.min(alvo, Bola.VELOCIDADE_MAXIMA));

            if (cronometro.acabou() && !golDeOuro) {
                if (pontosEsquerda == pontosDireita) {
                    golDeOuro = true;
                } else {
                    encerrar(pontosEsquerda > pontosDireita ? 1 : 2);
                    return;
                }
            }
        }

        // Gols.
        if (bola.saiuPelaDireita()) {
            marcarGol(1);
        } else if (bola.saiuPelaEsquerda()) {
            marcarGol(2);
        }
    }

    /**
     * Ativa o especial de bola em chamas para um jogador (1 uso por partida).
     *
     * @param jogador 1 (esquerda) ou 2 (direita)
     * @return {@code false} se indisponível (já usado, encerrado ou em uso)
     */
    public boolean usarEspecial(int jogador) {
        if (encerrado) {
            return false;
        }
        if (jogador == 1 && especial1Disponivel && !especial1Ativo) {
            especial1Disponivel = false;
            especial1Ativo = true;
            tempoEspecialRestante = DURACAO_ESPECIAL;
            return true;
        }
        if (jogador == 2 && especial2Disponivel && !especial2Ativo) {
            especial2Disponivel = false;
            especial2Ativo = true;
            tempoEspecialRestante = DURACAO_ESPECIAL;
            return true;
        }
        return false;
    }

    /** @return {@code true} se a bola deve aparecer em chamas (fogo visual) */
    public boolean isBolaEmChamas() {
        return especial1Ativo || especial2Ativo || bola.getVelocidade() >= LIMIAR_CHAMAS;
    }

    /** Move a raquete de um jogador. */
    public void moverJogador(int jogador, int direcao, double dt) {
        int passos = (int) Math.round(VELOCIDADE_RAQUETE * dt);
        Raquete raquete = jogador == 1 ? raqueteEsquerda : raqueteDireita;
        if (direcao < 0) {
            raquete.moverCima(passos);
        } else if (direcao > 0) {
            raquete.moverBaixo(passos);
        }
    }

    private void marcarGol(int quemMarca) {
        if (quemMarca == 1) {
            pontosEsquerda++;
        } else {
            pontosDireita++;
        }
        trocaAtual = 0;
        especial1Ativo = false;
        especial2Ativo = false;
        tempoEspecialRestante = 0;

        // Gol de ouro decide na hora.
        if (golDeOuro) {
            encerrar(quemMarca);
            return;
        }

        // Modo clássico: primeiro a atingir o placar-alvo vence.
        if (modo == Modo.CLASSICO
                && (pontosEsquerda == alvoPontos || pontosDireita == alvoPontos)) {
            encerrar(pontosEsquerda == alvoPontos ? 1 : 2);
            return;
        }

        // Quem sofreu o gol saca a próxima.
        sacaEsquerda = quemMarca == 2;
        sacar();
    }

    private void sacar() {
        raqueteEsquerda.centralizar();
        raqueteDireita.centralizar();
        bola.centralizar(larguraCampo / 2.0, alturaCampo / 2.0, sacaEsquerda ? 1 : -1);
    }

    private void encerrar(int quemVenceu) {
        encerrado = true;
        vencedor = quemVenceu;
    }

    private void registrarRebatida() {
        trocaAtual++;
        melhorTroca = Math.max(melhorTroca, trocaAtual);
    }

    // ---------- Acessores para a interface ----------

    public Raquete getRaqueteEsquerda() {
        return raqueteEsquerda;
    }

    public Raquete getRaqueteDireita() {
        return raqueteDireita;
    }

    /** @return dificuldade configurada para a IA */
    public Computador.Dificuldade getDificuldade() {
        return dificuldadeCPU;
    }

    public Bola getBola() {
        return bola;
    }

    public Modo getModo() {
        return modo;
    }

    public Cronometro getCronometro() {
        return cronometro;
    }

    public boolean isGolDeOuro() {
        return golDeOuro;
    }

    public boolean isEncerrado() {
        return encerrado;
    }

    /** @return 0 (sem vencedor), 1 (esquerda) ou 2 (direita) */
    public int getVencedor() {
        return vencedor;
    }

    public int getPontosEsquerda() {
        return pontosEsquerda;
    }

    public int getPontosDireita() {
        return pontosDireita;
    }

    public int getAlvoPontos() {
        return alvoPontos;
    }

    public boolean isEspecial1Disponivel() {
        return especial1Disponivel;
    }

    public boolean isEspecial2Disponivel() {
        return especial2Disponivel;
    }

    public boolean isEspecial1Ativo() {
        return especial1Ativo;
    }

    public boolean isEspecial2Ativo() {
        return especial2Ativo;
    }

    public double getTempoEspecialRestante() {
        return tempoEspecialRestante;
    }

    public double getVelocidadeMaxima() {
        return velocidadeMaxima;
    }

    public int getMelhorTroca() {
        return melhorTroca;
    }

    public boolean isDoisJogadores() {
        return computador == null;
    }

    public double getLarguraCampo() {
        return larguraCampo;
    }

    public double getAlturaCampo() {
        return alturaCampo;
    }

    /** @return {@code true} se a bola rebateu na parede neste frame */
    public boolean bateuParedeNoFrame() {
        return bateuParedeNoFrame;
    }

    /** @return {@code true} se a bola rebateu em alguma raquete neste frame */
    public boolean bateuRaqueteNoFrame() {
        return bateuRaqueteNoFrame;
    }
}