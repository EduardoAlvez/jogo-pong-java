# Jogo Pong 🏓

Pong personalizável em **Java Swing**, desenvolvido como projeto de portfólio.
Cronômetro estilo futebol, bola que **pega fogo**, skins e animações de partículas.

<p align="center">
  <em>P1 | ⏱ 02:00 | P2 — quem manda no placar?</em>
</p>

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
  raquete, bola acelera a cada rebatida (+6%, com teto)
- **Especial 🔥**: a bola pega fogo automaticamente ao ficar veloz, e cada
  jogador ainda tem **1 uso da tecla especial** (Z = P1, M = P2) por partida —
  visual-only, mas **deixa o rastro de queimado na mesa**
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
- **Indicador do especial 🔥** mostrando disponibilidade e duração restante
- **Efeitos sonoros** (bola, batida, gol, especial, fim de partida) com 🔊/🔇
- Janela redimensionável, controles por teclado (e mouse nos menus)

---

## 🚀 Como executar

### Pré-requisitos
- **JDK 8** ou superior
- (Opcional) Maven 3.x

### Opção 1 — Compilação manual (sem Maven)

```bash
mkdir -p out
javac -encoding UTF-8 -d out src/main/java/com/portfolio/pong/*.java
java -cp out com.portfolio.pong.TelaPong
```

### Opção 2 — Maven

```bash
mvn clean compile exec:java
```

### Opção 3 — IDE
Abra como projeto Maven no IntelliJ IDEA ou Eclipse e execute
`com.portfolio.pong.TelaPong`.

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
├── pom.xml                                  # Build Maven (Java 8, mainClass: TelaPong)
├── KANBAN.md                                # Quadro de tarefas do projeto
├── src/
│   ├── main/java/com/portfolio/pong/
│   │   ├── Bola.java                        # Física da bola (ângulos, aceleração, teto)
│   │   ├── Raquete.java                     # Movimento e limites
│   │   ├── Computador.java                  # IA batedível (3 dificuldades)
│   │   ├── Cronometro.java                  # Temporizador dos modos Tempo/gol de ouro
│   │   ├── Pong.java                        # Estado: modo, placar, especial, stats
│   │   ├── Skin.java                        # Dados visuais (cores, formato, emoji)
│   │   ├── CatalogoSkins.java               # Presets + persistência da personalizada
│   │   ├── Particula.java                   # Partícula do motor de animação
│   │   ├── Animacoes.java                   # Fogo, rastro de queimado, explosão, tremor
│   │   ├── EfeitosSonoros.java              # Sons .wav (bola, gol, especial...)
│   │   └── TelaPong.java                    # Interface Swing completa
│   └── ...
└── out/                                     # Saída da compilação manual (ignorada no git)
```

### Principais classes

| Classe | Responsabilidade |
|--------|------------------|
| `Bola` | Posição/velocidade, reflexões, ângulo por ponto de contato, aceleração |
| `Computador` | IA com velocidade limitada, reação e margem de erro |
| `Cronometro` | Contagem regressiva, fim de tempo e gol de ouro |
| `Pong` | Estado do jogo: modo, placar, sacada, especial e estatísticas |
| `Skin` / `CatalogoSkins` | Paleta e formato do jogo, presets e skin personalizada persistida |
| `Animacoes` | Motor de partículas: fogo, queimado na mesa, explosão, confete |
| `TelaPong` | Interface: briefing, jogo, pausa, fim, HUD, placar e relógio |

---

## 🛠️ Tecnologias

- **Java 8+** (compatível com versões superiores)
- **Swing** (`JFrame`, `JPanel`, `CardLayout`, `JLayeredPane`)
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

---

## 📌 Roadmap (Kanban)

- [x] Scaffold do projeto (pom.xml, README, KANBAN, LICENSE)
- [ ] Núcleo: Raquete / Bola / Computador / Cronometro / Pong
- [ ] Especial bola de fogo + queimado na mesa
- [ ] Skins (presets + personalizada persistida)
- [ ] Motor de partículas e animações
- [ ] TelaPong: briefing, jogo, pausa, fim + HUD futebol
- [ ] Efeitos sonoros
- [ ] Testes unitários (JUnit — branch separada)
- [ ] Publicar no GitHub

---

## 👤 Autor

**Eduardo Alvez** — [byteswood@gmail.com](mailto:byteswood@gmail.com)

Projeto de portfólio — Pong em Java Desktop.