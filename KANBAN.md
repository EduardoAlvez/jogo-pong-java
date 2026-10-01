# Kanban — Jogo Pong

## Status: concluído (por enquanto) — 01/10/2026

**Released como v1.0.1**, com 180 testes verdes nas duas branches e o
executável conferido embutindo o jar corrigido. O projeto entra em pausa
aqui por decisão do usuário.

**O que ficou fechado**: os dois defeitos visuais achados depois da
validação visual — o hover dos botões que nunca acendia e o prêmio que
congelava no gol de ouro — estão corrigidos, testados (com mutação
verificada em ambos) e publicados na 1.0.1.

**O que fica registrado para quem voltar**:

- **O badge fora do campo continua aberto** (item 1 abaixo) e está presente
  na 1.0.1. É limitação conhecida, não bug escondido.
- O layout da `ui/` continua sem cobertura automática (item 2): o badge
  precisa de uma classe de geometria pura para ser testável.
- A medição do badge foi **corrigida**: a visibilidade depende de `escala`
  e `oy` do letterbox. Na janela padrão 3 badges aparecem e 2 saem; o caso
  "nenhum aparece" é de tela grande.
- Nada foi validado visualmente depois de 30/09: os testes provam que o
  destaque acende e que o prêmio expira, não que ficou bonito na tela.

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
    ├── TelaPong.java       # Janela e desenho
    ├── Botao.java          # Botão puro (geometria + ação), sem Swing
    └── Botaos.java         # Registro dos botões e do highlight do hover

src/main/resources/
├── logo-{16,24,32,48,64,128,256}.png   # Ícone da janela, uma por resolução
├── logo.ico                              # Ícone do executável (.exe)
└── sons/                                # 7 .wav sintetizados

tools/
└── GerarLogo.java                        # Gera os PNGs e o logo.ico a partir do logo-256.png

