package com.portfolio.pong.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

import java.io.BufferedInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Gerencia os efeitos sonoros do jogo. Carrega os arquivos {@code .wav} do
 * classpath de forma preguiçosa (apenas no primeiro uso), guarda-os em memória
 * para reuso e permite silenciar tudo com um único interruptor global.
 *
 * <h2>Por que o som sumia dentro do executável</h2>
 * O mixer padrão do Java é escolhido uma vez e fica preso nele. Quando o
 * usuário troca o dispositivo de saída no meio da sessão — encaixa um fone,
 * conecta um USB, liga o Bluetooth — o Windows redireciona o áudio, mas a JVM
 * continua abrindo linhas no dispositivo antigo. Nesses casos a linha abre,
 * aceita {@code start()} e não produz som nenhum, sem lançar exceção. Como o
 * jogo antes engolia qualquer falha, o som simplesmente sumia.
 *
 * <p>Havia um segundo defeito, independente do dispositivo: o áudio é servido
 * de dentro do JAR, e {@code AudioSystem.getAudioInputStream()} precisa de
 * {@code mark/reset} para ler o cabeçalho do WAV. O stream cru de uma entrada
 * de JAR não oferece isso, então a leitura falhava com
 * {@code mark/reset not supported} e o jogo ficava mudo — tanto no jar quanto
 * no executável. Envolver o stream em {@link BufferedInputStream} resolve.
 *
 * <p>Por isso aqui o clipe é aberto em um mixer escolhido explicitamente, com
 * preferência pelo mixer padrão e tentativa nos demais quando o primeiro falha
 * ou fica inativo. Se nenhum funcionar, o estado é registrado e a UI pode
 * avisar o usuário em vez de deixar o jogo mudo em silêncio. Toda a sondagem
 * acontece fora da EDT, para não travar o jogo enquanto procura um dispositivo.

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
    private static final Map<Som, Clip> CLIPS = new ConcurrentHashMap<Som, Clip>();

    /** Efeitos pedidos enquanto o clipe ainda estava sendo carregado. */
    private static final Set<Som> PENDENTES = ConcurrentHashMap.newKeySet();

    /** Efeitos com carga em andamento, para não duplicar trabalho. */
    private static final Set<Som> CARREGANDO = ConcurrentHashMap.newKeySet();

    /**
     * Thread dedicada à abertura de linhas. Abrir um clipe exige ler o arquivo,
     * sondar mixers e dormir alguns milissegundos para conferir se o áudio saiu
     * — trabalho que não pode travar a EDT, sob pena de congelar o jogo.
     */
    private static final ExecutorService CARREGADOR =
            Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable tarefa) {
                    Thread t = new Thread(tarefa, "pong-audio");
                    t.setDaemon(true);
                    return t;
                }
            });

    /** Interruptor global de som (ligado por padrão). */
    private static boolean ligado = true;

    /** Mixer em uso, resolvido na primeira carga que funcionar. */
    private static Mixer mixerEscolhido;

    /** {@code true} quando nenhum mixer serviu, para a UI poder avisar. */
    private static boolean audioIndisponivel;

    /** Mensagem do último erro de abertura, para diagnóstico. */
    private static String ultimoErro = "";

    /** Tempo de espera para decidir se um clip realmente começou a tocar. */
    private static final long VERIFICACAO_MS = 40;

    /**
     * Abre as linhas de áudio. Existe para que os testes possam simular uma
     * máquina sem dispositivo de saída.
     */
    interface FabricaDeLinha {

        /**
         * @param mixer mixer onde a linha deve ser aberta, {@code null} para o
         *              mixer padrão do sistema
         * @param formato formato de áudio desejado
         * @return linha de reprodução aberta
         * @throws LineUnavailableException se o mixer não puder fornecer a linha
         */
        Clip abrir(Mixer mixer, AudioFormat formato) throws LineUnavailableException;
    }

    /** Fábrica real, baseada na API padrão do Java Sound. */
    private static final FabricaDeLinha FABRICA_REAL = (mixer, formato) -> {
        if (mixer == null) {
            return AudioSystem.getClip();
        }
        DataLine.Info info = new DataLine.Info(Clip.class, formato);
        if (!mixer.isLineSupported(info)) {
            throw new LineUnavailableException(
                    "mixer nao suporta Clip: " + mixer.getMixerInfo().getName());
        }
        return (Clip) mixer.getLine(info);
    };

    /** Fábrica em uso; substituída pelos testes. */
    private static FabricaDeLinha fabrica = FABRICA_REAL;

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
     * @return {@code true} quando nenhum mixer conseguiu fornecer áudio, o que
     *         normalmente significa saída indisponível ou volume zerado
     */
    public static boolean isAudioIndisponivel() {
        return audioIndisponivel;
    }

    /**
     * @return descrição do último erro encontrado ao abrir uma linha, ou string
     *         vazia se não houve erro
     */
    public static String getUltimoErro() {
        return ultimoErro;
    }

    /**
     * @return nome do mixer em uso, ou {@code null} se nenhum foi resolvido
     */
    public static String getMixerEmUso() {
        return mixerEscolhido == null ? null : mixerEscolhido.getMixerInfo().getName();
    }

    /**
     * Descarta todos os clipes em cache e resolve o mixer de novo na próxima
     * reprodução. Útil quando o usuário troca o dispositivo de saída com o jogo
     * aberto: sem isso, os clipes antigos continuariam apontando para o
     * dispositivo anterior.
     */
    public static void recarregar() {
        for (Clip clip : CLIPS.values()) {
            try {
                clip.close();
            } catch (Exception ignorada) {
                // Fechar um clip já morto não impede a recarga.
            }
        }
        CLIPS.clear();
        PENDENTES.clear();
        mixerEscolhido = null;
        audioIndisponivel = false;
        ultimoErro = "";
    }

    /**
     * Recarrega o áudio se a configuração de som do sistema tiver mudado.
     * Chamado a cada toque de tecla do jogo: quando o jogador encaixa um fone
     * ou conecta uma caixa USB, o Windows troca a saída e os clipes em cache
     * passam a apontar para um dispositivo morto. A verificação é barata — só
     * compara a lista de mixers com a da última checagem.
     */
    public static void verificarMudancaDeDispositivo() {
        List<Mixer> atuais;
        try {
            atuais = mixersOrdemDePreferencia();
        } catch (Exception ignorada) {
            return;
        }
        if (!mesmaOrdem(atuais, ultimaVerificacao)) {
            ultimaVerificacao = atuais;
            recarregar();
        }
    }

    /**
     * @param a primeira lista de mixers
     * @param b segunda lista de mixers
     * @return {@code true} quando as listas têm os mesmos nomes na mesma ordem
     */
    private static boolean mesmaOrdem(List<Mixer> a, List<Mixer> b) {
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!nome(a.get(i)).equals(nome(b.get(i)))) {
                return false;
            }
        }
        return true;
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
            // Primeira vez: carrega em segundo plano e toca assim que ficar
            // pronto, para não travar a EDT com a sondagem de mixers.
            PENDENTES.add(som);
            carregarEmSegundoPlano(som);
            return;
        }
        if (!reproduzir(clip)) {
            // O clipe estava preso a um dispositivo que saiu. Descarta e tenta
            // resolver o áudio de novo em vez de ficar mudo para sempre.
            PENDENTES.add(som);
            CLIPS.remove(som);
            carregarEmSegundoPlano(som);
        }
    }

    /**
     * Agenda a carga de um efeito na thread de áudio.
     *
     * @param som efeito a carregar
     */
    private static void carregarEmSegundoPlano(final Som som) {
        if (!CARREGANDO.add(som)) {
            return;
        }
        CARREGADOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Clip clip = carregar(som.caminho);
                    if (clip != null) {
                        CLIPS.put(som, clip);
                        if (ligado && PENDENTES.remove(som)) {
                            reproduzir(clip);
                        }
                    } else {
                        PENDENTES.remove(som);
                    }
                } finally {
                    CARREGANDO.remove(som);
                }
            }
        });
    }

    /**
     * Toca um clipe já carregado, avisando se ele não respondeu.
     *
     * @param clip clipe em cache
     * @return {@code false} quando a linha não pôde ser tocada
     */
    private static boolean reproduzir(Clip clip) {
        try {
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lê e prepara um clip de áudio a partir do recurso no classpath. O ganho é
     * reduzido para −9 dB para que os sons não soem estridentes.
     *
     * <p>Um clip só é aceito depois de tocados de verdade: linhas abertas em
     * dispositivo unplugado aceitam {@code start()} e ficam inativas sem erro,
     * e aceitar essas linhas é justamente o que deixava o jogo mudo em silêncio.
     *
     * @param caminho caminho do recurso dentro de {@code /sons/}
     * @return clip pronto para tocar, ou {@code null} se não for possível carregar
     */
    private static Clip carregar(String caminho) {
        URL url = EfeitosSonoros.class.getResource(caminho);
        if (url == null) {
            ultimoErro = "recurso ausente: " + caminho;
            audioIndisponivel = true;
            return null;
        }

        StringBuilder falhas = new StringBuilder();
        for (Mixer mixer : candidatos()) {
            Clip clip = tentarAbrir(url, mixer, falhas);
            if (clip != null) {
                mixerEscolhido = mixer;
                audioIndisponivel = false;
                ultimoErro = "";
                return clip;
            }
        }

        audioIndisponivel = true;
        ultimoErro = falhas.toString();
        return null;
    }

    /**
     * Monta a lista de mixers a tentar: o já resolvido, o padrão do sistema e
     * depois todos os demais, sem repetir. Testar todos é o que permite
     * recuperar o som depois de uma troca de dispositivo.
     *
     * @return mixers em ordem de preferência
     */
    static List<Mixer> mixersOrdemDePreferencia() {
        List<Mixer> candidatos = new ArrayList<Mixer>();
        if (mixerEscolhido != null) {
            candidatos.add(mixerEscolhido);
        }
        Mixer padrao = mixerPadrao();
        if (padrao != null && !candidatos.contains(padrao)) {
            candidatos.add(padrao);
        }
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            if (!candidatos.contains(mixer)) {
                candidatos.add(mixer);
            }
        }
        return candidatos;
    }

    /**
     * @return o mixer padrão do sistema, ou {@code null} se a máquina não tiver
     *         nenhum dispositivo de saída
     */
    static Mixer mixerPadrao() {
        try {
            return AudioSystem.getMixer(null);
        } catch (Exception ignorada) {
            return null;
        }
    }

    /**
     * Tenta abrir e tocar o som em um mixer específico, devolvendo o clip só
     * se ele realmente produzir áudio.
     *
     * @param url    recurso do arquivo de áudio
     * @param mixer  mixer a tentar, {@code null} para o padrão do sistema
     * @return clip validado, ou {@code null} se este mixer não serviu
     */
    private static Clip tentarAbrir(URL url, Mixer mixer, StringBuilder falhas) {
        AudioInputStream fluxo = null;
        Clip clip = null;
        boolean aceito = false;
        try {
            // O stream precisa de mark/reset: é assim que o Java Sound lê o
            // cabeçalho para descobrir o formato. O stream cru de um JAR não
            // suporta mark, e sem o BufferedInputStream a leitura falha com
            // "mark/reset not supported" antes mesmo de abrir a linha. Além
            // disso, cada tentativa precisa de um stream novo, porque o
            // consumo da anterior a deixa esgotada.
            fluxo = AudioSystem.getAudioInputStream(new BufferedInputStream(url.openStream()));
            AudioFormat formato = fluxo.getFormat();
            clip = fabrica.abrir(mixer, formato);
            clip.open(fluxo);
            aplicarGanho(clip);

            if (!tocouDeVerdade(clip)) {
                registrar(falhas, nome(mixer) + ": linha aberta mas sem audio");
                return null;
            }
            aceito = true;
            return clip;
        } catch (Exception e) {
            registrar(falhas, nome(mixer) + ": " + e);
            return null;
        } finally {
            // Só o clip recusado é fechado. Um clip aceito continua aberto e
            // em cache para as próximas tocadas.
            if (!aceito) {
                fechar(fluxo, clip);
            }
        }
    }

    /**
     * Guarda uma falha de tentativa e libera o clip recusado, se houver.
     *
     * @param falhas acumulador com o motivo de cada mixer descartado
     * @param motivo motivo pelo qual o mixer não serviu
     */
    private static void registrar(StringBuilder falhas, String motivo) {
        if (falhas.length() > 0) {
            falhas.append(" | ");
        }
        falhas.append(motivo);
    }

    /**
     * Verifica se o clip realmente saiu. Uma linha presa em dispositivo morto
     * aceita {@code start()} e permanece inativa; uma linha sãa fica ativa.
     *
     * @param clip clip recém-aberto e já iniciado
     * @return {@code true} se o áudio começou a tocar
     */
    private static boolean tocouDeVerdade(Clip clip) {
        try {
            // O teste roda com o ganho no mínimo: confirma que a linha produz
            // áudio sem que o jogador ouça um som extra na primeira vez.
            float ganhoOriginal = silenciar(clip);
            try {
                clip.setFramePosition(0);
                clip.start();
                Thread.sleep(VERIFICACAO_MS);
                return clip.isActive();
            } finally {
                clip.stop();
                clip.setFramePosition(0);
                restaurarGanho(clip, ganhoOriginal);
            }
        } catch (InterruptedException restaurada) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Baixa o ganho do clipe ao mínimo, se o controle existir.
     *
     * @param clip clip aberto
     * @return ganho aplicado, ou {@link Float#NaN} se não houver controle
     */
    private static float silenciar(Clip clip) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl ganho =
                        (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float original = ganho.getValue();
                ganho.setValue(ganho.getMinimum());
                return original;
            }
        } catch (Exception ignorada) {
            // Sem controle de ganho: o teste roda com volume normal.
        }
        return Float.NaN;
    }

    /**
     * Devolve o ganho do clipe ao valor anterior ao teste.
     *
     * @param clip         clip aberto
     * @param ganhoOriginal ganho devolvido por {@link #silenciar(Clip)}
     */
    private static void restaurarGanho(Clip clip, float ganhoOriginal) {
        if (Float.isNaN(ganhoOriginal)) {
            return;
        }
        try {
            FloatControl ganho = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            ganho.setValue(ganhoOriginal);
        } catch (Exception ignorada) {
            // Ganho indisponível não impede o som de tocar.
        }
    }

    /**
     * Reduz o ganho do clip para −9 dB para que os sons não soem estridentes.
     * Ganho indisponível não impede o som de tocar.
     *
     * @param clip clip aberto
     */
    private static void aplicarGanho(Clip clip) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl ganho = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                ganho.setValue(Math.max(ganho.getMinimum(), -9.0f));
            }
        } catch (Exception ignorada) {
            // Ganho indisponível não impede o som de tocar.
        }
    }

    /**
     * @param mixer mixer usado, {@code null} para o padrão do sistema
     * @return nome legível do mixer para mensagens de erro
     */
    private static String nome(Mixer mixer) {
        if (mixer == null) {
            return "mixer padrao do sistema";
        }
        return mixer.getMixerInfo().getName();
    }

    /**
     * Fecha o clip e o fluxo sem deixar exceção escapar, para não mascarar o
     * motivo real da falha.
     *
     * @param fluxo stream do arquivo de áudio
     * @param clip  clip aberto, {@code null} se nem chegou a abrir
     */
    private static void fechar(AudioInputStream fluxo, Clip clip) {
        if (clip != null) {
            try {
                clip.close();
            } catch (Exception ignorada) {
                // Clip já morto não impede o fechamento do fluxo.
            }
        }
        if (fluxo != null) {
            try {
                fluxo.close();
            } catch (Exception ignorada) {
                // Sem ação possível.
            }
        }
    }

    /**
     * Substitui a fábrica de linhas e devolve a instância anterior. Usado
     * apenas pelos testes para simular ausência de áudio.
     *
     * @param novaFabrica fábrica a usar, {@code null} restaura a real
     * @return a fábrica que estava em uso
     */
    static FabricaDeLinha trocarFabrica(FabricaDeLinha novaFabrica) {
        FabricaDeLinha anterior = fabrica;
        fabrica = novaFabrica == null ? FABRICA_REAL : novaFabrica;
        return anterior;
    }

    /**
     * Substitui a lista de mixers candidatos. Usado apenas pelos testes.
     *
     * @param mixers mixers a considerar, pode ser vazio
     */
    static void definirMixers(List<Mixer> mixers) {
        candidatosForcados = mixers;
    }

    /** Mixers injetados pelos testes; {@code null} usa a ordem normal. */
    private static List<Mixer> candidatosForcados;

    /** Ordem de mixers vista na última verificação de dispositivo. */
    private static List<Mixer> ultimaVerificacao;

    /**
     * @return lista de mixers a tentar, respeitando a injeção dos testes
     */
    private static List<Mixer> candidatos() {
        if (candidatosForcados != null) {
            return candidatosForcados;
        }
        return mixersOrdemDePreferencia();
    }
}
