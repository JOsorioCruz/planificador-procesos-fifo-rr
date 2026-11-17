package co.edu.unicartagena.planificador.planificador;

import co.edu.unicartagena.planificador.modelo.EstadoProceso;

/**
 * Registro de un cambio de estado: en qué instante, a qué proceso,
 * de qué estado a cuál y por qué.
 */
public record EventoTraza(int instante, String procesoId, EstadoProceso estadoAnterior,
                          EstadoProceso estadoNuevo, CausaEvento causa) {

    @Override
    public String toString() {
        return "t=" + instante + "  " + procesoId + " " + causa.getDescripcion()
                + " (" + estadoAnterior + " a " + estadoNuevo + ")";
    }
}
