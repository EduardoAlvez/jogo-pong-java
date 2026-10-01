# Jogo Pong 🏓

Pong personalizável em **Java Swing**, desenvolvido como projeto de portfólio.
Cronômetro estilo futebol, bola que **pega fogo**, skins e animações de partículas.

<p align="center">
  <em>P1 | ⏱ 02:00 | P2 — quem manda no placar?</em>
</p>

## ⬇️ Baixar

| | Arquivo | Observação |
|---|---|---|
| **Windows** | [`jogo-pong-1.0.exe`](https://github.com/EduardoAlvez/jogo-pong-java/releases/tag/v1.0.0) | Arquivo único, com o logo. **Precisa de Java 17+ instalado.** |
| **Outros** | [`jogo-pong-1.0.jar`](https://github.com/EduardoAlvez/jogo-pong-java/releases/tag/v1.0.0) | `java -jar jogo-pong-1.0.jar` |

O executável não embute a JRE para não pesar centenas de MB: se não encontrar
nenhum Java instalado, ele mostra uma caixa de erro apontando para o download.

> O ícone do `.jar` é o do Windows, não o do jogo — um `.jar` é um ZIP, e o
> Windows usa o ícone da associação de arquivos. Por isso existe o `.exe`.

---

## ✨ Funcionalidades

### Jogabilidade
- **2 modos**:
  - **Modo Tempo (estilo futebol)**: cronômetro de **1, 2 ou 3 minutos** na tela;
    conforme o tempo esgota, **a bola acelera progressivamente**. Acabou o tempo,
    vence quem tem mais pontos. Empate → **gol de ouro** (morte súbita).
  - **Modo Clássico**: sem relógio, primeiro a **5, 7 ou 10 pontos** (escolha sua).
- **1P vs Computador** (dificuldade Fácil/Médio/Difícil) ou **2P local**
- **Física clássica**: ângulo de saída varia conforme o ponto de contato na
  raquete, bola acelera a cada rebatida (+8% composto, com teto de 900 px/s)
  e **volta à velocidade base (240 px/s) a cada gol**
- **Pausa de 1s após o gol** para a animação terminar antes do novo saque
- **Multiball**: até 2 bolas em campo; a IA defende a bola que chega primeiro
- **Especial 🔥 recarregável**: bola pega fogo automaticamente ao ficar veloz e
  cada jogador tem uma **bateria de 3 cargas**, que sobe **+1 a cada rebatida da
  própria raquete** (não carrega durante o próprio especial, persiste após gol).
  Ao usar (Z = P1, M = P2) a bola vai a **chamas + velocidade ×2** por 5s.
  Só entra com **pelo menos uma bola no seu campo** (do meio para o seu lado) e
  **um especial por vez** — o outro jogador espera o de quem está usando acabar
- **Prêmios no campo (estilo Mario)**: cards coloridos que pulsam e somem em 9s,
  surgindo em **levas de 1 a 3** (1ª aos 8s, depois a cada 10–16s). A coleta é
  **pela bola** e vale para quem rebateu por último (ou para o lado que a bola
  está indo, no saque):

  | Prêmio | Efeito | Duração |
  |---|---|---|
  | ↕ Inverter | controles do adversário (só 2P) | 10s |
  | ❄ Congelar | raquete adversária travada | 4s |
  | ⚡ Turbo | +40% na própria raquete | 10s |
  | 🔥 Chamas | fogo, sem gastar o especial | 6s |
  | ⏱ +10s | só no modo Tempo | — |
  | ▮ Encolher | raquete adversária vai a 60px | 10s |
  | 🔵 +1 bola | até o próximo gol (máx. 2) | — |
  | ? Coringa | re-sorteia um efeito real | — |

  Sem prêmios durante o gol de ouro, e o Inverter não aparece no 1P (contra a CPU)
- **Sistema de partículas**: rastro de fogo, explosão no gol, tremor de tela e
  fundo animado

### Personalização (skins)
- **6 presets** com preview animado: Clássico, Neon, Retrô/fósforo, Oceano,
  Sunset e Floresta (o efeito CRT/scanline é exclusivo da Retrô)
- **Skin Personalizada**: escolha as cores (fundo, linhas, raquetes e bola) e
  ainda defina um **emoji/letra** para cada raquete e para a bola; fica salva
  em `~/.jogo-pong-skin.properties` e é recarregada no próximo início

### Interface
- **Placar estilo futebol**: `P1 | cronômetro | P2` no topo central, números
  grandes com brilho; relógio **fica vermelho e pisca** nos últimos 10 segundos
- **Briefing antes da partida** com countdown 3-2-1-JÁ!
- **Pausa real** (overlay Retomar/Reiniciar/Voltar) e **tela de fim com stats**:
  placar, duração, melhor troca e velocidade máxima atingida
- **Indicador do especial 🔥** (3 traços por jogador: verde/amarelo/vermelho)
  mostrando carga disponível e duração restante
- **7 efeitos sonoros** (bola, parede, rebatida, gol, especial, prêmio, fim de
  partida) com interruptor 🔊/🔇
- Janela redimensionável, controles por teclado (e mouse nos menus)

---

## 🚀 Como executar

### Pré-requisitos
- **JDK 17** ou superior
- (Opcional) Maven 3.x

### Opção 1 — Compilação manual (sem Maven)

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")
java -cp out com.portfolio.pong.ui.TelaPong
```

### Opção 2 — Maven

```bash
mvn clean compile exec:java
```

### Opção 3 — IDE
Abra como projeto Maven no IntelliJ IDEA ou Eclipse e execute
`com.portfolio.pong.ui.TelaPong`.

### Opção 4 — Gerar o executável para Windows

```bash
mvn clean package
```

Produz dois artefatos em `target/`:

| Arquivo | Tamanho | Para quê |
|---|---|---|
| `jogo-pong.exe` | 364 250 bytes | Executável com o logo embutido, para distribuir |
| `jogo-pong-1.0.jar` | 241 882 bytes | Rodar com `java -jar`, útil em outras plataformas |

> Note o nome: ao compilar, o executável sai como `jogo-pong.exe`, sem a
> versão; na release ele é publicado como `jogo-pong-1.0.exe`. O `.jar` já sai
> versionado nos dois casos. É só o launch4j dar o nome ao executável, e o
> `1.0` na release é o que identifica a versão para quem baixa.

O `.exe` é um único arquivo: o jar fica embutido dentro dele e a JRE **não**,
então quem abre precisa ter Java 17+ instalado. Sem Java, o executável mostra
uma caixa de erro apontando para a página de download.

Para pular a geração do executável (build mais rápido, ou em CI):

```bash
mvn clean package -DskipLaunch4j
```

#### Os efeitos sonoros no executável

Rodando o jogo direto do Maven ou de um `.jar` solto, os 7 efeitos tocam sem
problema. Distribuído — dentro do `.jar` que o `.exe` embute — o jogo ficava
**completo e mudo**, sem mensagem de erro.

O motivo: `AudioSystem.getAudioInputStream()` precisa de `mark/reset` para ler o
cabeçalho do `.wav` e descobrir o formato. Um `file:` URL (recurso solto em disco)
já entrega um `BufferedInputStream` e nunca mostra o problema; a entrada de um
`jar` entrega o stream cru, que não aceita `mark`, e a leitura falha com
`mark/reset not supported` antes de qualquer linha de áudio ser aberta.

O caminho de leitura embrulha o stream em `BufferedInputStream`, e a suíte cobre
o caso: o teste grava uma das WAVs em um jar de verdade e a passa pelo mesmo
caminho de leitura do jogo. Sem o buffer, ele falha.

> Um detalhe que engana: rodar `mvn test` **não** reproduz o defeito, porque no
> classpath de teste os recursos são arquivos soltos. Só a leitura de dentro de um
> jar expõe o problema.

Além disso, a escolha do dispositivo de saída é explícita: o mixer padrão do
Java fica preso ao dispositivo escolhido na primeira vez, e uma linha aberta em
um dispositivo que o Windows removeu aceita `start()` e fica inativa sem erro.
Por isso o áudio é testado de verdade antes de ser aceito, e recarrega sozinho
quando a lista de dispositivos do sistema muda.

#### Por que existe um `.exe` se o projeto já tem `.jar`

Um `.jar` **não consegue** ter ícone próprio. Ele é um arquivo ZIP (começa com
`PK`) e o Windows tira o ícone que aparece no Explorer da *associação de
arquivos* do sistema, não do conteúdo do arquivo. Nenhum atributo de manifesto
muda isso — é limitação do Windows, não configuração faltando.

Por isso existem dois ícones, com caminhos diferentes:

- **ícone da janela** — `logo-*.png` + `setIconImages` em `TelaPong`; o Windows
  escolhe a resolução certa para cada contexto (barra de tarefas, Alt+Tab, title)
- **ícone do arquivo** — `logo.ico` dentro do `.exe`, gerado pelo
  `launch4j-maven-plugin` durante o `package`

#### Regenerar os ícones

O logo mestre é `src/main/resources/logo-256.png`. As resoluções menores e o
`.ico` saem dele pela ferramenta:

```bash
java tools/GerarLogo.java
```

Sem dependência externa: o JDK não tem writer de ICO no `ImageIO`, então
`GerarLogo` monta o arquivo `.ico` na mão (entradas BMP com máscara AND até
48px, PNG comprimido acima disso). O `.ico` é formato little-endian; a
ferramenta escreve os inteiros byte a byte por causa disso.

---

## 🎮 Como jogar

1. No **briefing**, escolha o modo (Tempo/Clássico), a duração ou o placar-alvo,
   a dificuldade (vs Computador) e a skin
2. Você também pode montar sua própria skin na opção "Personalizada"
3. Pressione Iniciar: countdown **3, 2, 1, JÁ!** e a partida começa
4. Controles: **W/S** (P1) e **↑/↓** (P2); **Espaço** pausa; **Z** e **M** = especial
5. Acelere a bola acertando pelas bordas da raquete — cuidado, ela volta mais veloz
6. Modo Tempo: quando o cronômetro zerar empatado, vale **gol de ouro**
7. Fim de partida: confira as **stats** e jogue novamente ou volte ao início

---

## 🏗️ Estrutura do projeto

```
jogo-pong/
├── pom.xml                                  # Build Maven (Java 17, mainClass: TelaPong)
├── KANBAN.md                                # Quadro de tarefas do projeto
├── tools/
│   └── GerarLogo.java                       # Regera os PNGs e o logo.ico a partir do mestre
├── src/
│   ├── main/java/com/portfolio/pong/
│   │   ├── core/                            # Núcleo puro e testável (sem Swing)
│   │   │   ├── Bola.java                    # Física da bola (ângulos, aceleração, teto)
│   │   │   ├── Raquete.java                 # Movimento e limites
│   │   │   ├── Computador.java              # IA batedível (3 dificuldades)
│   │   │   ├── Cronometro.java              # Temporizador dos modos Tempo/gol de ouro
│   │   │   ├── Premio.java                   # Prêmios do campo (tipo, validade, posição)
│   │   │   └── Pong.java                    # Estado: modo, placar, especial, prêmios, stats
│   │   ├── skin/                            # Dados visuais e catálogo
│   │   │   ├── Skin.java                    # Cores, formato, emoji
│   │   │   └── CatalogoSkins.java           # Presets + persistência da personalizada
│   │   ├── fx/                              # Efeitos e animações
│   │   │   ├── Particula.java               # Partícula do motor de animação
│   │   │   └── Animacoes.java               # Fogo, queimado na mesa, explosão, tremor
│   │   ├── audio/                           # Efeitos sonoros
│   │   │   └── EfeitosSonoros.java          # Sons .wav (bola, gol, especial...)
│   │   └── ui/                              # Interface gráfica Swing
│   │       └── TelaPong.java                # Briefing, jogo, pausa, fim, HUD
│   ├── main/resources/
│   │   ├── logo-{16,24,32,48,64,128,256}.png  # Ícone da janela, uma por resolução
│   │   ├── logo.ico                          # Ícone do executável, gerado pelo GerarLogo
│   │   └── sons/                             # 7 arquivos .wav sintetizados
│   └── ...
├── out/                                     # Saída da compilação manual (ignorada no git)
└── target/                                  # Saída do Maven (ignorada no git)
    ├── jogo-pong.exe                        # Executável de arquivo único (launch4j)
    └── jogo-pong-1.0.jar
```

### Principais classes

| Classe | Camada | Responsabilidade |
|--------|--------|------------------|
| `core.Bola` | core | Posição/velocidade, reflexões, ângulo por ponto de contato, aceleração |
| `core.Raquete` | core | Movimento e limites da raquete |
| `core.Computador` | core | IA com velocidade limitada, reação e margem de erro |
| `core.Cronometro` | core | Contagem regressiva, fim de tempo e gol de ouro |
| `core.Premio` | core | Prêmios do campo: tipo, cor, validade (9s) e posição |
| `core.Pong` | core | Estado do jogo: modo, placar, sacada, especial, prêmios, pausa pós-gol e estatísticas |
| `skin.Skin` / `skin.CatalogoSkins` | skin | Paleta e formato do jogo, presets e skin personalizada persistida |
| `fx.Animacoes` | fx | Motor de partículas: fogo, queimado na mesa, explosão, confete |
| `audio.EfeitosSonoros` | audio | Sons .wav com mudo global |
| `ui.TelaPong` | ui | Interface: briefing, jogo, pausa, fim, HUD, placar e relógio |

---

## 🛠️ Tecnologias

- **Java 17+** (compatível com versões superiores)
- **Swing** (`JFrame`, `JPanel`, `KeyBindings`)
- **Graphics2D** (física desenhada, partículas, glow e scanlines)
- **`javax.sound.sampled`** (efeitos sonoros)
- **Maven** (build e gerenciamento de dependências)

---

## 🧠 Notas de implementação

- **Núcleo separado da UI**: `Bola`, `Raquete`, `Computador`, `Cronometro` e
  `Pong` não dependem de Swing — a lógica é testável isoladamente com JUnit.
- **Especial testável**: a condição de "bola em chamas" é decidida por
  velocidade no núcleo; o desenho das chamas/queimado fica na camada gráfica.
- **Queimado na mesa**: camada offscreen onde a bola em chamas deixa marcas
  persistentes que acumulam durante a partida.
- **Persistência da skin**: arquivo `~/.jogo-pong-skin.properties`; se
  corrompido, cai no preset Clássico.
- **Campo fixo escalado**: o jogo roda em 800×500 e é escalado (letterbox)
  para caber na janela — redimensionável sem alterar a física.
- **Sons sintetizados em código**: os `.wav` (rebater, parede, gol, especial,
  clique, vitória, prêmio) são gerados por síntese PCM 16-bit 44,1 kHz mono.
- **Cobertura não é o mesmo que casos piores cobertos**: a suíte mede quantidade
  de caminhos, não os piores cenários. Já deixou passar uma sobreposição de
  especiais dos dois jogadores — a bola ficava presa no dobro da velocidade para
  o resto da partida — porque todos os testes de `usarEspecial` usavam
  `jogador == 1`. A regra adotada: para cada regra nova, escrever o cenário
  simultâneo (os dois jogadores, duas bolas) e o de reversão (o estado volta ao
  normal quando o efeito acaba), além do caminho feliz isolado.

---

## 📌 Roadmap (Kanban)

- [x] Scaffold do projeto (pom.xml, README, KANBAN, LICENSE, estrutura por camadas)
- [x] Núcleo: Raquete / Bola / Computador / Cronometro / Pong
- [x] Especial bola de fogo + queimado na mesa
- [x] Skins (presets + personalizada persistida)
- [x] Motor de partículas e animações
- [x] TelaPong: briefing, jogo, pausa, fim + HUD futebol
- [x] Efeitos sonoros (corrigidos para tocar de dentro do `.jar`/`.exe`)
- [x] Testes unitários — 180 testes (branch `testes-jogo-pong`, mergeada na `main`)
- [x] Hover dos botões — o destaque nunca acendia em jogo (`Botao`/`Botaos` extraídos de `TelaPong`)
- [x] Prêmios no gol de ouro — o card em campo não congela mais
- [x] Publicar no GitHub e lançar a **v1.0.0** (`main` + `testes-jogo-pong`)

### Em aberto (ver [`KANBAN.md`](KANBAN.md))

- [ ] **Badges de efeito somem com a raquete no topo** — defeito confirmado: o
      primeiro badge é desenhado 20px acima da raquete e cada um sobe mais 21px;
      com a raquete em `y = 0` e os 5 efeitos ativos o topo chega a y = −104 e
      nenhum aparece. Sem `clip`, e sem exceção, então nada acusa
- [ ] Layout sem teste — `ui/` tem `BotaosTest` (o hover), mas a **geometria** do
      desenho ainda não tem cobertura: badges e cards de prêmio dependem de
      decisões tomadas dentro do `paintComponent`
- [ ] Polir a IA com 2 bolas (adiado)

O ícone na barra de tarefas/Alt+Tab e o feeling da janela de 5s do especial já
foram conferidos e aprovados.

---

## 👤 Autor

**Eduardo Alvez** — [byteswood@gmail.com](mailto:byteswood@gmail.com)

Projeto de portfólio — Pong em Java Desktop.