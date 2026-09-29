package com.portfolio.pong.fx;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Motor de efeitos do Pong: gerencia partículas (explosão de gol, chamas da
 * bola, confetes), marca de queimado persistente na mesa e tremor de tela.
 *
 * <p>Efeitos são visuais (não alteram a física). O desenho fica com a camada
 * de interface; aqui fica apenas o estado e a evolução temporal.</p>
 *
 * @author Eduardo Alvez
 */
public class Animacoes {

    /** Limite do número de partículas vivas (jogo fluido). */
    public static final int LIMITE_PARTICULAS = 400;

    /**
     * Marca de queimado deixada pela bola em chamas na mesa: persistente
     * durante toda a partida.
     */
    public static class MarcaQueimado {
        private final double x;
        private final double y;
        private final double raio;

        MarcaQueimado(double x, double y, double raio) {
            this.x = x;
            this.y = y;
            this.raio = raio;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getRaio() {
            return raio;
        }
    }

    private final Random aleatorio = new Random();
    private final List<Particula> particulas = new ArrayList<>();
    private final List<MarcaQueimado> marcas = new ArrayList<>();
    private double tremor;

    /** Avança todas as partículas e decai o tremor. */
    public void atualizar(double dt) {
        for (int i = particulas.size() - 1; i >= 0; i--) {
            Particula p = particulas.get(i);
            p.atualizar(dt);
            if (!p.estaViva()) {
                particulas.remove(i);
            }
        }
        if (tremor > 0) {
            tremor = Math.max(0.0, tremor - dt * 6.0);
        }
    }

    /** Emite uma pequena rajada de chamas na posição da bola. */
    public void emitirFogo(double x, double y) {
        adicionar(new Particula(
                x + desvio(6), y + desvio(6),
                aleatorio.nextDouble() * 60 - 30, aleatorio.nextDouble() * 60 - 30,
                porFogo(), aleatorio.nextDouble() * 3 + 2, 0.25 + aleatorio.nextDouble() * 0.2));
        if (marcas.size() < 250) {
            marcas.add(new MarcaQueimado(x + desvio(10), y + desvio(10), 2 + aleatorio.nextDouble() * 3));
        }
    }

    /** Explosão de partículas no momento do gol (com tremor). */
    public void explosaoGol(double x, double y) {
        int quantidade = 40;
        for (int i = 0; i < quantidade; i++) {
            double angulo = aleatorio.nextDouble() * 2 * Math.PI;
            double velocidade = 60 + aleatorio.nextDouble() * 220;
            Color cor = i % 2 == 0 ? new Color(0xFFD700) : new Color(0xFF7F27);
            adicionar(new Particula(x, y,
                    Math.cos(angulo) * velocidade, Math.sin(angulo) * velocidade - 40,
                    cor, 2 + aleatorio.nextDouble() * 4, 0.5 + aleatorio.nextDouble() * 0.6));
        }
        adicionarTremor(14);
    }

    /** Chuva de confetes do alto da tela (vitória). */
    public void confete(double largura, Color... cores) {
        int quantidade = 90;
        for (int i = 0; i < quantidade; i++) {
            Color cor = cores.length == 0
                    ? new Color(0xFFD700)
                    : cores[aleatorio.nextInt(cores.length)];
            adicionar(new Particula(
                    aleatorio.nextDouble() * largura, -10 - aleatorio.nextDouble() * 40,
                    aleatorio.nextDouble() * 40 - 20, 30 + aleatorio.nextDouble() * 60,
                    cor, 3 + aleatorio.nextDouble() * 3, 2.0 + aleatorio.nextDouble() * 2.0));
        }
    }

    /** Rajada de brilho quando a bola coleta um prêmio. */
    public void coletarPremio(double x, double y, Color cor) {
        int quantidade = 26;
        for (int i = 0; i < quantidade; i++) {
            double angulo = aleatorio.nextDouble() * 2 * Math.PI;
            double velocidade = 30 + aleatorio.nextDouble() * 130;
            adicionar(new Particula(x, y,
                    Math.cos(angulo) * velocidade, Math.sin(angulo) * velocidade - 30,
                    cor, 2 + aleatorio.nextDouble() * 3, 0.35 + aleatorio.nextDouble() * 0.35));
        }
    }

    /** Registra um tremor de tela (magnitude em pixels). */
    public void adicionarTremor(double magnitude) {
        tremor = Math.max(tremor, magnitude);
    }

    /** Remove todas as partículas, marcas e o tremor (reinício/partida nova). */
    public void limpar() {
        particulas.clear();
        marcas.clear();
        tremor = 0;
    }

    /** @return lista viva de partículas (para desenho) */
    public List<Particula> getParticulas() {
        return particulas;
    }

    /** @return marcas de queimado persistente na mesa */
    public List<MarcaQueimado> getMarcas() {
        return marcas;
    }

    /** @return magnitude atual do tremor de tela em pixels */
    public double getTremor() {
        return tremor;
    }

    /** @return deslocamento aleatório para posições de rastro/chamas */
    private double desvio(double maximo) {
        return aleatorio.nextDouble() * 2 * maximo - maximo;
    }

    /** Paleta de cores da chama (amarelo → laranja → vermelho). */
    private Color porFogo() {
        double sorte = aleatorio.nextDouble();
        if (sorte < 0.5) {
            return new Color(0xFFD700);
        }
        if (sorte < 0.8) {
            return new Color(0xFF7F27);
        }
        return new Color(0xFF3B30);
    }

    private void adicionar(Particula particula) {
        if (particulas.size() >= LIMITE_PARTICULAS) {
            particulas.remove(0);
        }
        particulas.add(particula);
    }
}