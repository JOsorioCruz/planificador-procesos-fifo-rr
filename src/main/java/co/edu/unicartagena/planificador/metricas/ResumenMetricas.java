package co.edu.unicartagena.planificador.metricas;

import java.util.List;

/**
 * Métricas de una simulación completa: las de cada proceso, sus promedios,
 * la utilización de la CPU y los cambios de contexto.
 */
public record ResumenMetricas(String estrategia, List<MetricasProceso> procesos,
                              int instanteFinal, int tiempoCpuOcupada, int cambiosContexto) {

    public ResumenMetricas {
        procesos = List.copyOf(procesos);
    }

    public double promedioSistema() {
        return procesos.stream().mapToInt(MetricasProceso::tiempoSistema).average().orElse(0);
    }

    public double promedioEspera() {
        return procesos.stream().mapToInt(MetricasProceso::tiempoEspera).average().orElse(0);
    }

    public double promedioRespuesta() {
        return procesos.stream().mapToInt(MetricasProceso::tiempoRespuesta).average().orElse(0);
    }

    /** Porcentaje del tiempo total, desde el instante 0, en que la CPU estuvo ocupada. */
    public double utilizacionCpu() {
        return instanteFinal == 0 ? 0 : 100.0 * tiempoCpuOcupada / instanteFinal;
    }
}
