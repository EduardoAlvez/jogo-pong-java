package com.portfolio.pong.skin;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Catálogo das skins do Pong: seis temas fixos mais a personalizada, com
 * persistência no arquivo {@code ~/.jogo-pong-skin.properties}.
 *
 * <p>A persistência guarda o nome da skin selecionada e, quando o usuário
 * personaliza, as cores escolhidas e o rótulo da bola. Se o arquivo estiver
 * corrompido ou faltando, o catálogo cai sempre no preset Clássico.</p>
 *
 * @author Eduardo Alvez
 */
public class CatalogoSkins {

    private static final String ARQUIVO = ".jogo-pong-skin.properties";

    private final List<Skin> presets = new ArrayList<>();
    private final Properties propriedades = new Properties();
    private final Skin classico;

    /** Cores base da skin clássica (fallback de segurança). */
    private static final Color COR_CAMP0_CLASSICO = new Color(0x1E7A3C);
    private static final Color COR_LINHA_CLASSICO = Color.WHITE;
    private static final Color COR_BORDA_CLASSICO = Color.WHITE;
    private static final Color COR_R1_CLASSICO = Color.WHITE;
    private static final Color COR_R2_CLASSICO = Color.WHITE;
    private static final Color COR_BOLA_CLASSICO = Color.WHITE;
    private static final Color COR_QUEIMADO_CLASSICO = new Color(0x1A2B20);

    public CatalogoSkins() {
        classico = criarPreset("Clássico", COR_CAMP0_CLASSICO, COR_LINHA_CLASSICO,
                COR_BORDA_CLASSICO, COR_R1_CLASSICO, COR_R2_CLASSICO,
                COR_BOLA_CLASSICO, COR_QUEIMADO_CLASSICO, "", false);

        presets.add(classico);
        presets.add(criarPreset("Neon",
                new Color(0x0B0B1A), new Color(0x00F6FF), new Color(0x4D1B7A),
                new Color(0x00F6FF), new Color(0xFF00E5),
                new Color(0xFFF700), new Color(0x173A5E), "", false));
        presets.add(criarPreset("Retrô",
                Color.BLACK, new Color(0x00FF41), new Color(0x00FF41),
                new Color(0x00FF41), new Color(0x00FF41),
                new Color(0x00FF41), new Color(0x0B3B1D), "", true));
        presets.add(criarPreset("Oceano",
                new Color(0x0A3D62), Color.WHITE, new Color(0x85C1E9),
                new Color(0x85C1E9), new Color(0x48C9B0),
                new Color(0xFDFEFE), new Color(0x0E2A40), "", false));
        presets.add(criarPreset("Sunset",
                new Color(0xA93226), new Color(0xF5B041), new Color(0x6E2C00),
                new Color(0xF1C40F), new Color(0xF39C12),
                new Color(0xFFF8DC), new Color(0x4A160F), "", false));
        presets.add(criarPreset("Floresta",
                new Color(0x145A32), new Color(0xD4E6F1), new Color(0x0E3B21),
                new Color(0x27AE60), new Color(0xA9DFBF),
                new Color(0xFDFEFE), new Color(0x0A2E18), "", false));

        // Carregar o arquivo (~/.jogo-pong-skin.properties), se existir.
        if (carregarArquivo()) {
            String nome = propriedades.getProperty("skin.selecionada");
            selecionada = nome != null ? nome : classico.getNome();
        } else {
            selecionada = classico.getNome();
        }
    }

    private String selecionada;

    /** @return nomes das skins disponíveis no seletor (presets + personalizada) */
    public String[] getNomes() {
        String[] nomes = new String[presets.size() + 1];
        for (int i = 0; i < presets.size(); i++) {
            nomes[i] = presets.get(i).getNome();
        }
        nomes[presets.size()] = Skin.PERSONALIZADA;
        return nomes;
    }

    /** @return skin atualmente selecionada (com fallback para Clássico) */
    public Skin obterSelecionada() {
        if (Skin.PERSONALIZADA.equals(selecionada)) {
            return getPersonalizada();
        }
        Skin preset = buscarPorNome(selecionada);
        return preset != null ? preset : classico;
    }

    /** Seleciona uma skin pelo nome e persiste a escolha. */
    public void selecionar(String nome) {
        if (nome == null || (!buscarValido(nome))) {
            return;
        }
        selecionada = nome;
        salvarArquivo();
    }

