package co.edu.unicartagena.planificador.metricas;

import co.edu.unicartagena.planificador.modelo.EstadoProceso;
import co.edu.unicartagena.planificador.modelo.Proceso;
import co.edu.unicartagena.planificador.planificador.EventoTraza;
import co.edu.unicartagena.planificador.planificador.ResultadoSimulacion;
import co.edu.unicartagena.planificador.planificador.TramoGantt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula las métricas a partir del resultado de una simulación.
 * El tiempo de espera se mide con la traza: suma los intervalos que el
 * proceso pasó en estado listo, desde que entra a la cola hasta que recibe la CPU.
 */
public final class CalculadoraMetricas {

    private CalculadoraMetricas() {
    }

    public static ResumenMetricas calcular(ResultadoSimulacion resultado) {
        Map<String, Integer> espera = esperaPorProceso(resultado.traza());
        List<MetricasProceso> metricas = new ArrayList<>();
        for (Proceso p : resultado.procesos()) {
            metricas.add(new MetricasProceso(
                    p.getId(), p.getLlegada(), p.getTotalCpu(), p.getTotalEntradaSalida(),
                    p.getPrimeraEjecucion(), p.getFinalizacion(),
                    p.getFinalizacion() - p.getLlegada(),
                    espera.getOrDefault(p.getId(), 0),
                    p.getPrimeraEjecucion() - p.getLlegada()));
        }
        int ocupada = resultado.gantt().stream()
                .filter(t -> !t.esOcioso())
                .mapToInt(TramoGantt::duracion)
                .sum();
        return new ResumenMetricas(resultado.estrategia(), metricas, resultado.instanteFinal(),
                ocupada, contarCambiosDeContexto(resultado.gantt()));
    }

    private static Map<String, Integer> esperaPorProceso(List<EventoTraza> traza) {
        Map<String, Integer> entradaACola = new HashMap<>();
        Map<String, Integer> acumulado = new HashMap<>();
        for (EventoTraza e : traza) {
            if (e.estadoNuevo() == EstadoProceso.LISTO) {
                entradaACola.put(e.procesoId(), e.instante());
            } else if (e.estadoAnterior() == EstadoProceso.LISTO) {
                int desde = entradaACola.remove(e.procesoId());
                acumulado.merge(e.procesoId(), e.instante() - desde, Integer::sum);
            }
        }
        return acumulado;
    }

    /** Veces que la CPU pasa de un proceso a otro distinto, sin contar los intervalos ociosos. */
    private static int contarCambiosDeContexto(List<TramoGantt> gantt) {
        int cambios = 0;
        String anterior = null;
        for (TramoGantt t : gantt) {
            if (t.esOcioso()) {
                continue;
            }
            if (anterior != null && !anterior.equals(t.procesoId())) {
                cambios++;
            }
            anterior = t.procesoId();
        }
        return cambios;
    }
}
