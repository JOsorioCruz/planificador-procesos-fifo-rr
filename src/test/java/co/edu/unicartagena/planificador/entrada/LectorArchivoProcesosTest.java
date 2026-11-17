package co.edu.unicartagena.planificador.entrada;

import co.edu.unicartagena.planificador.modelo.Proceso;
import co.edu.unicartagena.planificador.modelo.Rafaga;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LectorArchivoProcesosTest {

    @Test
    void leeProcesosIgnorandoComentariosYLineasVacias() throws Exception {
        List<Proceso> ps = LectorArchivoProcesos.leerLineas(List.of(
                "# comentario", "", "P1; 0; 3; 2; 2", "  P2;1;4  "));
        assertEquals(2, ps.size());
        assertEquals("P1", ps.get(0).getId());
        assertEquals(List.of(Rafaga.cpu(3), Rafaga.entradaSalida(2), Rafaga.cpu(2)), ps.get(0).getRafagas());
        assertEquals(1, ps.get(1).getLlegada());
    }

    @Test
    void indicaLaLineaDelError() {
        ErrorFormatoProcesos e = assertThrows(ErrorFormatoProcesos.class,
                () -> LectorArchivoProcesos.leerLineas(List.of("# encabezado", "P1;0;3", "P2;x;4")));
        assertEquals(3, e.getLinea());
        assertTrue(e.getMessage().contains("Línea 3"));
    }

    @Test
    void rechazaDatosInvalidos() {
        assertThrows(ErrorFormatoProcesos.class, () -> LectorArchivoProcesos.leerLineas(List.of("P1;0")));
        assertThrows(ErrorFormatoProcesos.class, () -> LectorArchivoProcesos.leerLineas(List.of("P1;-1;3")));
        assertThrows(ErrorFormatoProcesos.class, () -> LectorArchivoProcesos.leerLineas(List.of("P1;0;3;2")));
        assertThrows(ErrorFormatoProcesos.class, () -> LectorArchivoProcesos.leerLineas(List.of("P1;0;0")));
        assertThrows(ErrorFormatoProcesos.class, () -> LectorArchivoProcesos.leerLineas(List.of(";0;3")));
        assertThrows(ErrorFormatoProcesos.class, () -> LectorArchivoProcesos.leerLineas(List.of("P1;0;3", "P1;1;2")));
        assertThrows(ErrorFormatoProcesos.class, () -> LectorArchivoProcesos.leerLineas(List.of("# vacío")));
    }

    @Test
    void informaSiElArchivoNoExiste(@TempDir Path dir) {
        assertThrows(ErrorFormatoProcesos.class, () -> LectorArchivoProcesos.leer(dir.resolve("no_existe.txt")));
    }

    @Test
    void leeUnArchivoReal(@TempDir Path dir) throws Exception {
        Path archivo = dir.resolve("procesos.txt");
        Files.writeString(archivo, "Word;0;8\nExcel;2;6\n");
        assertEquals(2, LectorArchivoProcesos.leer(archivo).size());
    }

    @Test
    void losArchivosDeEjemploSonValidos() throws Exception {
        for (String nombre : List.of("video_fifo.txt", "video_round_robin.txt", "ejercicio_entrada_salida.txt")) {
            assertFalse(LectorArchivoProcesos.leer(Path.of("ejemplos", nombre)).isEmpty(), nombre);
        }
    }
}
