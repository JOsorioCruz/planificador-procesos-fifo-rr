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

class PlanificadorFIFOTest {

    private final Planificador fifo = new PlanificadorFIFO();

    private static Map<String, Proceso> porId(ResultadoSimulacion r) {
        return r.procesos().stream().collect(Collectors.toMap(Proceso::getId, Function.identity()));
    }

    /**
     * Ejemplo del video FIFO del curso: Word (8, llega 0), Excel (6, llega 2),
     * Paint (4, llega 4) y Cal (2, llega 6).
     * Tiempo del sistema: 8, 12, 14, 14. Tiempo de espera: 0, 6, 10, 12.
     */
    @Test
    void reproduceElEjemploDelVideo() {
        List<Proceso> entrada = List.of(
                new Proceso("Word", 0, List.of(Rafaga.cpu(8))),
                new Proceso("Excel", 2, List.of(Rafaga.cpu(6))),
                new Proceso("Paint", 4, List.of(Rafaga.cpu(4))),
                new Proceso("Cal", 6, List.of(Rafaga.cpu(2))));

        ResultadoSimulacion r = fifo.simular(entrada);

        assertEquals(List.of(
                new TramoGantt("Word", 0, 8),
                new TramoGantt("Excel", 8, 14),
                new TramoGantt("Paint", 14, 18),
                new TramoGantt("Cal", 18, 20)), r.gantt());

        Map<String, Proceso> p = porId(r);
        // Tiempo del sistema = salida - llegada
        assertEquals(8, p.get("Word").getFinalizacion() - p.get("Word").getLlegada());
        assertEquals(12, p.get("Excel").getFinalizacion() - p.get("Excel").getLlegada());
        assertEquals(14, p.get("Paint").getFinalizacion() - p.get("Paint").getLlegada());
        assertEquals(14, p.get("Cal").getFinalizacion() - p.get("Cal").getLlegada());
        // Tiempo de espera = inicio de ejecución - llegada
        assertEquals(0, p.get("Word").getPrimeraEjecucion() - p.get("Word").getLlegada());
        assertEquals(6, p.get("Excel").getPrimeraEjecucion() - p.get("Excel").getLlegada());
        assertEquals(10, p.get("Paint").getPrimeraEjecucion() - p.get("Paint").getLlegada());
        assertEquals(12, p.get("Cal").getPrimeraEjecucion() - p.get("Cal").getLlegada());
        assertEquals(20, r.instanteFinal());
    }

    /**
     * Ejercicio resuelto a mano con entrada/salida:
     * P1 llega en 0 con CPU 3, E/S 2, CPU 2; P2 llega en 1 con CPU 4;
     * P3 llega en 2 con CPU 2, E/S 1, CPU 1.
     * Gantt esperado: P1 [0,3) P2 [3,7) P3 [7,9) P1 [9,11) P3 [11,12).
     */
    @Test
    void liberaLaCpuCuandoElProcesoPideEntradaSalida() {
        List<Proceso> entrada = List.of(
                new Proceso("P1", 0, List.of(Rafaga.cpu(3), Rafaga.entradaSalida(2), Rafaga.cpu(2))),
                new Proceso("P2", 1, List.of(Rafaga.cpu(4))),
                new Proceso("P3", 2, List.of(Rafaga.cpu(2), Rafaga.entradaSalida(1), Rafaga.cpu(1))));

        ResultadoSimulacion r = fifo.simular(entrada);

        assertEquals(List.of(
                new TramoGantt("P1", 0, 3),
                new TramoGantt("P2", 3, 7),
                new TramoGantt("P3", 7, 9),
                new TramoGantt("P1", 9, 11),
                new TramoGantt("P3", 11, 12)), r.gantt());

        Map<String, Proceso> p = porId(r);
        assertEquals(11, p.get("P1").getFinalizacion());
        assertEquals(7, p.get("P2").getFinalizacion());
        assertEquals(12, p.get("P3").getFinalizacion());
        assertTrue(r.procesos().stream().allMatch(x -> x.getEstado() == EstadoProceso.TERMINADO));

        // P1 termina su E/S en 5 y queda detrás de P3, que estaba listo desde 2.
        assertTrue(r.traza().contains(new EventoTraza(5, "P1", EstadoProceso.BLOQUEADO,
                EstadoProceso.LISTO, CausaEvento.FIN_ENTRADA_SALIDA)));
        assertTrue(r.traza().contains(new EventoTraza(3, "P1", EstadoProceso.EJECUTANDO,
                EstadoProceso.BLOQUEADO, CausaEvento.PETICION_ENTRADA_SALIDA)));
    }

    @Test
    void registraLaCpuOciosaCuandoNoHayProcesosListos() {
        List<Proceso> entrada = List.of(
                new Proceso("A", 2, List.of(Rafaga.cpu(1), Rafaga.entradaSalida(2), Rafaga.cpu(1))));

        ResultadoSimulacion r = fifo.simular(entrada);

        assertEquals(List.of(
                new TramoGantt(TramoGantt.OCIOSO, 0, 2),
                new TramoGantt("A", 2, 3),
                new TramoGantt(TramoGantt.OCIOSO, 3, 5),
                new TramoGantt("A", 5, 6)), r.gantt());
    }

    @Test
    void nuncaExpulsaPorTiempo() {
        Proceso largo = new Proceso("Largo", 0, List.of(Rafaga.cpu(50)));
        Proceso corto = new Proceso("Corto", 0, List.of(Rafaga.cpu(1)));
        ResultadoSimulacion r = fifo.simular(List.of(largo, corto));
        assertEquals(List.of(new TramoGantt("Largo", 0, 50), new TramoGantt("Corto", 50, 51)), r.gantt());
        assertTrue(r.traza().stream().noneMatch(e -> e.causa() == CausaEvento.QUANTUM_AGOTADO));
    }

    @Test
    void noModificaLosProcesosDeEntrada() {
        Proceso original = new Proceso("A", 0, List.of(Rafaga.cpu(2)));
        fifo.simular(List.of(original));
        assertEquals(EstadoProceso.NUEVO, original.getEstado());
    }

    @Test
    void validaLaEntrada() {
        assertThrows(IllegalArgumentException.class, () -> fifo.simular(List.of()));
        Proceso a = new Proceso("A", 0, List.of(Rafaga.cpu(1)));
        Proceso a2 = new Proceso("A", 1, List.of(Rafaga.cpu(1)));
        assertThrows(IllegalArgumentException.class, () -> fifo.simular(List.of(a, a2)));
    }
}
