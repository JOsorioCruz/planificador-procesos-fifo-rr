package co.edu.unicartagena.planificador.planificador;

import co.edu.unicartagena.planificador.modelo.Proceso;

import java.util.List;

/**
 * Lo que deja una simulación: la estrategia usada, el diagrama de Gantt,
 * la traza de eventos y los procesos ya terminados.
 */
public record ResultadoSimulacion(String estrategia, List<TramoGantt> gantt,
                                  List<EventoTraza> traza, List<Proceso> procesos) {

    public ResultadoSimulacion {
        gantt = List.copyOf(gantt);
        traza = List.copyOf(traza);
        procesos = List.copyOf(procesos);
    }

    /** Instante en que terminó el último proceso. */
    public int instanteFinal() {
        return gantt.isEmpty() ? 0 : gantt.get(gantt.size() - 1).fin();
    }
}