target/                                   # Saída do build (não versionada)
├── jogo-pong.exe                         # Executável de arquivo único (launch4j)
└── jogo-pong-1.0.jar
```

## A Fazer (Backlog)

| # | Prioridade | Camada | Tarefa | Detalhes |
|---|-----------|--------|--------|----------|
| 1 | **Alta** | ui | **Badges somem com a raquete no topo** | **Defeito conhecido da 1.0.1, aceito como limitação até o projeto voltar.** Em `TelaPong.desenharBadges` o primeiro badge é desenhado em `r.getY() - 20` e cada um sobe mais 21px. `Raquete.setY` permite `y = 0`, e não há `clip` no desenho: com a raquete encostada no topo os badges saem do campo — 1 badge corta 20px, 3 cortam 62px e **com os 5 ativos (Inverter+Congelar+Turbo+Chamas+Encolher, todos podem coexistir) o topo chega a y = −104 e nenhum aparece**. Nenhuma exceção é lançada, por isso nenhum teste existente pega. **Medição corrigida depois**: a visibilidade depende de `escala` e `oy` do letterbox, não só de `[0, FH]` — na janela padrão (`escala` 1,10) 3 badges aparecem na margem e 2 saem; "nenhum aparece" só vale acima de `escala` 3,3 (4K maximizada), onde até um badge único sai. **A fazer**: (a) decidir o desenho — inverter a pilha para baixo quando não couber acima, reservar faixa, ou empilhar na horizontal; (b) extrair a geometria para uma classe pura (como `LayoutSnake`) e testar que nenhum badge sai do campo, com a raquete em `y = 0` e 5 efeitos ativos |
| 2 | Média | ui | Testes de UI (parciais) | A suíte tem **180 testes**; `ui/` agora tem `BotaosTest` (27 testes), extração que tornou o hover testável sem abrir janela. Ainda não coberto: **o layout em si** — `TelaPong` só aparece como referência, nunca é instanciada num teste, e a geometria dos badges (item 1) e dos cards de prêmio (entrada, halo, ícone vetorial) continua sem verificação automática. **A fazer**: extrair a geometria para uma classe pura de layout (como `LayoutSnake` no outro jogo) e testá-la |
| 3 | Média | (branch) | Testes unitários | Branch `testes-jogo-pong`, já mergeada na `main`. A suíte vive nas duas: **180 testes, todos verdes** (core 122, ui 27, skin 12, fx 10, audio 9), com `Pong` em 95,3% de cobertura. O `pom` (JUnit 4.13.2 + surefire + JaCoCo) e o `src/test` estão hoje **na `main`**, que foi publicada assim na v1.0.1. Cobrem física, placar/sacada, IA nos limites, vitória pontos/tempo, gol de ouro, cronômetro, skins/persistência/fallback, fogo por velocidade, bateria do especial, **prêmios (levas, validade, sorteio, coleta e os 8 efeitos, incluindo o que acontece com o prêmio em campo no gol de ouro)**, **pausa pós-gol**, **IA com 2 bolas**, **o portão do especial** (bola no seu campo, meio compartilhado, 2 bolas, um especial por vez e o ratchet de velocidade com os dois querendo usar), **o hover dos botões** e **o áudio servido de dentro de um jar** (regressão do silêncio no executável, troca de mixer e linha que não produz áudio). Cada teste novo de regra vem com o cenário simultâneo e o de reversão |
| 4 | Baixa | core+ui | Polir a IA com 2 bolas | A CPU agora defende a bola mais urgente. Opcional: reduzir velocidade/reação da CPU enquanto houver 2 bolas, para o prêmio bola extra continuar sendo desafio real. **Adiado pelo usuário** |

## Validação visual (conferida na mão pelo usuário)

Fechado em 30/09/2026: **o jogo estáBom assim como está**, incluindo o ícone na
barra de tarefas/Alt+Tab e o feeling da janela de 5s em que o adversário fica
travado por exclusividade durante o especial. Os dois pontos que dependiam de
olhar a tela foram julgados e aprovados — não fica pendência perceptual.

> Ressalva: o item 1 do backlog (badges fora do campo com a raquete no topo) foi
> achado **depois** dessa aprovação, por leitura de código e medida, e não
> chegou a ser visto. Pode estar presente sem ter sido notado.

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
- **core**: `Pong` — modos TEMPO e CLASSICO, placar, sacada alternada (quem sofreu saca), gol de ouro, **especial com bateria de 3 cargas** (+1 por rebatida própria, 5s de chamas + velocidade ×2; **portão: só ativa com pelo menos uma bola do meio para o próprio campo, meio compartilhado, e um especial por vez**), `isBolaEmChamas()` por velocidade (≥540) ou especial, rampa de tempo (fator 0.8), **prêmios em levas de 1–3** (1º aos 8s, depois 10–16s; até 5 em campo; **no gol de ouro não surge leva nova, mas o que já está em campo continua correndo — expira em 9s e pode ser coletado**; coleta pela bola, 1 por frame), **pausa pós-gol de 1s** (entrada e especial bloqueados), **IA defende a bola mais urgente** quando há 2 bolas, stats (vel. máx, melhor troca), encerramento/vitorioso — coberto por testes unitários (`PongTest`, `PausaGolEIaTest`, `PremioTest`)
- **skin**: `Skin` — dados visuais (campo/linha/borda/raquetes/bola/queimado, rótulo da bola, scanlines CRT)
- **skin**: `CatalogoSkins` — 6 presets (Clássico, Neon, Retrô, Oceano, Sunset, Floresta) + Personalizada; persistência `~/.jogo-pong-skin.properties`; fallback Clássico; `toHex`/`parseColor` — seleção/aplicação/reabertura cobertas por `CatalogoSkinsTest`
- **fx**: `Particula` — posição, velocidade, cor, tamanho, vida com fade (`getAlpha`)
- **fx**: `Animacoes` — `emitirFogo` (chamas + marca de queimado persistente, limite 250), `explosaoGol` (40 partículas + tremor), `confete`, `adicionarTremor`/decaimento, `limpar`, limite de 400 partículas — coberto por `AnimacoesTest`
- **audio**: `EfeitosSonoros` — enum `Som`, cache de `Clip`, ganho −9 dB, interruptor global e **escolha explícita de mixer** (padrão, depois os demais, com verificação de que a linha produziu áudio de fato) para sobreviver à troca de dispositivo pelo Windows. A carga sai da EDT e recarrega sozinha quando a lista de mixers muda. **Correção do silêncio no executável**: o áudio é servido de dentro do jar e `AudioSystem.getAudioInputStream()` precisa de `mark/reset` para ler o cabeçalho do WAV, que o stream cru de uma entrada de jar não oferece — daí o `BufferedInputStream`; sem ele o jogo ficava mudo, sem erro visível, no jar e no `.exe`
- **audio**: 7 `.wav` sintetizados (PCM 16-bit 44.1kHz mono) em `src/main/resources/sons/` — rebater, parede, gol, especial, clique, vitoria, premio — validados por `AudioSystem` pelo classpath e **lendo de dentro de um jar**, que é como o jogo é distribuído
- **distribuição**: `logo.ico` gerado por `tools/GerarLogo.java` a partir de `logo-256.png`, em 7 resoluções, e `target/jogo-pong.exe` via launch4j — arquivo único com o jar embutido, ícone e VersionInfo, exigindo Java 17+ (sem JRE embarcado). O ícone extraído do executável foi conferido contra o `logo-32.png`
- **publicação**: repositório `github.com/EduardoAlvez/jogo-pong-java` com as branches `main` e `testes-jogo-pong`, descrição e topics; **release v1.0.1** com `jogo-pong-1.0.exe` (365 625 bytes, SHA-256 `33f9f3da…a2c90c`) e `jogo-pong-1.0.1.jar` (243 257 bytes, SHA-256 `a749631c…08a8e`). O `.exe` foi aberto e conferido embutindo o jar novo (`Botao`/`Botaos` presentes). A 1.0.0 fica disponível na tag anterior, com os dois defeitos visuais corrigidos na 1.0.1
- **ui**: logo da janela em 7 resoluções (`src/main/resources/logo-*.png`), carregado do classpath e aplicado via `setIconImages` — o Windows escolhe a imagem de cada contexto em vez de escalar uma só; carga tolerante a arquivo ausente, verificada por harness headless próprio (fora da suíte)
- **ui**: `TelaPong` — briefing customizado, controle via KeyBindings (W/S, ↑/↓, Z/M, Espaço, Esc), campo 800×500 escalado (letterbox/responsivo), HUD futebol (P1\|⏱\|P2, timer vermelho piscando nos últimos 10s, 3 traços de carga do especial), countdown 3-2-1-JÁ!, banner GOL!, gol de ouro, glassmorphism nos cards, skins com seletor ◀▶ + Personalizar (JColorChooser), pausa real com Retomar/Reiniciar/Início, fim com stats (placar/duração/melhor troca/velocidade máx) + Jogar de novo/Início, brilho no especial, marca de queimado, **badges de efeito sobre a raquete** (ícone + segundos) e **cards de prêmio com ícones vetoriais** (sem depender de fonte: ↕ ❄ ⚡ ⏱ ▮ 🔵 viram formas, o "?" do coringa é ASCII) — as 6 fases renderizam sem erro (harness headless pontual, **fora da suíte**: não há teste de UI) e `mvn package` OK. **Defeito conhecido**: os badges de efeito saem do campo quando a raquete encosta no topo — ver item 1 do backlog
- **ui**: hover dos botões — o destaque **nunca acendia** em jogo, e o defeito estava certo: o cursor virava mão, mas o fundo não clareava, então o jogo parecia interativo sem responder ao mouse. O estado vivia em cada instância de `Botao`, que a própria pintura recria a cada quadro (o highlight era apagado antes de ser lido). `Botao` e `Botaos` foram extraídos de `TelaPong` como classes sem Swing: o highlight passou a mora em `Botaos`, que vive a partida, e é comparado pela **chave geométrica** (`x:y:w:h`) em vez da identidade do objeto — assim o botão recriado pela pintura reencontra o seu destaque, e trocar o rótulo (o som muda o texto) não o perde. Ao corrigir, o teste pegou um bug da própria implementação: com botões sobrepostos, `sob()` devolvia o **primeiro**, e o clique podia acertar o botão errado — agora vale o último, que é a ordem de pintura. 27 testes em `BotaosTest`, incluindo o que reproduz o defeito (marcar, repinturar, conferir que o destaque continua). Mutação verificada: voltar ao highlight por instância derruba 4 dos testes
- **core**: prêmios no gol de ouro — `atualizarPremio` abria com um `return` quando o gol de ouro começava, o que não era só "não criar leva nova": o return cortava o resto do método, e o prêmio **já em campo parava de contar o tempo**. Ele não expirava, não animava e a bola passava por cima sem coletar; se o gol de ouro começasse logo após o prêmio surgir, ele travava no meio da animação de entrada, mostrando o "?" do coringa para sempre em vez do ícone. Como o gol de ouro só termina no próximo gol, o card ficava dezenas de segundos parado — lido como travamento. O return passou a proteger apenas o agendamento de leva nova. O teste que já existia passava **também** com o código quebrado, porque começava o gol de ouro com o campo vazio e nunca exercitava o prêmio congelado; agora há 4 cenários (expira, continua animando, a bola coleta, e a leva nova continua barrada pelo caminho real). Duas mutações verificadas: reintroduzir o return no topo derruba os 3 cenários de prêmio em campo; remover só o guard interno derruba apenas o de spawn, isolando o que não mudou

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
- **Prêmios**: levas de 1–3, validade 9s, até 5 em campo; no gol de ouro não surge
  leva nova, mas o prêmio que já estava em campo **não congela** — expira e pode ser
  coletado (congelar o card no lugar era lido como travamento em jogo); Inverter fora
  do sorteio no 1P e +10s fora do clássico
- **Branches**: `main` = jogo completo **com a suíte** (11 arquivos em `src/test`, hoje
  180 testes); `testes-jogo-pong` = onde se desenvolvem os testes novos, que depois
  são mergeados na `main`. A separação é de fluxo de trabalho, não de conteúdo.
  **Decisão do usuário (01/10/2026)**: manter a suíte na `main`. Ela não afeta o que
  é entregue — o JUnit está em `scope=test` e o jar publicado tem 32 classes com
  **zero** `*Test.class` (conferido) —, e permite `git checkout main && mvn test`
  validar a árvore que de fato roda. A regra antiga ("`main` sem dependências de
  teste") ficou sem efeito desde que a suíte foi publicada na v1.0.0 (e na 1.0.1)
- **Documentação**: `README.md` e `KANBAN.md` são editados **só na `main`** — não
  nas branches de teste, para o arquivo não divergir entre elas
- **Distribuição**: o jogo é entregue como `.exe` de arquivo único (launch4j), com o
  jar embutido e **sem JRE embarcado** — exige Java 17+ instalado. Um `.jar` não
  permite definir o ícone mostrado pelo Explorer, que é o motivo do executável
- **Publicação**: `README.md` e `KANBAN.md` descrevem a `1.0.1`; a nota de release
  fica no corpo da release do GitHub, não versionada no repositório. A release
  anexa o `.exe` e o `.jar`, e ambos são conferidos por hash após o upload
