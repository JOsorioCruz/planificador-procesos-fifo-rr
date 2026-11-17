package co.edu.unicartagena.planificador.consola;

import co.edu.unicartagena.planificador.metricas.MetricasProceso;
import co.edu.unicartagena.planificador.metricas.ResumenMetricas;
import co.edu.unicartagena.planificador.planificador.EventoTraza;
import co.edu.unicartagena.planificador.planificador.ResultadoSimulacion;
import co.edu.unicartagena.planificador.planificador.TramoGantt;

import java.util.List;
import java.util.Locale;

/**
 * Arma en texto el diagrama de Gantt, la traza, la tabla de métricas
 * y la tabla comparativa entre estrategias.
 */
public final class ImpresoraResultados {

    private static final String SALTO = System.lineSeparator();

    private ImpresoraResultados() {
    }

    /**
     * Gantt en texto. Cada tramo ocupa un ancho proporcional a su duración,
     * con un mínimo para que quepan el nombre y el instante final.
     */
    public static String gantt(ResultadoSimulacion resultado) {
        List<TramoGantt> tramos = resultado.gantt();
        int escala = resultado.instanteFinal() <= 40 ? 2 : 1;

        StringBuilder borde = new StringBuilder("+");
        StringBuilder nombres = new StringBuilder("|");
        StringBuilder tiempos = new StringBuilder("0");
        for (TramoGantt t : tramos) {
            String etiqueta = t.esOcioso() ? "--" : t.procesoId();
            int ancho = Math.max(t.duracion() * escala,
                    Math.max(etiqueta.length() + 2, String.valueOf(t.fin()).length() + 1));
            borde.append("-".repeat(ancho)).append('+');
            nombres.append(centrar(etiqueta, ancho)).append('|');
            int columnaFin = borde.length() - 1;
            String fin = String.valueOf(t.fin());
            int relleno = columnaFin - tiempos.length();
            tiempos.append(" ".repeat(Math.max(1, relleno))).append(fin);
        }
        return borde + SALTO + nombres + SALTO + borde + SALTO + tiempos + SALTO
                + "(-- = CPU ociosa)" + SALTO;
    }

    public static String traza(ResultadoSimulacion resultado) {
        StringBuilder sb = new StringBuilder();
        for (EventoTraza e : resultado.traza()) {
            sb.append(String.format("t=%-4d %-10s %-28s %s -> %s%n", e.instante(), e.procesoId(),
                    e.causa().getDescripcion(), e.estadoAnterior(), e.estadoNuevo()));
        }
        return sb.toString();
    }

    public static String tablaMetricas(ResumenMetricas resumen) {
        StringBuilder sb = new StringBuilder();
        String formato = "%-10s %8s %5s %5s %7s %8s %8s %8s %9s%n";
        sb.append(String.format(formato, "Proceso", "Llegada", "CPU", "E/S", "Inicio",
                "Salida", "Sistema", "Espera", "Respuesta"));
        for (MetricasProceso m : resumen.procesos()) {
            sb.append(String.format(formato, m.procesoId(), m.llegada(), m.totalCpu(),
                    m.totalEntradaSalida(), m.primeraEjecucion(), m.finalizacion(),
                    m.tiempoSistema(), m.tiempoEspera(), m.tiempoRespuesta()));
        }
        sb.append(String.format(Locale.ROOT, "%-10s %8s %5s %5s %7s %8s %8.2f %8.2f %9.2f%n", "Promedio",
                "", "", "", "", "", resumen.promedioSistema(), resumen.promedioEspera(),
                resumen.promedioRespuesta()));
        sb.append(String.format(Locale.ROOT, "Utilización de CPU: %.2f %% (%d de %d unidades)%n",
                resumen.utilizacionCpu(), resumen.tiempoCpuOcupada(), resumen.instanteFinal()));
        sb.append("Cambios de contexto: ").append(resumen.cambiosContexto()).append(SALTO);
        return sb.toString();
    }

    /** Una columna por estrategia, una fila por indicador. */
    public static String tablaComparativa(List<ResumenMetricas> resumenes) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-30s", "Indicador"));
        for (ResumenMetricas r : resumenes) {
            sb.append(String.format("%22s", r.estrategia()));
        }
        sb.append(SALTO);
        fila(sb, "Tiempo promedio del sistema", resumenes, ResumenMetricas::promedioSistema);
        fila(sb, "Tiempo promedio de espera", resumenes, ResumenMetricas::promedioEspera);
        fila(sb, "Tiempo promedio de respuesta", resumenes, ResumenMetricas::promedioRespuesta);
        fila(sb, "Utilización de CPU (%)", resumenes, ResumenMetricas::utilizacionCpu);
        fila(sb, "Cambios de contexto", resumenes, r -> r.cambiosContexto());
        fila(sb, "Instante final", resumenes, r -> r.instanteFinal());
        return sb.toString();
    }

    private static void fila(StringBuilder sb, String nombre, List<ResumenMetricas> resumenes,
                             java.util.function.ToDoubleFunction<ResumenMetricas> valor) {
        sb.append(String.format("%-30s", nombre));
        for (ResumenMetricas r : resumenes) {
            sb.append(String.format(Locale.ROOT, "%22.2f", valor.applyAsDouble(r)));
        }
        sb.append(SALTO);
    }

    private static String centrar(String texto, int ancho) {
        int izquierda = (ancho - texto.length()) / 2;
        int derecha = ancho - texto.length() - izquierda;
        return " ".repeat(izquierda) + texto + " ".repeat(derecha);
    }
}
