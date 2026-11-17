package co.edu.unicartagena.planificador.planificador;

/**
 * Round Robin (cíclico): los procesos se atienden en el orden de la cola
 * de listos, pero cada uno usa la CPU como máximo un quantum seguido.
 * Si lo agota sin terminar su ráfaga, vuelve al final de la cola.
 * También deja la CPU si pide entrada/salida o si termina.
 */
public class PlanificadorRoundRobin extends Planificador {

    private final int quantum;

    public PlanificadorRoundRobin(int quantum) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("El quantum debe ser mayor que cero");
        }
        this.quantum = quantum;
    }

    public int getQuantum() {
        return quantum;
    }

    @Override
    public String getNombre() {
        return "Round Robin (Q=" + quantum + ")";
    }

    @Override
    protected boolean debeExpulsar(int unidadesContinuas) {
        return unidadesContinuas >= quantum;
    }
}
