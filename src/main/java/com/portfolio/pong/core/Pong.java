package com.portfolio.pong.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado da partida de Pong: orquestra raquetes, bolas, IA, cronômetro,
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

    /** Rebatidas da própria raquete necessárias para liberar o especial. */
    public static final int CARGAS_PARA_ESPECIAL = 3;

    /** Velocidade de deslocamento das raquetes dos jogadores (px/s). */
    public static final double VELOCIDADE_RAQUETE = 440.0;

    /** Fator extra aplicado à velocidade quando o tempo chega a zero. */
    public static final double FATOR_URGENCIA = 0.8;

    private final double larguraCampo;
    private final double alturaCampo;

    private final Raquete raqueteEsquerda;
    private final Raquete raqueteDireita;
    /** Bolas em campo: a primeira ({@link #getBola()}) é a principal. */
    private final List<Bola> bolas;
    private Computador computador;

    private Modo modo = Modo.CLASSICO;
    private Computador.Dificuldade dificuldadeCPU = Computador.Dificuldade.MEDIO;
    private int alvoPontos = 7;
    private Cronometro cronometro;

    private int pontosEsquerda;
    private int pontosDireita;
    private boolean sacaEsquerda = true;

    private int cargaEspecial1;
    private int cargaEspecial2;
    private boolean especial1Ativo;
    private boolean especial2Ativo;
    private double tempoEspecial1;
    private double tempoEspecial2;
    /** Velocidade de cada bola no instante do especial (restaurada ao expirar). */
    private final Map<Bola, Double> baseEspecial1 = new HashMap<>();
    private final Map<Bola, Double> baseEspecial2 = new HashMap<>();

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
        this.bolas = new ArrayList<>();
        this.bolas.add(new Bola(larguraCampo, alturaCampo));
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
        cargaEspecial1 = 0;
        cargaEspecial2 = 0;
        especial1Ativo = false;
        especial2Ativo = false;
        tempoEspecial1 = 0;
        tempoEspecial2 = 0;
        baseEspecial1.clear();
        baseEspecial2.clear();
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

        // Especiais em andamento expiram (timer próprio por jogador).
        if (especial1Ativo) {
            tempoEspecial1 -= dt;
            if (tempoEspecial1 <= 0) {
                encerrarEspecial(1);
            }
        }
        if (especial2Ativo) {
            tempoEspecial2 -= dt;
            if (tempoEspecial2 <= 0) {
                encerrarEspecial(2);
            }
        }

        // IA acompanha a bola principal.
        if (computador != null) {
            computador.atualizar(bolas.get(0), dt);
        }

        // Física das bolas: move, rebate nas paredes e nas raquetes.
        bateuParedeNoFrame = false;
        bateuRaqueteNoFrame = false;
        for (Bola b : bolas) {
            b.mover(dt);
            bateuParedeNoFrame |= b.rebaterParedes();
            if (b.rebaterNaRaquete(raqueteEsquerda)) {
                bateuRaqueteNoFrame = true;
                registrarRebatida(1);
            }
            if (b.rebaterNaRaquete(raqueteDireita)) {
                bateuRaqueteNoFrame = true;
                registrarRebatida(2);
            }
            velocidadeMaxima = Math.max(velocidadeMaxima, b.getVelocidade());
        }

        // Rampa de tempo: a bola principal acelera conforme o cronômetro esgota.
        // Suspensa enquanto algum especial está ativo (a bola mantém o ×2).
        if (modo == Modo.TEMPO) {
            cronometro.atualizar(dt);
            if (!especial1Ativo && !especial2Ativo) {
                double alvo = Bola.VELOCIDADE_BASE * (1.0 + FATOR_URGENCIA * cronometro.fatorDeUrgencia());
                bolas.get(0).ajustarVelocidade(Math.min(alvo, Bola.VELOCIDADE_MAXIMA));
            }

            if (cronometro.acabou() && !golDeOuro) {
                if (pontosEsquerda == pontosDireita) {
                    golDeOuro = true;
                } else {
                    encerrar(pontosEsquerda > pontosDireita ? 1 : 2);
                    return;
                }
            }
        }

        // Durante o especial, a bola mantém o dobro da velocidade que tinha.
        aplicarDobroEspecial();

        // Gols: a primeira bola a cruzar a lateral decide o frame.
        for (Bola b : bolas) {
            if (b.saiuPelaDireita()) {
                marcarGol(1);
                break;
            } else if (b.saiuPelaEsquerda()) {
                marcarGol(2);
                break;
            }
        }
    }

    /**
     * Ativa o especial (chamas + velocidade ×2) para um jogador, consumindo a
     * bateria cheia (3 cargas). Duração {@link #DURACAO_ESPECIAL} segundos.
     *
     * @param jogador 1 (esquerda) ou 2 (direita)
     * @return {@code false} se indisponível (bateria incompleta, em uso ou encerrado)
     */
    public boolean usarEspecial(int jogador) {
        if (encerrado) {
            return false;
        }
        int carga = jogador == 1 ? cargaEspecial1 : cargaEspecial2;
        boolean ativo = jogador == 1 ? especial1Ativo : especial2Ativo;
        if (carga < CARGAS_PARA_ESPECIAL || ativo) {
            return false;
        }

        // Congela a velocidade de cada bola no instante da ativação.
        Map<Bola, Double> base = jogador == 1 ? baseEspecial1 : baseEspecial2;
        base.clear();
        for (Bola b : bolas) {
            base.put(b, b.getVelocidade());
        }

        if (jogador == 1) {
            cargaEspecial1 = 0;
            especial1Ativo = true;
            tempoEspecial1 = DURACAO_ESPECIAL;
            return true;
        }
        if (jogador == 2) {
            cargaEspecial2 = 0;
            especial2Ativo = true;
            tempoEspecial2 = DURACAO_ESPECIAL;
            return true;
        }
        return false;
    }

    /**
     * Restaura a velocidade das bolas ao valor anterior ao especial e desliga
     * o estado ativo do jogador.
     */
    private void encerrarEspecial(int jogador) {
        Map<Bola, Double> base = jogador == 1 ? baseEspecial1 : baseEspecial2;
        for (Map.Entry<Bola, Double> e : base.entrySet()) {
            if (bolas.contains(e.getKey())) {
                e.getKey().ajustarVelocidade(e.getValue());
            }
        }
        base.clear();
        if (jogador == 1) {
            especial1Ativo = false;
            tempoEspecial1 = 0;
        } else {
            especial2Ativo = false;
            tempoEspecial2 = 0;
        }
    }

    /**
     * Mantém as bolas no dobro da velocidade registrada no início do especial.
     * Se os dois especiais estiverem ativos, vale o ativado por último.
     */
    private void aplicarDobroEspecial() {
        Map<Bola, Double> base = null;
        if (especial1Ativo) {
            base = baseEspecial1;
        } else if (especial2Ativo) {
            base = baseEspecial2;
        }
        if (base == null || base.isEmpty()) {
            return;
        }
        double teto = Bola.VELOCIDADE_MAXIMA;
        for (Bola b : bolas) {
            Double origem = base.get(b);
            if (origem != null) {
                b.ajustarVelocidade(Math.min(teto, origem * 2.0));
            }
        }
    }

    /** @return {@code true} se a bola deve aparecer em chamas (fogo visual) */
    public boolean isBolaEmChamas() {
        return especial1Ativo || especial2Ativo || bolas.get(0).getVelocidade() >= LIMIAR_CHAMAS;
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
        // O especial em andamento encerra no gol, mas a bateria carregada fica.
        especial1Ativo = false;
        especial2Ativo = false;
        tempoEspecial1 = 0;
        tempoEspecial2 = 0;
        baseEspecial1.clear();
        baseEspecial2.clear();

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
        // Remove bolas extras (prêmio de bola extra) e volta ao saque único.
        while (bolas.size() > 1) {
            bolas.remove(bolas.size() - 1);
        }
        bolas.get(0).centralizar(larguraCampo / 2.0, alturaCampo / 2.0, sacaEsquerda ? 1 : -1);
    }

    private void encerrar(int quemVenceu) {
        encerrado = true;
        vencedor = quemVenceu;
    }

    private void registrarRebatida(int jogador) {
        trocaAtual++;
        melhorTroca = Math.max(melhorTroca, trocaAtual);

        // Bateria do especial: recarrega a cada rebatida da própria raquete,
        // mas não enquanto o próprio especial está ativo.
        boolean ativo = jogador == 1 ? especial1Ativo : especial2Ativo;
        if (!ativo) {
            if (jogador == 1 && cargaEspecial1 < CARGAS_PARA_ESPECIAL) {
                cargaEspecial1++;
            } else if (jogador == 2 && cargaEspecial2 < CARGAS_PARA_ESPECIAL) {
                cargaEspecial2++;
            }
        }
    }

    // ---------- Acessores para a interface ----------

    public Raquete getRaqueteEsquerda() {
        return raqueteEsquerda;
    }

    public Raquete getRaqueteDireita() {
        return raqueteDireita;
    }

    public Bola getBola() {
        return bolas.get(0);
    }

    /** @return todas as bolas em campo (a primeira é a principal) */
    public List<Bola> getBolas() {
        return bolas;
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

    /** Cargas atuais da bateria do especial (0 a {@link #CARGAS_PARA_ESPECIAL}). */
    public int getCargaEspecial(int jogador) {
        return jogador == 1 ? cargaEspecial1 : cargaEspecial2;
    }

    public boolean isEspecial1Ativo() {
        return especial1Ativo;
    }

    public boolean isEspecial2Ativo() {
        return especial2Ativo;
    }

    /** Segundos restantes do especial ativo do jogador (0 se inativo). */
    public double getTempoEspecialRestante(int jogador) {
        return jogador == 1 ? tempoEspecial1 : tempoEspecial2;
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