    /**
     * Aplica a skin personalizada com as cores escolhidas, salva no arquivo e
     * seleciona "Personalizada".
     */
    public void aplicarPersonalizada(Color corCampo, Color corLinha, Color corBorda,
                                     Color corRaquete1, Color corRaquete2, Color corBola,
                                     String rotuloBola) {
        propriedades.setProperty("custom.campo", toHex(corCampo));
        propriedades.setProperty("custom.linha", toHex(corLinha));
        propriedades.setProperty("custom.borda", toHex(corBorda));
        propriedades.setProperty("custom.raquete1", toHex(corRaquete1));
        propriedades.setProperty("custom.raquete2", toHex(corRaquete2));
        propriedades.setProperty("custom.bola", toHex(corBola));
        propriedades.setProperty("custom.rotulo", rotuloBola == null ? "" : rotuloBola);
        selecionada = Skin.PERSONALIZADA;
        salvarArquivo();
    }

    /** @return a skin personalizada construída das propriedades salvas (fallback Clássico) */
    public Skin getPersonalizada() {
        Color campo = getCor("custom.campo", COR_CAMP0_CLASSICO);
        Color linha = getCor("custom.linha", COR_LINHA_CLASSICO);
        Color borda = getCor("custom.borda", COR_BORDA_CLASSICO);
        Color r1 = getCor("custom.raquete1", COR_R1_CLASSICO);
        Color r2 = getCor("custom.raquete2", COR_R2_CLASSICO);
        Color bola = getCor("custom.bola", COR_BOLA_CLASSICO);
        String rotulo = propriedades.getProperty("custom.rotulo", "");
        return new Skin(Skin.PERSONALIZADA, campo, linha, borda, r1, r2, bola,
                COR_QUEIMADO_CLASSICO, rotulo, false);
    }

    /** @return presets atuais (para prévia no briefing) */
    public List<Skin> getPresets() {
        return List.copyOf(presets);
    }

    private Skin buscarPorNome(String nome) {
        for (Skin s : presets) {
            if (s.getNome().equals(nome)) {
                return s;
            }
        }
        return null;
    }

    /** Nome é um preset válido ou a personalizada. */
    private boolean buscarValido(String nome) {
        return Skin.PERSONALIZADA.equals(nome) || buscarPorNome(nome) != null;
    }

    private Color getCor(String chave, Color padrao) {
        String valor = propriedades.getProperty(chave);
        if (valor == null) {
            return padrao;
        }
        try {
            return parseColor(valor);
        } catch (IllegalArgumentException ex) {
            return padrao;
        }
    }

    private boolean carregarArquivo() {
        Path arquivo = caminhoArquivo();
        if (!Files.exists(arquivo)) {
            return false;
        }
        try (InputStream in = Files.newInputStream(arquivo)) {
            propriedades.load(in);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private void salvarArquivo() {
        try {
            propriedades.setProperty("skin.selecionada", selecionada);
            Path arquivo = caminhoArquivo();
            Path dir = arquivo.getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            try (var out = Files.newOutputStream(arquivo)) {
                propriedades.store(out, "Preferências de skin do Jogo Pong");
            }
        } catch (IOException ex) {
            // Persistência é best-effort: ignora falha e mantém sessão atual.
        }
    }

    private static Path caminhoArquivo() {
        return Paths.get(System.getProperty("user.home", ".")).resolve(ARQUIVO);
    }

    private static Skin criarPreset(String nome, Color campo, Color linha, Color borda,
                                    Color r1, Color r2, Color bola, Color queimado,
                                    String rotulo, boolean scanlines) {
        return new Skin(nome, campo, linha, borda, r1, r2, bola, queimado, rotulo, scanlines);
    }

    /** Converte uma cor no formato hex {@code "#RRGGBB"}. */
    public static String toHex(Color cor) {
        return String.format("#%02X%02X%02X", cor.getRed(), cor.getGreen(), cor.getBlue());
    }

    /** Converte {@code "#RRGGBB"} (ou "RRGGBB") em cor. Lança exceção se inválido. */
    public static Color parseColor(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("hex nulo");
        }
        String limpo = hex.trim();
        if (limpo.isEmpty()) {
            throw new IllegalArgumentException("hex vazio");
        }
        if (limpo.charAt(0) == '#') {
            limpo = limpo.substring(1);
        }
        if (limpo.length() != 6) {
            throw new IllegalArgumentException("esperado #RRGGBB: " + hex);
        }
        return new Color(
                Integer.valueOf(limpo.substring(0, 2), 16),
                Integer.valueOf(limpo.substring(2, 4), 16),
                Integer.valueOf(limpo.substring(4, 6), 16));
    }
}