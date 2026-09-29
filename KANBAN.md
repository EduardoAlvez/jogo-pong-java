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
| 1 | Alta | (branch) | Testes unitários | Branch `testes-jogo-pong`: pom (JUnit 4.13.2 + surefire + JaCoCo) + `PongTest` cobrindo física, placar/sacada, IA nos limites, vitória pontos/tempo, gol de ouro, cronômetro, especial (1 uso), skins/persistência/fallback, fogo por velocidade |
| 2 | Alta | ui | Validação visual | Jogar/testar manualmente: briefing, modos, especial, skins, gol de ouro, pausa, fim + stats, som |
| 3 | Média | — | Publicar no GitHub | Repositório remoto + push das branches main e testes |
| 4 | Alta | core+ui | Prêmios no campo (estilo Mario) | Card "?" pulsando sobe no gramado; coleta **pela bola** (coletor = quem rebateu por último); spawn: 1º após 8s, depois 10–16s; despawn 9s; máx. 1 ativo; **sem spawn em gol de ouro**. Efeitos: ↕ Inverter (controles do adversário 5s, só 2P), ❄ Congelar (raquete adversária travada 3s), ⚡ Turbo (+40% própria raquete 5s), 🔥 Chamas (fogo 3s sem gastar especial), ⏱ +10s (modo Tempo), ▮ Encolher (raquete adversária 60px 5s), 🔵 +1 bola (até o próximo gol, máx. 2), ? Coringa (re-sorteio). Em 1P o adversário é a CPU (Inverter some do sorteio — validado por teste via `tiposDePremio()`). HUD com badges por jogador + burst de partículas na coleta |
| 5 | Alta | core+ui | Especial recarregável (bateria) | Substitui o "1 uso por partida": bateria de 3 cargas por jogador, +1 por **rebatida da própria raquete** (não carrega enquanto o próprio especial está ativo; persiste após gol; zera no `iniciarPartida`). Uso com carga 3 → zera e ativa por 5s: bola **em chamas + velocidade ×2** (restaura a velocidade anterior ao expirar; funciona com multiball, cada bola restaura a própria; rampa do modo Tempo suspensa durante o especial). HUD: 3 traços por jogador — **3 = verde (cheia), 2 = amarelo, 1 = vermelho**; quando ativo mostra 🔥×2 [seg] |

## Fazendo (Doing)

_— vazio —_

## Feito (Done)

- Scaffold do projeto (pom.xml, README, KANBAN, LICENSE, .gitignore, estrutura por camadas: core/skin/fx/audio/ui)
- Repositório git inicializado com identidade configurada
- **core**: `Raquete` — posição/tamanho, `moverCima`/`moverBaixo`, clamp nos limites do campo, `centralizar`, lado
- **core**: `Bola` — reflexão de parede (flip Y), ângulo por ponto de contato (±60°, clamp), anti-sticking pós-rebote, +8% por rebatida (teto 800 px/s), `ajustarVelocidade` preservando ângulo, detecção de passe de lateral
- **core**: `Computador` — IA batedível (velocidade limitada + reação atrasada + zona morta) com enum `Dificuldade` (Fácil/Médio/Difícil) e previsão de interceptação
- **core**: `Cronometro` — durações 60/120/180s, contagem regressiva, `fatorDeUrgencia` (0→1), formato MM:SS
- **core**: `Pong` — modos TEMPO e CLASSICO, placar, sacada alternada (quem sofreu saca), gol de ouro, especial (1 uso/jogador, 5s), `isBolaEmChamas()` por velocidade (≥540) ou especial, rampa de tempo (fator 0.8), stats (vel. máx, melhor troca), encerramento/vitorioso — validado por smoke test
- **skin**: `Skin` — dados visuais (campo/linha/borda/raquetes/bola/queimado, rótulo da bola, scanlines CRT)
- **skin**: `CatalogoSkins` — 6 presets (Clássico, Neon, Retrô, Oceano, Sunset, Floresta) + Personalizada; persistência `~/.jogo-pong-skin.properties`; fallback Clássico; `toHex`/`parseColor` — validado por smoke test de seleção/aplicação/reabertura
- **fx**: `Particula` — posição, velocidade, cor, tamanho, vida com fade (`getAlpha`)
- **fx**: `Animacoes` — `emitirFogo` (chamas + marca de queimado persistente, limite 250), `explosaoGol` (40 partículas + tremor), `confete`, `adicionarTremor`/decaimento, `limpar`, limite de 400 partículas — validado por smoke test
- **audio**: `EfeitosSonoros` — mesmo padrão da Forca (enum `Som`, carga preguiçosa, cache de `Clip`, ganho −9 dB, interruptor global)
- **audio**: 6 `.wav` sintetizados (PCM 16-bit 44.1kHz mono) em `src/main/resources/sons/` — rebater, parede, gol, especial, clique, vitoria — validados por `AudioSystem` (carregamento pelo classpath)
- **ui**: `TelaPong` — briefing customizado, controle via KeyBindings (W/S, ↑/↓, Z/M, Espaço, Esc), campo 800×500 escalado (letterbox/responsivo), HUD futebol (P1\|⏱\|P2, timer vermelho piscando nos últimos 10s, indicador do especial 🔥), countdown 3-2-1-JÁ!, banner GOL!, gol de ouro, glassmorphism nos cards, skins com seletor ◀▶ + Personalizar (JColorChooser), pausa real com Retomar/Reiniciar/Início, fim com stats (placar/duração/melhor troca/velocidade máx) + Jogar de novo/Início, brilho no especial, marca de queimado — renderizado sem erros nas 6 fases (smoke test headless) e `mvn package` OK

## Regras (tomadas de decisão)

- **Estrutura**: subpacotes por camada (`core`, `skin`, `fx`, `audio`, `ui`) — núcleo sem Swing, testável
- **Modos**: 1P vs Computador (Fácil/Médio/Difícil) e 2P local
- **Modo Tempo**: duração 1/2/3 min (escolha do usuário); bola acelera conforme o tempo esgota; empate → gol de ouro (morte súbita)
- **Modo Clássico**: sem relógio, primeiro a 5/7/10 pontos (escolha do usuário)
- **Controles**: W/S (P1), ↑/↓ (P2), Espaço pausa, Z (especial P1), M (especial P2)
- **Bola de fogo**: automática por velocidade + tecla especial; efeito só visual, mas deixa rastro de queimado na mesa
- **Branches**: `main` = jogo completo sem dependências de teste; testes sempre em branch separada