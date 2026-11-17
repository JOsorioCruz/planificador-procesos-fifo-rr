package co.edu.unicartagena.planificador.modelo;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados por los que pasa un proceso durante su ciclo de vida.
 * Cada estado sabe a qué estados puede pasar, así una transición
 * inválida se detecta en el momento en que se intenta.
 */
public enum EstadoProceso {
    NUEVO,
    LISTO,
    EJECUTANDO,
    BLOQUEADO,
    TERMINADO;

    /**
     * Indica si desde este estado se puede pasar al estado destino.
     */
    public boolean puedePasarA(EstadoProceso destino) {
        return destinosPermitidos().contains(destino);
    }

    private Set<EstadoProceso> destinosPermitidos() {
        return switch (this) {
            case NUEVO -> EnumSet.of(LISTO);
            case LISTO -> EnumSet.of(EJECUTANDO);
            case EJECUTANDO -> EnumSet.of(LISTO, BLOQUEADO, TERMINADO);
            case BLOQUEADO -> EnumSet.of(LISTO);
            case TERMINADO -> EnumSet.noneOf(EstadoProceso.class);
        };
    }
}
