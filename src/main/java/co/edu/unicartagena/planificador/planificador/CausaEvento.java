package co.edu.unicartagena.planificador.planificador;

/**
 * Motivo por el que un proceso cambia de estado durante la simulación.
 */
public enum CausaEvento {
    LLEGADA("llega al sistema"),
    DESPACHO("recibe la CPU"),
    PETICION_ENTRADA_SALIDA("pide entrada/salida"),
    FIN_ENTRADA_SALIDA("termina su entrada/salida"),
    QUANTUM_AGOTADO("agota su quantum"),
    FINALIZACION("termina");

    private final String descripcion;

    CausaEvento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
