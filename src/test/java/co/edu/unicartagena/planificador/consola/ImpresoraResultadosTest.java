package co.edu.unicartagena.planificador.consola;

import co.edu.unicartagena.planificador.metricas.CalculadoraMetricas;
import co.edu.unicartagena.planificador.metricas.ResumenMetricas;
import co.edu.unicartagena.planificador.modelo.Proceso;
import co.edu.unicartagena.planificador.modelo.Rafaga;
import co.edu.unicartagena.planificador.planificador.PlanificadorFIFO;
import co.edu.unicartagena.planificador.planificador.PlanificadorRoundRobin;
import co.edu.unicartagena.planificador.planificador.ResultadoSimulacion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImpresoraResultadosTest {

    private final List<Proceso> procesos = List.of(
            new Proceso("Word", 0, List.of(Rafaga.cpu(8))),
            new Proceso("Excel", 2, List.of(Rafaga.cpu(6))));

    @Test
    void elGanttMuestraCadaTramoYSusInstantes() {
        String g = ImpresoraResultados.gantt(new PlanificadorFIFO().simular(procesos));
        String[] lineas = g.split("\\R");
        assertTrue(lineas[1].contains("Word"));
        assertTrue(lineas[1].contains("Excel"));
        assertTrue(lineas[3].startsWith("0"));
        assertTrue(lineas[3].contains("8"));
        assertTrue(lineas[3].trim().endsWith("14"));
        // Las tres primeras líneas tienen el mismo ancho.
        assertEquals(lineas[0].length(), lineas[1].length());
        assertEquals(lineas[0].length(), lineas[2].length());
    }

    @Test
    void elGanttMarcaLaCpuOciosa() {
        ResultadoSimulacion r = new PlanificadorFIFO().simular(
                List.of(new Proceso("A", 3, List.of(Rafaga.cpu(1)))));
        assertTrue(ImpresoraResultados.gantt(r).contains("--"));
    }

    @Test
    void laTablaIncluyePromediosUtilizacionYCambios() {
        ResumenMetricas r = CalculadoraMetricas.calcular(new PlanificadorFIFO().simular(procesos));
        String tabla = ImpresoraResultados.tablaMetricas(r);
        assertTrue(tabla.contains("Promedio"));
        assertTrue(tabla.contains("Utilización de CPU: 100.00 %"));
        assertTrue(tabla.contains("Cambios de contexto: 1"));
    }

    @Test
    void laComparativaTieneUnaColumnaPorEstrategia() {
        ResumenMetricas fifo = CalculadoraMetricas.calcular(new PlanificadorFIFO().simular(procesos));
        ResumenMetricas rr = CalculadoraMetricas.calcular(new PlanificadorRoundRobin(2).simular(procesos));
        String tabla = ImpresoraResultados.tablaComparativa(List.of(fifo, rr));
        assertTrue(tabla.contains("FIFO"));
        assertTrue(tabla.contains("Round Robin (Q=2)"));
        assertTrue(tabla.contains("Tiempo promedio de espera"));
    }
}
