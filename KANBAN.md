# Kanban — Jogo Pong

## Estrutura (subpacotes por camada)

```
src/main/java/com/portfolio/pong/
├── core/       Núcleo puro e testável (sem Swing)
│   ├── Raquete.java / Bola.java / Computador.java / Cronometro.java / Premio.java / Pong.java
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
| 1 | Alta | (branch) | Testes unitários | Branch `testes-jogo-pong`: pom (JUnit 4.13.2 + surefire + JaCoCo) + **144 testes** cobrindo física, placar/sacada, IA nos limites, vitória pontos/tempo, gol de ouro, cronômetro, skins/persistência/fallback, fogo por velocidade, bateria do especial, **prêmios (levas, validade, sorteio, coleta e os 8 efeitos)**, **pausa pós-gol**, **IA com 2 bolas** e **o portão do especial** (bola no seu campo, meio compartilhado, 2 bolas, um especial por vez e o ratchet de velocidade com os dois querendo usar). Cada teste novo de regra vem com o cenário simultâneo e o de reversão |
| 2 | Média | ui | Validação visual | **Feito**: briefing, modos, skins + seletor, gol de ouro, pausa, fim + stats, som, prêmios, pausa pós-gol, Marco 4. **Pendente**: o feeling do portão do especial — se a janela de 5s em que o adversário fica travado por exclusividade incomoda na mão |
| 3 | Média | — | Publicar no GitHub | Repositório remoto + push das branches main e testes |
| 4 | Baixa | core+ui | Polir a IA com 2 bolas | A CPU agora defende a bola mais urgente. Opcional: reduzir velocidade/reação da CPU enquanto houver 2 bolas, para o prêmio bola extra continuar sendo desafio real. **Adiado pelo usuário** |

## Fazendo (Doing)

_— vazio —_

## Feito (Done)

- Scaffold do projeto (pom.xml, README, KANBAN, LICENSE, .gitignore, estrutura por camadas: core/skin/fx/audio/ui)
- Repositório git inicializado com identidade configurada
- **core**: `Raquete` — posição/tamanho, `moverCima`/`moverBaixo`, clamp nos limites do campo, `centralizar`, lado
- **core**: `Bola` — reflexão de parede (flip Y), ângulo por ponto de contato (±60°, clamp), anti-sticking pós-rebote, +8% por rebatida (teto **900 px/s**), `ajustarVelocidade` preservando ângulo, detecção de passe de lateral
- **core**: `Computador` — IA batedível (velocidade limitada + reação atrasada + zona morta) com enum `Dificuldade` (Fácil/Médio/Difícil) e previsão de interceptação
- **core**: `Cronometro` — durações 60/120/180s, contagem regressiva, `fatorDeUrgencia` (0→1), `adicionarSegundos(int)` (prêmio +10s), formato MM:SS
- **core**: `Premio` — 8 tipos com cor/rótulo, validade 9s, animação de entrada, pulso e `atualizar/acabou`; `Pong.tiposDePremio()` (package-private) filtra por modo
- **core**: `Pong` — modos TEMPO e CLASSICO, placar, sacada alternada (quem sofreu saca), gol de ouro, **especial com bateria de 3 cargas** (+1 por rebatida própria, 5s de chamas + velocidade ×2; **portão: só ativa com pelo menos uma bola do meio para o próprio campo, meio compartilhado, e um especial por vez**), `isBolaEmChamas()` por velocidade (≥540) ou especial, rampa de tempo (fator 0.8), **prêmios em levas de 1–3** (1º aos 8s, depois 10–16s; até 5 em campo; sem spawn em gol de ouro; coleta pela bola, 1 por frame), **pausa pós-gol de 1s** (entrada e especial bloqueados), **IA defende a bola mais urgente** quando há 2 bolas, stats (vel. máx, melhor troca), encerramento/vitorioso — validado por smoke test
- **skin**: `Skin` — dados visuais (campo/linha/borda/raquetes/bola/queimado, rótulo da bola, scanlines CRT)
- **skin**: `CatalogoSkins` — 6 presets (Clássico, Neon, Retrô, Oceano, Sunset, Floresta) + Personalizada; persistência `~/.jogo-pong-skin.properties`; fallback Clássico; `toHex`/`parseColor` — validado por smoke test de seleção/aplicação/reabertura
- **fx**: `Particula` — posição, velocidade, cor, tamanho, vida com fade (`getAlpha`)
- **fx**: `Animacoes` — `emitirFogo` (chamas + marca de queimado persistente, limite 250), `explosaoGol` (40 partículas + tremor), `confete`, `adicionarTremor`/decaimento, `limpar`, limite de 400 partículas — validado por smoke test
- **audio**: `EfeitosSonoros` — mesmo padrão da Forca (enum `Som`, carga preguiçosa, cache de `Clip`, ganho −9 dB, interruptor global)
- **audio**: 7 `.wav` sintetizados (PCM 16-bit 44.1kHz mono) em `src/main/resources/sons/` — rebater, parede, gol, especial, clique, vitoria, premio — validados por `AudioSystem` (carregamento pelo classpath)
- **ui**: `TelaPong` — briefing customizado, controle via KeyBindings (W/S, ↑/↓, Z/M, Espaço, Esc), campo 800×500 escalado (letterbox/responsivo), HUD futebol (P1\|⏱\|P2, timer vermelho piscando nos últimos 10s, 3 traços de carga do especial), countdown 3-2-1-JÁ!, banner GOL!, gol de ouro, glassmorphism nos cards, skins com seletor ◀▶ + Personalizar (JColorChooser), pausa real com Retomar/Reiniciar/Início, fim com stats (placar/duração/melhor troca/velocidade máx) + Jogar de novo/Início, brilho no especial, marca de queimado, **badges de efeito sobre a raquete** (ícone + segundos) e **cards de prêmio com ícones vetoriais** (sem depender de fonte: ↕ ❄ ⚡ ⏱ ▮ 🔵 viram formas, o "?" do coringa é ASCII) — renderizado sem erros nas 6 fases (smoke test headless) e `mvn package` OK

## Regras (tomadas de decisão)

- **Estrutura**: subpacotes por camada (`core`, `skin`, `fx`, `audio`, `ui`) — núcleo sem Swing, testável
- **Modos**: 1P vs Computador (Fácil/Médio/Difícil) e 2P local
- **Modo Tempo**: duração 1/2/3 min (escolha do usuário); bola acelera conforme o tempo esgota; empate → gol de ouro (morte súbita)
- **Modo Clássico**: sem relógio, primeiro a 5/7/10 pontos (escolha do usuário)
- **Controles**: W/S (P1), ↑/↓ (P2), Espaço pausa, Z (especial P1), M (especial P2)
- **Bola de fogo**: automática por velocidade + especial; deixa rastro de queimado na mesa
- **Especial**: bateria de 3 cargas (+1 por rebatida da própria raquete); só entra com
  pelo menos uma bola do meio do campo para o lado do jogador (o centro é
  compartilhado e libera os dois) e **um por vez** — enquanto um está ativo, o
  outro espera, e a carga não é gasta na tentativa
- **Velocidade**: base 240 px/s, +8% composto por rebatida, teto 900 px/s; volta à base a cada gol (com 1s de pausa antes do novo saque)
- **Prêmios**: levas de 1–3, validade 9s, até 5 em campo, sem spawn no gol de ouro; Inverter fora do sorteio no 1P e +10s fora do clássico
- **Branches**: `main` = jogo completo sem dependências de teste; testes sempre em branch separada
- **Documentação**: `README.md` e `KANBAN.md` são editados **só na `main`** — não
  nas branches de teste, para o arquivo não divergir entre elas
