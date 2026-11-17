package co.edu.unicartagena.planificador.metricas;

import co.edu.unicartagena.planificador.modelo.Proceso;
import co.edu.unicartagena.planificador.modelo.Rafaga;
import co.edu.unicartagena.planificador.planificador.PlanificadorFIFO;
import co.edu.unicartagena.planificador.planificador.PlanificadorRoundRobin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraMetricasTest {

    private static final double DELTA = 1e-9;

    private static Map<String, MetricasProceso> porId(ResumenMetricas r) {
        return r.procesos().stream().collect(Collectors.toMap(MetricasProceso::procesoId, Function.identity()));
    }

    private static List<Proceso> ejemploVideoFifo() {
        return List.of(
                new Proceso("Word", 0, List.of(Rafaga.cpu(8))),
                new Proceso("Excel", 2, List.of(Rafaga.cpu(6))),
                new Proceso("Paint", 4, List.of(Rafaga.cpu(4))),
                new Proceso("Cal", 6, List.of(Rafaga.cpu(2))));
    }

    private static List<Proceso> ejemploVideoRr() {
        return List.of(
                new Proceso("P1", 0, List.of(Rafaga.cpu(8))),
                new Proceso("P2", 0, List.of(Rafaga.cpu(6))),
                new Proceso("P3", 0, List.of(Rafaga.cpu(4))),
                new Proceso("P4", 0, List.of(Rafaga.cpu(2))));
    }

    /**
     * Valores del video FIFO: sistema 8, 12, 14, 14; espera 0, 6, 10, 12.
     * El video escribe (0 + 6 + 10 + 12) / 4 = 6, pero la suma es 28 y el promedio correcto es 7.
     */
    @Test
    void coincideConLosCalculosDelVideoFifo() {
        ResumenMetricas r = CalculadoraMetricas.calcular(new PlanificadorFIFO().simular(ejemploVideoFifo()));
        Map<String, MetricasProceso> m = porId(r);

        assertEquals(8, m.get("Word").tiempoSistema());
        assertEquals(12, m.get("Excel").tiempoSistema());
        assertEquals(14, m.get("Paint").tiempoSistema());
        assertEquals(14, m.get("Cal").tiempoSistema());

        assertEquals(0, m.get("Word").tiempoEspera());
        assertEquals(6, m.get("Excel").tiempoEspera());
        assertEquals(10, m.get("Paint").tiempoEspera());
        assertEquals(12, m.get("Cal").tiempoEspera());
        assertEquals(7.0, r.promedioEspera(), DELTA);
        assertEquals(12.0, r.promedioSistema(), DELTA);

        assertEquals(100.0, r.utilizacionCpu(), DELTA);
        assertEquals(3, r.cambiosContexto());
    }

    /**
     * Ejemplo del video Round Robin (Q = 2, todos desde 0), calculado a mano.
     * Salidas: P1 20, P2 18, P3 14, P4 8. Espera = sistema - CPU: 12, 12, 10, 6 (promedio 10).
     * Respuesta: 0, 2, 4, 6 (promedio 3). Cambios de contexto: 9.
     */
    @Test
    void calculaElEjemploDelVideoRoundRobin() {
        ResumenMetricas r = CalculadoraMetricas.calcular(new PlanificadorRoundRobin(2).simular(ejemploVideoRr()));
        Map<String, MetricasProceso> m = porId(r);

        assertEquals(12, m.get("P1").tiempoEspera());
        assertEquals(12, m.get("P2").tiempoEspera());
        assertEquals(10, m.get("P3").tiempoEspera());
        assertEquals(6, m.get("P4").tiempoEspera());
        assertEquals(10.0, r.promedioEspera(), DELTA);
        assertEquals(15.0, r.promedioSistema(), DELTA);
        assertEquals(3.0, r.promedioRespuesta(), DELTA);
        assertEquals(9, r.cambiosContexto());
    }

    /**
     * Ejercicio FIFO con E/S de la tarea 06, calculado a mano:
     * P1 sistema 11, espera 4 (de 5 a 9); P2 sistema 6, espera 2; P3 sistema 10, espera 6.
     */
    @Test
    void laEsperaNoCuentaElTiempoEnEntradaSalida() {
        List<Proceso> entrada = List.of(
                new Proceso("P1", 0, List.of(Rafaga.cpu(3), Rafaga.entradaSalida(2), Rafaga.cpu(2))),
                new Proceso("P2", 1, List.of(Rafaga.cpu(4))),
                new Proceso("P3", 2, List.of(Rafaga.cpu(2), Rafaga.entradaSalida(1), Rafaga.cpu(1))));
        ResumenMetricas r = CalculadoraMetricas.calcular(new PlanificadorFIFO().simular(entrada));
        Map<String, MetricasProceso> m = porId(r);

        assertEquals(11, m.get("P1").tiempoSistema());
        assertEquals(4, m.get("P1").tiempoEspera());
        assertEquals(2, m.get("P2").tiempoEspera());
        assertEquals(6, m.get("P3").tiempoEspera());
        for (MetricasProceso p : r.procesos()) {
            // Como cada proceso hace su propia E/S, sistema = CPU + E/S + espera.
            assertEquals(p.tiempoSistema(), p.totalCpu() + p.totalEntradaSalida() + p.tiempoEspera());
        }
    }

    @Test
    void laCpuOciosaBajaLaUtilizacion() {
        List<Proceso> entrada = List.of(
                new Proceso("A", 2, List.of(Rafaga.cpu(1), Rafaga.entradaSalida(2), Rafaga.cpu(1))));
        ResumenMetricas r = CalculadoraMetricas.calcular(new PlanificadorFIFO().simular(entrada));
        assertEquals(2, r.tiempoCpuOcupada());
        assertEquals(6, r.instanteFinal());
        assertEquals(100.0 * 2 / 6, r.utilizacionCpu(), DELTA);
        assertEquals(0, r.cambiosContexto());
    }

    @Test
    void elMismoProcesoRedespachadoNoEsCambioDeContexto() {
        List<Proceso> entrada = List.of(new Proceso("B", 0, List.of(Rafaga.cpu(6))));
        ResumenMetricas r = CalculadoraMetricas.calcular(new PlanificadorRoundRobin(2).simular(entrada));
        assertEquals(0, r.cambiosContexto());
        assertEquals(0, r.procesos().get(0).tiempoEspera());
    }
}
