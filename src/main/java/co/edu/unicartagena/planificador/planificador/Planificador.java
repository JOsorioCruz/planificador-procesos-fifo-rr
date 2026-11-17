package co.edu.unicartagena.planificador.planificador;

import co.edu.unicartagena.planificador.modelo.ColaListos;
import co.edu.unicartagena.planificador.modelo.EstadoProceso;
import co.edu.unicartagena.planificador.modelo.ListaBloqueados;
import co.edu.unicartagena.planificador.modelo.Proceso;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ciclo de simulación común a las estrategias de planificación.
 *
 * El tiempo avanza en unidades enteras. En cada instante t se hace, en orden:
 * 1. Entran a la cola de listos los procesos que llegan en t.
 * 2. Entran los procesos que terminaron su E/S en t.
 * 3. Entra el proceso expulsado de la CPU en t, si lo hay.
 * 4. Si la CPU está libre, se despacha el primero de la cola.
 * 5. Durante [t, t+1) avanza la E/S de los bloqueados y el proceso en CPU
 *    consume una unidad. Al llegar a t+1 se revisa si terminó su ráfaga
 *    (pasa a bloqueado o a terminado) o si la estrategia decide expulsarlo.
 *
 * Cada estrategia solo define cuándo se expulsa a un proceso que sigue
 * necesitando la CPU.
 */
public abstract class Planificador {

    /** Nombre de la estrategia para mostrar en resultados. */
    public abstract String getNombre();

    /**
     * Indica si el proceso en CPU debe dejarla aunque no haya terminado su ráfaga.
     *
     * @param unidadesContinuas unidades de CPU que lleva desde que fue despachado
     */
    protected abstract boolean debeExpulsar(int unidadesContinuas);

    /**
     * Simula la ejecución de los procesos. Trabaja sobre copias limpias,
     * así la misma lista se puede simular con otra estrategia.
     */
    public final ResultadoSimulacion simular(List<Proceso> entrada) {
        if (entrada == null || entrada.isEmpty()) {
            throw new IllegalArgumentException("Se necesita al menos un proceso para simular");
        }
        validarIdentificadoresUnicos(entrada);
        Ejecucion ejecucion = new Ejecucion(entrada);
        ejecucion.correr();
        return new ResultadoSimulacion(getNombre(), ejecucion.gantt, ejecucion.traza, ejecucion.procesos);
    }

    private static void validarIdentificadoresUnicos(List<Proceso> entrada) {
        long distintos = entrada.stream().map(Proceso::getId).distinct().count();
        if (distintos != entrada.size()) {
            throw new IllegalArgumentException("Los identificadores de los procesos deben ser únicos");
        }
    }

    /** Estado de una corrida concreta de la simulación. */
    private final class Ejecucion {

        private final List<Proceso> procesos = new ArrayList<>();
        private final List<Proceso> porLlegar;
        private final ColaListos listos = new ColaListos();
        private final ListaBloqueados bloqueados = new ListaBloqueados();
        private final List<TramoGantt> gantt = new ArrayList<>();
        private final List<EventoTraza> traza = new ArrayList<>();

        private List<Proceso> terminaronEntradaSalida = new ArrayList<>();
        private Proceso expulsado;
        private Proceso enCpu;
        private int unidadesContinuas;
        private boolean nuevoDespacho;
        private int terminados;

        Ejecucion(List<Proceso> entrada) {
            for (Proceso original : entrada) {
                procesos.add(original.copiaNueva());
            }
            // Orden de llegada; si dos llegan juntos se respeta el orden de la entrada.
            porLlegar = new ArrayList<>(procesos);
            porLlegar.sort(Comparator.comparingInt(Proceso::getLlegada));
        }

        void correr() {
            int t = 0;
            while (terminados < procesos.size()) {
                admitirLlegadas(t);
                encolarFinesDeEntradaSalida(t);
                encolarExpulsado(t);
                despacharSiCpuLibre(t);
                avanzarUnaUnidad(t);
                t++;
            }
        }

        private void admitirLlegadas(int t) {
            while (!porLlegar.isEmpty() && porLlegar.get(0).getLlegada() == t) {
                Proceso p = porLlegar.remove(0);
                p.admitir();
                registrar(t, p, EstadoProceso.NUEVO, CausaEvento.LLEGADA);
                listos.encolar(p);
            }
        }

        private void encolarFinesDeEntradaSalida(int t) {
            for (Proceso p : terminaronEntradaSalida) {
                registrar(t, p, EstadoProceso.BLOQUEADO, CausaEvento.FIN_ENTRADA_SALIDA);
                listos.encolar(p);
            }
            terminaronEntradaSalida = new ArrayList<>();
        }

        private void encolarExpulsado(int t) {
            if (expulsado != null) {
                expulsado.expulsarPorQuantum();
                registrar(t, expulsado, EstadoProceso.EJECUTANDO, CausaEvento.QUANTUM_AGOTADO);
                listos.encolar(expulsado);
                expulsado = null;
            }
        }

        private void despacharSiCpuLibre(int t) {
            if (enCpu == null) {
                listos.desencolar().ifPresent(p -> {
                    p.despachar(t);
                    registrar(t, p, EstadoProceso.LISTO, CausaEvento.DESPACHO);
                    enCpu = p;
                    unidadesContinuas = 0;
                    nuevoDespacho = true;
                });
            }
        }

        private void avanzarUnaUnidad(int t) {
            // La E/S avanza primero, así un proceso que se bloquea en t+1 no adelanta E/S en [t, t+1).
            terminaronEntradaSalida = bloqueados.avanzarUnaUnidad();

            if (enCpu == null) {
                anotarEnGantt(TramoGantt.OCIOSO, t, false);
                return;
            }
            anotarEnGantt(enCpu.getId(), t, nuevoDespacho);
            nuevoDespacho = false;
            unidadesContinuas++;

            boolean rafagaTerminada = enCpu.ejecutarUnaUnidad();
            int siguiente = t + 1;
            if (rafagaTerminada) {
                EstadoProceso nuevo = enCpu.finalizarRafagaCpu(siguiente);
                if (nuevo == EstadoProceso.TERMINADO) {
                    registrar(siguiente, enCpu, EstadoProceso.EJECUTANDO, CausaEvento.FINALIZACION);
                    terminados++;
                } else {
                    registrar(siguiente, enCpu, EstadoProceso.EJECUTANDO, CausaEvento.PETICION_ENTRADA_SALIDA);
                    bloqueados.agregar(enCpu);
                }
                enCpu = null;
            } else if (debeExpulsar(unidadesContinuas)) {
                expulsado = enCpu;
                enCpu = null;
            }
        }

        /**
         * Agrega una unidad al Gantt. Si continúa el mismo despacho (o sigue ocioso),
         * alarga el último tramo; si no, abre uno nuevo.
         */
        private void anotarEnGantt(String id, int t, boolean abrirTramoNuevo) {
            if (!gantt.isEmpty() && !abrirTramoNuevo) {
                TramoGantt ultimo = gantt.get(gantt.size() - 1);
                if (ultimo.procesoId().equals(id) && ultimo.fin() == t) {
                    gantt.set(gantt.size() - 1, ultimo.extenderHasta(t + 1));
                    return;
                }
            }
            gantt.add(new TramoGantt(id, t, t + 1));
        }

        private void registrar(int t, Proceso p, EstadoProceso anterior, CausaEvento causa) {
            traza.add(new EventoTraza(t, p.getId(), anterior, p.getEstado(), causa));
        }
    }
}
