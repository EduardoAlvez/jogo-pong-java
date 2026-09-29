# Kanban — Jogo Pong

## Estrutura (subpacotes por camada)

```
src/main/java/com/portfolio/pong/
├── core/       Núcleo puro e testável (sem Swing)
│   ├── Raquete.java / Bola.java / Computador.java / Cronometro.java / Pong.java
├── skin/       Dados visuais e catálogo
│   ├── Skin.java / CatalogoSkins.java
├── fx/         Efeitos e animações (partículas)
│   ├── Particula.java / Animacoes.java
├── audio/      Efeitos sonoros
│   └── EfeitosSonoros.java
└── ui/         Interface gráfica Swing
    └── TelaPong.java
```

## A Fazer (Backlog)

| # | Prioridade | Camada | Tarefa | Detalhes |
|---|-----------|--------|--------|----------|
| 1 | Alta | ui | `TelaPong` — briefing | Card de início: modo, dificuldade, tempo/placar, skin com preview animado, controles |
| 2 | Alta | ui | `TelaPong` — jogo | Placar futebol P1\|⏱\|P2 topo central, timer vermelho+piscando nos 10s finais, indicador do especial 🔥, countdown 3-2-1-JÁ, banner GOL!, saque rápido |
| 3 | Alta | ui | `TelaPong` — pausa/fim | Pausa verdadeira (overlay Retomar/Reiniciar/Voltar); fim com stats (placar, duração, melhor troca, velocidade máxima) + Jogar novamente/Início |
| 4 | Média | ui | Visual | Glassmorphism nos cards, glow de neon, redimensionável |
| 5 | Média | (branch) | Testes unitários | Branch `testes-jogo-pong`: pom (JUnit 4.13.2 + surefire + JaCoCo) + `PongTest` cobrindo física, placar/sacada, IA nos limites, vitória pontos/tempo, gol de ouro, cronômetro, especial (1 uso), skins/persistência/fallback, fogo por velocidade |
| 6 | Média | — | Publicar no GitHub | Repositório remoto + push das branches main e testes |
| 7 | Média | (branch) | Sons: reproduzir OK em build | Confirmar que os `.wav` entram no `.jar` e tocam via `mvn package` + `java -jar` |

## Fazendo (Doing)

_— vazio —_

## Feito (Done)

- Scaffold do projeto (pom.xml, README, KANBAN, LICENSE, .gitignore, estrutura por camadas: core/skin/fx/audio/ui)
- Repositório git inicializado com identidade configurada
- **core**: `Raquete` — posição/tamanho, `moverCima`/`moverBaixo`, clamp nos limites do campo, `centralizar`, lado
- **core**: `Bola` — reflexão de parede (flip Y), ângulo por ponto de contato (±60°, clamp), anti-sticking pós-rebote, +6% por rebatida (teto 800 px/s), `ajustarVelocidade` preservando ângulo, detecção de passe de lateral
- **core**: `Computador` — IA batedível (velocidade limitada + reação atrasada + zona morta) com enum `Dificuldade` (Fácil/Médio/Difícil) e previsão de interceptação
- **core**: `Cronometro` — durações 60/120/180s, contagem regressiva, `fatorDeUrgencia` (0→1), formato MM:SS
- **core**: `Pong` — modos TEMPO e CLASSICO, placar, sacada alternada (quem sofreu saca), gol de ouro, especial (1 uso/jogador, 5s), `isBolaEmChamas()` por velocidade (≥540) ou especial, rampa de tempo (fator 0.8), stats (vel. máx, melhor troca), encerramento/vitorioso — validado por smoke test
- **skin**: `Skin` — dados visuais (campo/linha/borda/raquetes/bola/queimado, rótulo da bola, scanlines CRT)
- **skin**: `CatalogoSkins` — 6 presets (Clássico, Neon, Retrô, Oceano, Sunset, Floresta) + Personalizada; persistência `~/.jogo-pong-skin.properties`; fallback Clássico; `toHex`/`parseColor` — validado por smoke test de seleção/aplicação/reabertura
- **fx**: `Particula` — posição, velocidade, cor, tamanho, vida com fade (`getAlpha`)
- **fx**: `Animacoes` — `emitirFogo` (chamas + marca de queimado persistente, limite 250), `explosaoGol` (40 partículas + tremor), `confete`, `adicionarTremor`/decaimento, `limpar`, limite de 400 partículas — validado por smoke test
- **audio**: `EfeitosSonoros` — mesmo padrão da Forca (enum `Som`, carga preguiçosa, cache de `Clip`, ganho −9 dB, interruptor global)
- **audio**: 6 `.wav` sintetizados (PCM 16-bit 44.1kHz mono) em `src/main/resources/sons/` — rebater, parede, gol, especial, clique, vitoria — validados por `AudioSystem` (carregamento pelo classpath)

## Regras (tomadas de decisão)

- **Estrutura**: subpacotes por camada (`core`, `skin`, `fx`, `audio`, `ui`) — núcleo sem Swing, testável
- **Modos**: 1P vs Computador (Fácil/Médio/Difícil) e 2P local
- **Modo Tempo**: duração 1/2/3 min (escolha do usuário); bola acelera conforme o tempo esgota; empate → gol de ouro (morte súbita)
- **Modo Clássico**: sem relógio, primeiro a 5/7/10 pontos (escolha do usuário)
- **Controles**: W/S (P1), ↑/↓ (P2), Espaço pausa, Z (especial P1), M (especial P2)
- **Bola de fogo**: automática por velocidade + tecla especial; efeito só visual, mas deixa rastro de queimado na mesa
- **Branches**: `main` = jogo completo sem dependências de teste; testes sempre em branch separada