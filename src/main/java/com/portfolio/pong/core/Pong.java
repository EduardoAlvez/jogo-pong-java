package com.portfolio.pong.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

    /** Primeiro prêmio surge após esse tempo de partida (segundos). */
    public static final double PRIMEIRO_SPAWN_PREMIO = 8.0;

    /** Intervalo aleatório entre prêmios, em segundos: {@code [MIN, MAX]}. */
    public static final double INTERVALO_SPAWN_PREMIO_MIN = 10.0;
    public static final double INTERVALO_SPAWN_PREMIO_MAX = 16.0;

    /** Duração do efeito "inverter" (controles do adversário), em segundos. */
    public static final double DURACAO_INVERTE = 10.0;

    /** Duração do efeito "congelar" (raquete travada), em segundos. */
    public static final double DURACAO_CONGELA = 4.0;

    /** Duração do efeito "turbo" (raquete acelerada), em segundos. */
    public static final double DURACAO_TURBO = 10.0;

    /** Duração do efeito "chamas" (bola em chamas), em segundos. */
    public static final double DURACAO_CHAMAS = 6.0;

    /** Duração do efeito "encolher" (raquete menor), em segundos. */
    public static final double DURACAO_ENCOLHE = 10.0;

    /** Pausa antes do saque seguinte, para a animação de gol terminar, em segundos. */
    public static final double PAUSA_APOS_GOL = 1.0;

    /** Multiplicador de velocidade da raquete durante o "turbo". */
    public static final double FATOR_TURBO = 1.4;

    /** Altura da raquetado adversário enquanto "encolhida", em pixels. */
    public static final int ALTURA_ENCOLHIDA = Raquete.ALTURA_MINIMA;

    /** Máximo de bolas em campo com o prêmio "bola extra". */
    public static final int MAX_BOLAS = 2;

    /** Quantos prêmios cada leva traz: de 1 a este número. */
    public static final int MAX_PREMIO_POR_LEVA = 3;

    /** Teto de prêmios simultâneos no campo (evita acúmulo). */
    public static final int MAX_PREMIO_CAMPO = 5;

    /** Distância mínima entre dois prêmios da mesma leva, em pixels. */
    public static final double DISTANCIA_MINIMA_PREMIO = 90.0;

    /** Distância mínima de um prêmio até a bola, em pixels. */
    public static final double DISTANCIA_MINIMA_BOLA = 70.0;

    /** Segundos somados pelo prêmio "tempo". */
    public static final int BONUS_TEMPO = 10;

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

    // Prêmios no campo.
    private final Random aleatorio = new Random();
    private final List<Premio> premios = new ArrayList<>();
    private double tempoProximoSpawn;
    private double pausaGol;
    /** Último jogador que rebateu a bola (é quem coleta o prêmio). */
    private int ultimoRebatedor;
    private boolean premioColetadoFrame;
    private double premioColetadoX;
    private double premioColetadoY;
    private int premioColetadoCor;
    private int premios1;
    private int premios2;

    // Efeitos dos prêmios (índice 0 = jogador 1, índice 1 = jogador 2).
    private final double[] tempoInvertido = new double[2];
    private final double[] tempoCongelado = new double[2];
    private final double[] tempoTurbo = new double[2];
    private final double[] tempoChamas = new double[2];
    private final double[] tempoEncolhido = new double[2];

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
                raqueteEsquerda.setAltura(Raquete.ALTURA);
        raqueteDireita.setAltura(Raquete.ALTURA);
        raqueteEsquerda.centralizar();
        raqueteDireita.centralizar();
        premios.clear();
        tempoProximoSpawn = PRIMEIRO_SPAWN_PREMIO;
        pausaGol = 0.0;
        ultimoRebatedor = 0;
        premioColetadoFrame = false;
        premios1 = 0;
        premios2 = 0;
        Arrays.fill(tempoInvertido, 0.0);
        Arrays.fill(tempoCongelado, 0.0);
        Arrays.fill(tempoTurbo, 0.0);
        Arrays.fill(tempoChamas, 0.0);
        Arrays.fill(tempoEncolhido, 0.0);
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

        // Pausa pós-gol: nada se move enquanto a animação de gol roda. O saque
        // (e a volta da velocidade à base) só acontece quando ela termina.
        if (pausaGol > 0.0) {
            pausaGol -= dt;
            if (pausaGol <= 0.0) {
                pausaGol = 0.0;
                sacar();
            }
            return;
        }

        premioColetadoFrame = false;
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

        // Efeitos dos prêmios decaem (e a raquete encolhida volta ao normal).
        decairEfeitos(dt);

        // IA defende a bola que chega primeiro (a CPU também pode ficar congelada).
        if (computador != null && tempoCongelado[1] <= 0) {
            computador.atualizar(bolaMaisUrgenteParaCpu(), dt);
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

        // Prêmios: expiram o atual e podem surgir novos; coleta pela bola.
        atualizarPremio(dt);

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
     * <p>Só é liberado com ao menos uma bola no campo do próprio jogador (do meio
     * para o seu lado) e apenas um especial fica ativo por vez.
     *
     * @param jogador 1 (esquerda) ou 2 (direita)
     * @return {@code false} se indisponível (bateria incompleta, especial do
     *         outro em uso, bola no campo do adversário, pausa ou encerrado)
     */
    public boolean usarEspecial(int jogador) {
        if (encerrado || pausaGol > 0.0) {
            return false;
        }
        int carga = jogador == 1 ? cargaEspecial1 : cargaEspecial2;
        if (carga < CARGAS_PARA_ESPECIAL) {
            return false;
        }
        // Um especial por vez: os dois shareariam a mesma bola e o segundo
        // capturaria a velocidade já dobrada como se fosse a base.
        if (especial1Ativo || especial2Ativo) {
            return false;
        }
        if (!especialLiberado(jogador)) {
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
     * O especial só entra com a bola no território do jogador: do meio do campo
     * para o seu lado. A linha do meio é compartilhada, então a bola exatamente
     * no centro libera os dois. Com várias bolas basta uma estar do seu lado.
     */
    private boolean especialLiberado(int jogador) {
        double centro = larguraCampo / 2.0;
        for (Bola b : bolas) {
            if (jogador == 1 ? b.getX() <= centro : b.getX() >= centro) {
                return true;
            }
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
     * Como só um especial fica ativo por vez, os dois ramos são mutuamente
     * exclusivos: o do jogador 1 é o que está valendo.
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
        return especial1Ativo || especial2Ativo
                || tempoChamas[0] > 0 || tempoChamas[1] > 0
                || bolas.get(0).getVelocidade() >= LIMIAR_CHAMAS;
    }

    /** Move a raquete de um jogador (respeita congelamento e turbo). */
    public void moverJogador(int jogador, int direcao, double dt) {
        if (pausaGol > 0.0) {
            return;
        }
        int i = jogador - 1;
        if (tempoCongelado[i] > 0) {
            return;
        }
        double velocidade = tempoTurbo[i] > 0 ? VELOCIDADE_RAQUETE * FATOR_TURBO : VELOCIDADE_RAQUETE;
        int passos = (int) Math.round(velocidade * dt);
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

        // Quem sofreu o gol saca a próxima, mas só depois da animação.
        sacaEsquerda = quemMarca == 2;
        pausaGol = PAUSA_APOS_GOL;
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
        ultimoRebatedor = jogador;

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

// ---------- Prêmios no campo ----------

    /**
     * Gerencia os prêmios do campo: agenda as levas, expira os que acabaram e
     * detecta a coleta pela bola (um por frame; quem coleta é o último que
     * rebateu). No gol de ouro não surge leva nova, mas os prêmios já em campo
     * seguem correndo: expiram e podem ser coletados.
     */
    private void atualizarPremio(double dt) {
        // No gol de ouro nada novo surge, mas o que já está no campo continua
        // valendo: expira sozinho e ainda pode ser coletado. Um "return"
        // aqui congelaria o card no lugar — sem pulso, sem sumir e com a bola
        // passando por cima sem efeito, que é o que parece travado.
        if (premios.isEmpty()) {
            if (golDeOuro) {
                return;
            }
            tempoProximoSpawn -= dt;
            if (tempoProximoSpawn <= 0.0) {
                criarLeva();
                agendarProximoSpawn();
            }
            return;
        }

        // Expiram os que chegaram ao fim da validade.
        for (int i = premios.size() - 1; i >= 0; i--) {
            Premio p = premios.get(i);
            p.atualizar(dt);
            if (p.acabou()) {
                premios.remove(i);
            }
        }
        if (premios.isEmpty()) {
            agendarProximoSpawn();
            return;
        }

        // A bola coleta o primeiro prêmio que encostar (no máximo um por frame).
        for (Bola b : bolas) {
            for (int i = 0; i < premios.size(); i++) {
                Premio p = premios.get(i);
                double dx = b.getX() - p.getX();
                double dy = b.getY() - p.getY();
                if (Math.hypot(dx, dy) > b.getRaio() + Premio.RAIO) {
                    continue;
                }
                Premio.Tipo tipo = p.getTipo();
                double x = p.getX();
                double y = p.getY();
                int cor = tipo.getCorRgb();
                // Sem rebatedor ainda (bola do saque tocou o prêmio): quem "vai
                // buscar" é o lado para onde a bola está indo.
                int coletor = ultimoRebatedor != 0 ? ultimoRebatedor : (b.getVx() > 0 ? 1 : 2);
                premios.remove(i);
                if (premios.isEmpty()) {
                    agendarProximoSpawn();
                }
                coletarPremio(tipo, coletor, x, y, cor);
                return;
            }
        }
    }

    /**
     * Sorteia uma leva de 1 a {@link #MAX_PREMIO_POR_LEVA} prêmios, cada um com
     * tipo e posição livres, longe uns dos outros e da bola.
     */
    private void criarLeva() {
        int quantos = 1 + aleatorio.nextInt(MAX_PREMIO_POR_LEVA);
        int vagos = MAX_PREMIO_CAMPO - premios.size();
        quantos = Math.max(0, Math.min(quantos, vagos));
        List<Premio.Tipo> pool = tiposDePremio();
        for (int i = 0; i < quantos; i++) {
            Premio.Tipo tipo = pool.get(aleatorio.nextInt(pool.size()));
            double[] posicao = sortearPosicaoLivre();
            premios.add(new Premio(tipo, posicao[0], posicao[1]));
        }
    }

    /** Sorteia um ponto dentro do campo, longe dos prêmios e da bola. */
    private double[] sortearPosicaoLivre() {
        for (int tentativa = 0; tentativa < 40; tentativa++) {
            double x = 70 + aleatorio.nextDouble() * (larguraCampo - 140);
            double y = 80 + aleatorio.nextDouble() * (alturaCampo - 160);
            if (!longeDasBolas(x, y) && !longeDeOutrosPremios(x, y)) {
                return new double[] {x, y};
            }
        }
        // Campo cheio: varre uma grade e fica com o ponto mais distante de
        // qualquer prêmio e da bola, para não empilhar cards sobrepostos.
        return pontoMaisLivre();
    }

    /** Melhor ponto de uma grade regular, medindo a distância ao obstáculo mais perto. */
    private double[] pontoMaisLivre() {
        final int colunas = 11;
        final int linhas = 7;
        double[] melhor = {larguraCampo / 2.0, alturaCampo / 2.0};
        double melhorDistancia = -1.0;
        for (int c = 0; c < colunas; c++) {
            for (int l = 0; l < linhas; l++) {
                double x = 70 + (larguraCampo - 140) * c / (colunas - 1.0);
                double y = 80 + (alturaCampo - 160) * l / (linhas - 1.0);
                double distancia = distanciaAObstaculoMaisProximo(x, y);
                if (distancia > melhorDistancia) {
                    melhorDistancia = distancia;
                    melhor[0] = x;
                    melhor[1] = y;
                }
            }
        }
        return melhor;
    }

    /** Distância de (x, y) até o prêmio ou bola mais próximo. */
    private double distanciaAObstaculoMaisProximo(double x, double y) {
        double minima = Double.MAX_VALUE;
        for (Bola b : bolas) {
            minima = Math.min(minima, Math.hypot(b.getX() - x, b.getY() - y));
        }
        for (Premio p : premios) {
            minima = Math.min(minima, Math.hypot(p.getX() - x, p.getY() - y));
        }
        return minima;
    }

    private boolean longeDasBolas(double x, double y) {
        for (Bola b : bolas) {
            if (Math.hypot(b.getX() - x, b.getY() - y) < DISTANCIA_MINIMA_BOLA) {
                return false;
            }
        }
        return true;
    }

    private boolean longeDeOutrosPremios(double x, double y) {
        for (Premio p : premios) {
            if (Math.hypot(p.getX() - x, p.getY() - y) < DISTANCIA_MINIMA_PREMIO) {
                return false;
            }
        }
        return true;
    }

    /** Agenda a próxima leva entre {@code [MIN, MAX]} segundos. */
    private void agendarProximoSpawn() {
        tempoProximoSpawn = INTERVALO_SPAWN_PREMIO_MIN
                + aleatorio.nextDouble() * (INTERVALO_SPAWN_PREMIO_MAX - INTERVALO_SPAWN_PREMIO_MIN);
    }

    /** Tipos elegíveis para o modo atual: sem INVERTE em 1P, sem TEMPO no clássico. */
    List<Premio.Tipo> tiposDePremio() {
        List<Premio.Tipo> pool = new ArrayList<>();
        for (Premio.Tipo t : Premio.Tipo.values()) {
            if (t == Premio.Tipo.INVERTE && !isDoisJogadores()) {
                continue;
            }
            if (t == Premio.Tipo.TEMPO && modo != Modo.TEMPO) {
                continue;
            }
            pool.add(t);
        }
        return pool;
    }

    /** Registra a coleta e aplica o efeito do prêmio. */
    private void coletarPremio(Premio.Tipo tipo, int coletor, double x, double y, int cor) {
        premioColetadoFrame = true;
        premioColetadoX = x;
        premioColetadoY = y;
        premioColetadoCor = cor;
        if (coletor == 1) {
            premios1++;
        } else {
            premios2++;
        }
        aplicarPremio(tipo, coletor);
    }

    /**
     * Aplica o efeito do prêmio ao coletor (e/ou ao adversário).
     *
     * @param tipo    prêmio coletado
     * @param coletor quem coletou (1 ou 2)
     */
    private void aplicarPremio(Premio.Tipo tipo, int coletor) {
        int i = coletor - 1;
        int adv = coletor == 1 ? 1 : 0;
        switch (tipo) {
            case INVERTE:
                tempoInvertido[adv] = DURACAO_INVERTE;
                break;
            case CONGELA:
                tempoCongelado[adv] = DURACAO_CONGELA;
                break;
            case TURBO:
                tempoTurbo[i] = DURACAO_TURBO;
                break;
            case CHAMAS:
                tempoChamas[i] = DURACAO_CHAMAS;
                break;
            case TEMPO:
                cronometro.adicionarSegundos(BONUS_TEMPO);
                break;
            case ENCOLHE:
                tempoEncolhido[adv] = DURACAO_ENCOLHE;
                (adv == 0 ? raqueteEsquerda : raqueteDireita).setAltura(ALTURA_ENCOLHIDA);
                break;
            case DUPLO:
                adicionarBolaExtra();
                break;
            case CORINGA:
                Premio.Tipo novo = sortearTipoExcluindo(Premio.Tipo.CORINGA);
                if (novo != null) {
                    aplicarPremio(novo, coletor);
                }
                break;
            default:
                break;
        }
    }

    /** Sorteia um prêmio válido, excluindo um tipo (usado pelo coringa). */
    private Premio.Tipo sortearTipoExcluindo(Premio.Tipo excluir) {
        List<Premio.Tipo> pool = tiposDePremio();
        pool.remove(excluir);
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(aleatorio.nextInt(pool.size()));
    }

    /** Cria uma segunda bola espelhada (prêmio "bola extra"), até o limite. */
    private void adicionarBolaExtra() {
        if (bolas.size() >= MAX_BOLAS) {
            return;
        }
        Bola nova = new Bola(bolas.get(0));
        nova.espelharVertical();
        // afasta um pouco da principal para não nascerem coladas
        nova.mover(0.08);
        bolas.add(nova);
    }

    /**
     * Bola que a CPU precisa defender primeiro: a que cruza a linha da raquete
     * dela no menor tempo. Bolas indo embora ({@code vx <= 0}) não são ameaça e
     * são ignoradas, para a CPU não abandonar a outra por causa delas. Sem
     * nenhuma se aproximando, devolve a bola principal — assim a raquete segue
     * voltando ao centro, como antes.
     */
    private Bola bolaMaisUrgenteParaCpu() {
        double linhaRaquete = raqueteDireita.getX();
        Bola urgente = null;
        double menorTempo = Double.MAX_VALUE;
        for (Bola b : bolas) {
            if (b.getVx() <= 0.0) {
                continue;
            }
            double tempo = (linhaRaquete - b.getX()) / b.getVx();
            if (tempo < menorTempo) {
                menorTempo = tempo;
                urgente = b;
            }
        }
        return urgente != null ? urgente : bolas.get(0);
    }

    /** Decrementa os timers dos efeitos e restaura a altura da raquete. */
    private void decairEfeitos(double dt) {
        for (double[] timers : Arrays.asList(tempoInvertido, tempoCongelado,
                tempoTurbo, tempoChamas, tempoEncolhido)) {
            for (int i = 0; i < timers.length; i++) {
                timers[i] = Math.max(0.0, timers[i] - dt);
            }
        }
        if (tempoEncolhido[0] <= 0 && raqueteEsquerda.getAltura() != Raquete.ALTURA) {
            raqueteEsquerda.setAltura(Raquete.ALTURA);
        }
        if (tempoEncolhido[1] <= 0 && raqueteDireita.getAltura() != Raquete.ALTURA) {
            raqueteDireita.setAltura(Raquete.ALTURA);
        }
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

    /** @return {@code true} enquanto a animação de gol roda, antes do novo saque */
    public boolean isAguardandoSaque() {
        return pausaGol > 0.0;
    }

    /** @return os prêmios atualmente no campo (lista vazia se não há) */
    public List<Premio> getPremios() {
        return premios;
    }

    /** @return {@code true} se um prêmio foi coletado neste frame */
    public boolean coletouPremioNoFrame() {
        return premioColetadoFrame;
    }

    public double getPremioColetadoX() {
        return premioColetadoX;
    }

    public double getPremioColetadoY() {
        return premioColetadoY;
    }

    /** @return cor RGB do prêmio coletado neste frame */
    public int getPremioColetadoCor() {
        return premioColetadoCor;
    }

    /** @return quantos prêmios o jogador já coletou */
    public int getPremios(int jogador) {
        return jogador == 1 ? premios1 : premios2;
    }

    /** Segundos restantes do efeito "inverter" no jogador (0 se inativo). */
    public double getTempoInvertido(int jogador) {
        return tempoInvertido[jogador - 1];
    }

    /** Segundos restantes do efeito "congelar" no jogador (0 se inativo). */
    public double getTempoCongelado(int jogador) {
        return tempoCongelado[jogador - 1];
    }

    /** Segundos restantes do efeito "turbo" no jogador (0 se inativo). */
    public double getTempoTurbo(int jogador) {
        return tempoTurbo[jogador - 1];
    }

    /** Segundos restantes do efeito "chamas" no jogador (0 se inativo). */
    public double getTempoChamas(int jogador) {
        return tempoChamas[jogador - 1];
    }

    /** Segundos restantes do efeito "encolher" no jogador (0 se inativo). */
    public double getTempoEncolhido(int jogador) {
        return tempoEncolhido[jogador - 1];
    }

    /** @return {@code true} se os controles do jogador estão invertidos */
    public boolean isControleInvertido(int jogador) {
        return tempoInvertido[jogador - 1] > 0;
    }

    /** @return {@code true} se a raquete do jogador está travada */
    public boolean isRaqueteCongelada(int jogador) {
        return tempoCongelado[jogador - 1] > 0;
    }

    /** @return {@code true} se a bola está em chamas por prêmio (sem especial) */
    public boolean isChamasPorPremio(int jogador) {
        return tempoChamas[jogador - 1] > 0;
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