package co.edu.unicartagena.planificador.planificador;

/**
 * FIFO: primero en llegar, primero en ser despachado. El proceso conserva
 * la CPU hasta que termina o hasta que pide entrada/salida.
 */
public class PlanificadorFIFO extends Planificador {

    @Override
    public String getNombre() {
        return "FIFO";
    }

    @Override
    protected boolean debeExpulsar(int unidadesContinuas) {
        return false;
    }
}
