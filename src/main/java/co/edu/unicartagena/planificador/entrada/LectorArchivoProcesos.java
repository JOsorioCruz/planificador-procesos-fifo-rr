package co.edu.unicartagena.planificador.entrada;

import co.edu.unicartagena.planificador.modelo.Proceso;
import co.edu.unicartagena.planificador.modelo.Rafaga;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lee procesos desde texto. Formato, un proceso por línea:
 *
 *   identificador ; llegada ; ráfaga1 ; ráfaga2 ; ...
 *
 * Las ráfagas alternan CPU y E/S empezando y terminando en CPU, por ejemplo
 * "P1;0;3;2;2" es CPU 3, E/S 2, CPU 2. Se ignoran las líneas vacías y las
 * que empiezan con #.
 */
public final class LectorArchivoProcesos {

    private LectorArchivoProcesos() {
    }

    public static List<Proceso> leer(Path archivo) throws IOException, ErrorFormatoProcesos {
        if (!Files.isRegularFile(archivo)) {
            throw new ErrorFormatoProcesos(0, "No se encontró el archivo " + archivo);
        }
        return leerLineas(Files.readAllLines(archivo, StandardCharsets.UTF_8));
    }

    public static List<Proceso> leerLineas(List<String> lineas) throws ErrorFormatoProcesos {
        List<Proceso> procesos = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i).strip();
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue;
            }
            Proceso p = interpretarLinea(linea, i + 1);
            if (!ids.add(p.getId())) {
                throw new ErrorFormatoProcesos(i + 1, "el identificador " + p.getId() + " está repetido");
            }
            procesos.add(p);
        }
        if (procesos.isEmpty()) {
            throw new ErrorFormatoProcesos(0, "El archivo no tiene procesos");
        }
        return procesos;
    }

    /** Interpreta una línea con el formato del archivo. Usa 0 como número de línea si viene del teclado. */
    public static Proceso interpretarLinea(String linea, int numero) throws ErrorFormatoProcesos {
        String[] partes = linea.split(";", -1);
        if (partes.length < 3) {
            throw new ErrorFormatoProcesos(numero,
                    "se esperaba identificador;llegada;ráfagas (por ejemplo P1;0;3;2;2)");
        }
        String id = partes[0].strip();
        if (id.isEmpty()) {
            throw new ErrorFormatoProcesos(numero, "falta el identificador del proceso");
        }
        int llegada = entero(partes[1], numero, "la llegada");
        if (llegada < 0) {
            throw new ErrorFormatoProcesos(numero, "la llegada no puede ser negativa");
        }
        int[] duraciones = new int[partes.length - 2];
        for (int i = 2; i < partes.length; i++) {
            duraciones[i - 2] = entero(partes[i], numero, "la ráfaga " + (i - 1));
        }
        return crearProceso(id, llegada, duraciones, numero);
    }

    /** Crea un proceso a partir de duraciones alternadas CPU, E/S, CPU, ... */
    public static Proceso crearProceso(String id, int llegada, int[] duraciones, int numero)
            throws ErrorFormatoProcesos {
        if (duraciones.length % 2 == 0) {
            throw new ErrorFormatoProcesos(numero,
                    "el número de ráfagas debe ser impar: empieza y termina en CPU");
        }
        List<Rafaga> rafagas = new ArrayList<>();
        for (int i = 0; i < duraciones.length; i++) {
            if (duraciones[i] <= 0) {
                throw new ErrorFormatoProcesos(numero, "la ráfaga " + (i + 1) + " debe ser mayor que cero");
            }
            rafagas.add(i % 2 == 0 ? Rafaga.cpu(duraciones[i]) : Rafaga.entradaSalida(duraciones[i]));
        }
        try {
            return new Proceso(id, llegada, rafagas);
        } catch (IllegalArgumentException e) {
            throw new ErrorFormatoProcesos(numero, e.getMessage());
        }
    }

    private static int entero(String texto, int numero, String campo) throws ErrorFormatoProcesos {
        try {
            return Integer.parseInt(texto.strip());
        } catch (NumberFormatException e) {
            throw new ErrorFormatoProcesos(numero, campo + " debe ser un número entero y llegó \"" + texto.strip() + "\"");
        }
    }
}
