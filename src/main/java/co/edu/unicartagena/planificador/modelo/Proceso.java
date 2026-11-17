package co.edu.unicartagena.planificador.modelo;

import java.util.List;
import java.util.Objects;

/**
 * Proceso del simulador. Guarda sus datos de entrada (identificador,
 * instante de llegada y ráfagas) y su estado durante la simulación.
 *
 * Las ráfagas alternan CPU y entrada/salida, empiezan y terminan en CPU.
 * Cada cambio de estado pasa por los métodos de esta clase, que validan
 * que la transición sea permitida.
 */
public class Proceso {

    private static final int SIN_REGISTRO = -1;

    private final String id;
    private final int llegada;
    private final List<Rafaga> rafagas;

    private EstadoProceso estado;
    private int indiceRafaga;
    private int restanteRafaga;
    private int primeraEjecucion;
    private int finalizacion;

    public Proceso(String id, int llegada, List<Rafaga> rafagas) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador del proceso es obligatorio");
        }
        if (llegada < 0) {
            throw new IllegalArgumentException("El instante de llegada no puede ser negativo");
        }
        validarRafagas(rafagas);
        this.id = id.trim();
        this.llegada = llegada;
        this.rafagas = List.copyOf(rafagas);
        this.estado = EstadoProceso.NUEVO;
        this.indiceRafaga = 0;
        this.restanteRafaga = this.rafagas.get(0).getDuracion();
        this.primeraEjecucion = SIN_REGISTRO;
        this.finalizacion = SIN_REGISTRO;
    }

    private static void validarRafagas(List<Rafaga> rafagas) {
        if (rafagas == null || rafagas.isEmpty()) {
            throw new IllegalArgumentException("El proceso necesita al menos una ráfaga de CPU");
        }
        for (int i = 0; i < rafagas.size(); i++) {
            Rafaga rafaga = Objects.requireNonNull(rafagas.get(i), "Las ráfagas no pueden ser nulas");
            boolean debeSerCpu = i % 2 == 0;
            if (rafaga.esCpu() != debeSerCpu) {
                throw new IllegalArgumentException(
                        "Las ráfagas deben alternar CPU y E/S empezando por CPU (posición " + (i + 1) + ")");
            }
        }
        if (!rafagas.get(rafagas.size() - 1).esCpu()) {
            throw new IllegalArgumentException("La última ráfaga del proceso debe ser de CPU");
        }
    }

    /**
     * Devuelve un proceso nuevo con los mismos datos de entrada y estado inicial.
     * Sirve para simular el mismo conjunto de procesos con otra estrategia.
     */
    public Proceso copiaNueva() {
        return new Proceso(id, llegada, rafagas);
    }

    // Transiciones de estado

    /** El proceso llega al sistema y entra a la cola de listos. */
    public void admitir() {
        cambiarEstado(EstadoProceso.LISTO);
    }

    /** El planificador le entrega la CPU en el instante indicado. */
    public void despachar(int instante) {
        cambiarEstado(EstadoProceso.EJECUTANDO);
        if (primeraEjecucion == SIN_REGISTRO) {
            primeraEjecucion = instante;
        }
    }

    /** El proceso agotó su quantum y vuelve a la cola de listos. */
    public void expulsarPorQuantum() {
        if (rafagaActualTerminada()) {
            throw new IllegalStateException("El proceso " + id + " ya terminó su ráfaga; no se expulsa por quantum");
        }
        cambiarEstado(EstadoProceso.LISTO);
    }

    /**
     * Consume una unidad de CPU de la ráfaga actual.
     *
     * @return true si con esta unidad se terminó la ráfaga de CPU
     */
    public boolean ejecutarUnaUnidad() {
        exigirEstado(EstadoProceso.EJECUTANDO);
        if (rafagaActualTerminada()) {
            throw new IllegalStateException("La ráfaga de CPU del proceso " + id + " ya terminó");
        }
        restanteRafaga--;
        return rafagaActualTerminada();
    }

    /**
     * Cierra la ráfaga de CPU terminada. Si sigue una ráfaga de E/S, el proceso
     * pasa a bloqueado; si era la última, pasa a terminado en el instante indicado.
     *
     * @return el nuevo estado del proceso
     */
    public EstadoProceso finalizarRafagaCpu(int instante) {
        exigirEstado(EstadoProceso.EJECUTANDO);
        if (!rafagaActualTerminada()) {
            throw new IllegalStateException("La ráfaga de CPU del proceso " + id + " no ha terminado");
        }
        if (esUltimaRafaga()) {
            cambiarEstado(EstadoProceso.TERMINADO);
            finalizacion = instante;
        } else {
            avanzarARafagaSiguiente();
            cambiarEstado(EstadoProceso.BLOQUEADO);
        }
        return estado;
    }

    /**
     * Consume una unidad de la ráfaga de E/S actual.
     *
     * @return true si con esta unidad se terminó la E/S
     */
    public boolean avanzarEntradaSalida() {
        exigirEstado(EstadoProceso.BLOQUEADO);
        if (rafagaActualTerminada()) {
            throw new IllegalStateException("La E/S del proceso " + id + " ya terminó");
        }
        restanteRafaga--;
        return rafagaActualTerminada();
    }

    /** Termina la E/S: el proceso pasa a la siguiente ráfaga de CPU y queda listo. */
    public void finalizarEntradaSalida() {
        exigirEstado(EstadoProceso.BLOQUEADO);
        if (!rafagaActualTerminada()) {
            throw new IllegalStateException("La E/S del proceso " + id + " no ha terminado");
        }
        avanzarARafagaSiguiente();
        cambiarEstado(EstadoProceso.LISTO);
    }

    private void avanzarARafagaSiguiente() {
        indiceRafaga++;
        restanteRafaga = rafagas.get(indiceRafaga).getDuracion();
    }

    private void cambiarEstado(EstadoProceso destino) {
        if (!estado.puedePasarA(destino)) {
            throw new IllegalStateException(
                    "Transición inválida del proceso " + id + ": " + estado + " a " + destino);
        }
        estado = destino;
    }

    private void exigirEstado(EstadoProceso esperado) {
        if (estado != esperado) {
            throw new IllegalStateException(
                    "El proceso " + id + " debe estar " + esperado + " pero está " + estado);
        }
    }

    private boolean rafagaActualTerminada() {
        return restanteRafaga == 0;
    }

    private boolean esUltimaRafaga() {
        return indiceRafaga == rafagas.size() - 1;
    }

    // Consultas

    public String getId() {
        return id;
    }

    public int getLlegada() {
        return llegada;
    }

    public List<Rafaga> getRafagas() {
        return rafagas;
    }

    public EstadoProceso getEstado() {
        return estado;
    }

    public Rafaga getRafagaActual() {
        return rafagas.get(indiceRafaga);
    }

    public int getRestanteRafagaActual() {
        return restanteRafaga;
    }

    public int getTotalCpu() {
        return rafagas.stream().filter(Rafaga::esCpu).mapToInt(Rafaga::getDuracion).sum();
    }

    public int getTotalEntradaSalida() {
        return rafagas.stream().filter(r -> !r.esCpu()).mapToInt(Rafaga::getDuracion).sum();
    }

    public boolean haSidoEjecutado() {
        return primeraEjecucion != SIN_REGISTRO;
    }

    /** Instante en que recibió la CPU por primera vez. */
    public int getPrimeraEjecucion() {
        if (!haSidoEjecutado()) {
            throw new IllegalStateException("El proceso " + id + " aún no ha recibido la CPU");
        }
        return primeraEjecucion;
    }

    /** Instante en que terminó. */
    public int getFinalizacion() {
        if (estado != EstadoProceso.TERMINADO) {
            throw new IllegalStateException("El proceso " + id + " aún no ha terminado");
        }
        return finalizacion;
    }

    @Override
    public String toString() {
        return id + " [llegada=" + llegada + ", ráfagas=" + rafagas + ", estado=" + estado + "]";
    }
}
