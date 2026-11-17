package co.edu.unicartagena.planificador.metricas;

/**
 * Tiempos de un proceso al terminar la simulación.
 *
 * @param tiempoSistema   salida menos llegada
 * @param tiempoEspera    tiempo total que pasó en la cola de listos
 * @param tiempoRespuesta primer instante en CPU menos llegada
 */
public record MetricasProceso(String procesoId, int llegada, int totalCpu, int totalEntradaSalida,
                              int primeraEjecucion, int finalizacion,
                              int tiempoSistema, int tiempoEspera, int tiempoRespuesta) {
}
