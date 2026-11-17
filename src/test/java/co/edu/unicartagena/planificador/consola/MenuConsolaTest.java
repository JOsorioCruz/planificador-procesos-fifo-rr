package co.edu.unicartagena.planificador.consola;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MenuConsolaTest {

    private String correr(String teclado) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        MenuConsola menu = new MenuConsola(
                new ByteArrayInputStream(teclado.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
        menu.ejecutar();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void cargaUnArchivoYSimulaFifo() {
        String salida = correr("2\nejemplos/video_fifo.txt\n4\nn\n0\n");
        assertTrue(salida.contains("Se cargaron 4 procesos desde video_fifo.txt"));
        assertTrue(salida.contains("=== FIFO ==="));
        assertTrue(salida.contains("Promedio"));
        assertTrue(salida.contains("Fin del simulador."));
    }

    @Test
    void ingresaProcesosPorTecladoYSimulaRoundRobinConTraza() {
        String salida = correr("1\n2\n\n0\n3\nB\n0\n2\n5\n2\ns\n0\n");
        assertTrue(salida.contains("Se cargaron 2 procesos."));
        assertTrue(salida.contains("=== Round Robin (Q=2) ==="));
        assertTrue(salida.contains("agota su quantum"));
    }

    @Test
    void comparaFifoConVariosQuantums() {
        String salida = correr("2\nejemplos/video_round_robin.txt\n6\n2,4\n0\n");
        assertTrue(salida.contains("=== Comparación ==="));
        assertTrue(salida.contains("Round Robin (Q=2)"));
        assertTrue(salida.contains("Round Robin (Q=4)"));
    }

    @Test
    void manejaErroresSinCaerse() {
        String salida = correr("9\n4\n2\nno_existe.txt\n5\n-1\n1\nabc\n0\n");
        assertTrue(salida.contains("Opción no válida"));
        assertTrue(salida.contains("Primero cargue procesos"));
        assertTrue(salida.contains("No se pudo cargar el archivo"));
        assertTrue(salida.contains("Valor inválido"));
        assertTrue(salida.contains("Fin del simulador."));
    }

    @Test
    void pideDeNuevoUnProcesoMalEscrito() {
        String salida = correr("1\n1\n\nx\n3\n\n0\n3 1\n\n0\n3\n0\n");
        assertTrue(salida.contains("Dato inválido"));
        assertTrue(salida.contains("Se cargaron 1 procesos."));
    }

    @Test
    void terminaSiSeAcabaLaEntrada() {
        assertTrue(correr("2\n").contains("Fin del simulador."));
    }
}
