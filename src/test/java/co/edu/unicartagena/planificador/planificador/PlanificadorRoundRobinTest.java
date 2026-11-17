package co.edu.unicartagena.planificador.planificador;

import co.edu.unicartagena.planificador.modelo.EstadoProceso;
import co.edu.unicartagena.planificador.modelo.Proceso;
import co.edu.unicartagena.planificador.modelo.Rafaga;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PlanificadorRoundRobinTest {

    private static Map<String, Proceso> porId(ResultadoSimulacion r) {
        return r.procesos().stream().collect(Collectors.toMap(Proceso::getId, Function.identity()));
    }

    private static TramoGantt t(String id, int inicio, int fin) {
        return new TramoGantt(id, inicio, fin);
    }

    /**
     * Ejemplo del video Round Robin del curso: Q = 2, P1 (8), P2 (6), P3 (4), P4 (2),
     * en la cola en ese orden desde el inicio. Orden de turnos del video:
     * P1, P2, P3, P4, P1, P2, P3, P1, P2, P1.
     */
    @Test
    void reproduceElEjemploDelVideo() {
        List<Proceso> entrada = List.of(
                new Proceso("P1", 0, List.of(Rafaga.cpu(8))),
                new Proceso("P2", 0, List.of(Rafaga.cpu(6))),
                new Proceso("P3", 0, List.of(Rafaga.cpu(4))),
                new Proceso("P4", 0, List.of(Rafaga.cpu(2))));

        ResultadoSimulacion r = new PlanificadorRoundRobin(2).simular(entrada);

        assertEquals(List.of(
                t("P1", 0, 2), t("P2", 2, 4), t("P3", 4, 6), t("P4", 6, 8),
                t("P1", 8, 10), t("P2", 10, 12), t("P3", 12, 14),
                t("P1", 14, 16), t("P2", 16, 18), t("P1", 18, 20)), r.gantt());

        Map<String, Proceso> p = porId(r);
        assertEquals(8, p.get("P4").getFinalizacion());
        assertEquals(14, p.get("P3").getFinalizacion());
        assertEquals(18, p.get("P2").getFinalizacion());
        assertEquals(20, p.get("P1").getFinalizacion());
    }

    /**
     * Mismo caso, pero con llegadas reales en 0, 1, 2 y 3. Resuelto a mano:
     * en t=2 llega P3 y P1 agota su quantum; P3 entra a la cola antes que P1.
     * Gantt: P1 0-2, P2 2-4, P3 4-6, P1 6-8, P4 8-10, P2 10-12, P3 12-14,
     * P1 14-16, P2 16-18, P1 18-20.
     */
    @Test
    void conLlegadasRealesElOrdenCambia() {
        List<Proceso> entrada = List.of(
                new Proceso("P1", 0, List.of(Rafaga.cpu(8))),
                new Proceso("P2", 1, List.of(Rafaga.cpu(6))),
                new Proceso("P3", 2, List.of(Rafaga.cpu(4))),
                new Proceso("P4", 3, List.of(Rafaga.cpu(2))));

        ResultadoSimulacion r = new PlanificadorRoundRobin(2).simular(entrada);

        assertEquals(List.of(
                t("P1", 0, 2), t("P2", 2, 4), t("P3", 4, 6), t("P1", 6, 8),
                t("P4", 8, 10), t("P2", 10, 12), t("P3", 12, 14),
                t("P1", 14, 16), t("P2", 16, 18), t("P1", 18, 20)), r.gantt());

        // Desempate en t=2: primero la llegada de P3, después el expulsado P1.
        int llegadaP3 = r.traza().indexOf(new EventoTraza(2, "P3", EstadoProceso.NUEVO,
                EstadoProceso.LISTO, CausaEvento.LLEGADA));
        int expulsionP1 = r.traza().indexOf(new EventoTraza(2, "P1", EstadoProceso.EJECUTANDO,
                EstadoProceso.LISTO, CausaEvento.QUANTUM_AGOTADO));
        assertTrue(llegadaP3 >= 0 && expulsionP1 > llegadaP3);
    }

    /** Si la ráfaga es menor que el quantum, el proceso termina antes y se planifica otro. */
    @Test
    void conQuantumMayorQueLaRafagaNoSeDesperdiciaTiempo() {
        List<Proceso> entrada = List.of(
                new Proceso("A", 0, List.of(Rafaga.cpu(3))),
                new Proceso("B", 0, List.of(Rafaga.cpu(6))));

        ResultadoSimulacion r = new PlanificadorRoundRobin(4).simular(entrada);

        // B agota su quantum en 7, la cola está vacía y vuelve a recibir la CPU: tramo nuevo.
        assertEquals(List.of(t("A", 0, 3), t("B", 3, 7), t("B", 7, 9)), r.gantt());
        Map<String, Proceso> p = porId(r);
        assertEquals(3, p.get("A").getFinalizacion());
        assertEquals(9, p.get("B").getFinalizacion());
    }

    /**
     * Interrupción por entrada/salida. Q = 2; P1 llega en 0 con CPU 3, E/S 1, CPU 1;
     * P2 llega en 0 con CPU 2. Resuelto a mano:
     * P1 0-2 (quantum), P2 2-4 (termina), P1 4-5 (pide E/S), CPU ociosa 5-6, P1 6-7 (termina).
     */
    @Test
    void liberaLaCpuPorEntradaSalidaAntesDelQuantum() {
        List<Proceso> entrada = List.of(
                new Proceso("P1", 0, List.of(Rafaga.cpu(3), Rafaga.entradaSalida(1), Rafaga.cpu(1))),
                new Proceso("P2", 0, List.of(Rafaga.cpu(2))));

        ResultadoSimulacion r = new PlanificadorRoundRobin(2).simular(entrada);

        assertEquals(List.of(t("P1", 0, 2), t("P2", 2, 4), t("P1", 4, 5),
                t(TramoGantt.OCIOSO, 5, 6), t("P1", 6, 7)), r.gantt());
        assertTrue(r.traza().contains(new EventoTraza(5, "P1", EstadoProceso.EJECUTANDO,
                EstadoProceso.BLOQUEADO, CausaEvento.PETICION_ENTRADA_SALIDA)));
        assertTrue(r.traza().contains(new EventoTraza(7, "P1", EstadoProceso.EJECUTANDO,
                EstadoProceso.TERMINADO, CausaEvento.FINALIZACION)));
    }

    /** Con un quantum enorme, Round Robin se comporta igual que FIFO. */
    @Test
    void conQuantumMuyGrandeEquivaleAFifo() {
        List<Proceso> entrada = List.of(
                new Proceso("Word", 0, List.of(Rafaga.cpu(8))),
                new Proceso("Excel", 2, List.of(Rafaga.cpu(6))),
                new Proceso("Paint", 4, List.of(Rafaga.cpu(4))),
                new Proceso("Cal", 6, List.of(Rafaga.cpu(2))));

        assertEquals(new PlanificadorFIFO().simular(entrada).gantt(),
                new PlanificadorRoundRobin(100).simular(entrada).gantt());
    }

    @Test
    void rechazaQuantumNoPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new PlanificadorRoundRobin(0));
        assertThrows(IllegalArgumentException.class, () -> new PlanificadorRoundRobin(-3));
    }

    @Test
    void elNombreIncluyeElQuantum() {
        assertEquals("Round Robin (Q=3)", new PlanificadorRoundRobin(3).getNombre());
    }
}
