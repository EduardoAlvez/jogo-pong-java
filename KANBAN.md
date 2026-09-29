# Kanban — Jogo Pong

## A Fazer (Backlog)

| # | Prioridade | Tarefa | Detalhes |
|---|-----------|--------|----------|
| 1 | Alta | Núcleo `Raquete` | Posição/tamanho, movimento, limites do campo |
| 2 | Alta | Núcleo `Bola` | Reflexão de parede (flip Y); ângulo por ponto de contato (±60°, clamp anti-180°); anti-sticking pós-rebote; +6% velocidade por rebatida (teto); rampa contínua conforme o tempo esgota |
| 3 | Alta | Núcleo `Computador` | IA batedível: velocidade limitada + reação atrasada + margem de erro; Fácil/Médio/Difícil |
| 4 | Alta | Núcleo `Cronometro` | Contagem MM:SS (durações 1/2/3 min), fim do tempo, estado de gol de ouro |
| 5 | Alta | Núcleo `Pong` | Estado: modo, placar, sacada alternada, fim de partida, stats (vel. máx, melhor troca) |
| 6 | Alta | Fogo/Especial (núcleo) | `isBolaEmChamas()` automático por velocidade; `ativarEspecial()` (tecla, 1 uso por jogador: Z=P1, M=P2, 5s); visual-only |
| 7 | Alta | `Skin` + `CatalogoSkins` | 6 presets (Clássico, Neon, Retrô/fósforo, Oceano, Sunset, Floresta) + Personalizada (cores via JColorChooser + emoji/letra de bola/raquetes); persistência `~/.jogo-pong-skin.properties`; fallback Clássico; scanlines CRT só na Retrô |
| 8 | Alta | Partículas/animações | Motor de partículas (life/cor/fade); aura + rastro de fogo; marca de queimado persistente na mesa; explosão no gol + tremor; fundo animado sutil; confete na vitória |
| 9 | Alta | `TelaPong` — briefing | Card de início: modo, dificuldade, tempo/placar, skin com preview animado, controles |
| 10 | Alta | `TelaPong` — jogo | Placar futebol P1\|⏱\|P2 topo central, timer vermelho+piscando nos 10s finais, indicador do especial 🔥, countdown 3-2-1-JÁ, banner GOL!, saque rápido |
| 11 | Alta | `TelaPong` — pausa/fim | Pausa verdadeira (overlay Retomar/Reiniciar/Voltar); fim com stats (placar, duração, melhor troca, velocidade máxima) + Jogar novamente/Início |
| 12 | Alta | Efeitos sonoros | Bola/parede, gol, especial, fim de partida (padrão da Forca, com botão 🔊/🔇) |
| 13 | Média | Visual | Glassmorphism nos cards, glow de neon, redimensionável |
| 14 | Média | Testes unitários | Branch `testes-jogo-pong`: pom (JUnit 4.13.2 + surefire + JaCoCo) + `PongTest` cobrindo física, placar/sacada, IA nos limites, vitória pontos/tempo, gol de ouro, cronômetro, especial (1 uso), skins/persistência/fallback, fogo por velocidade |
| 15 | Média | Publicar no GitHub | Repositório remoto + push das branches main e testes |

## Fazendo (Doing)

| # | Tarefa | Observações |
|---|--------|-------------|
| 1 | Scaffold do projeto | pom.xml, estrutura, git inicial — em andamento |

## Feito (Done)

- Scaffold do projeto (pom.xml, README, KANBAN, LICENSE, .gitignore, estrutura de pastas)
- Repositório git inicializado com identidade configurada

## Regras (tomadas de decisão)

- **Modos**: 1P vs Computador (Fácil/Médio/Difícil) e 2P local
- **Modo Tempo**: duração 1/2/3 min (escolha do usuário); bola acelera conforme o tempo esgota; empate → gol de ouro (morte súbita)
- **Modo Clássico**: sem relógio, primeiro a 5/7/10 pontos (escolha do usuário)
- **Controles**: W/S (P1), ↑/↓ (P2), Espaço pausa, Z (especial P1), M (especial P2)
- **Bola de fogo**: automática por velocidade + tecla especial; efeito só visual, mas deixa rastro de queimado na mesa
- **Branches**: `main` = jogo completo sem dependências de teste; testes sempre em branch separada