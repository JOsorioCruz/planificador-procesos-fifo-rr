package co.edu.unicartagena.planificador.consola;

import co.edu.unicartagena.planificador.entrada.ErrorFormatoProcesos;
import co.edu.unicartagena.planificador.entrada.LectorArchivoProcesos;
import co.edu.unicartagena.planificador.metricas.CalculadoraMetricas;
import co.edu.unicartagena.planificador.metricas.ResumenMetricas;
import co.edu.unicartagena.planificador.modelo.Proceso;
import co.edu.unicartagena.planificador.modelo.Rafaga;
import co.edu.unicartagena.planificador.planificador.Planificador;
import co.edu.unicartagena.planificador.planificador.PlanificadorFIFO;
import co.edu.unicartagena.planificador.planificador.PlanificadorRoundRobin;
import co.edu.unicartagena.planificador.planificador.ResultadoSimulacion;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

/**
 * Menú de consola del simulador. Recibe la entrada y la salida como
 * parámetros para poder probarlo sin teclado.
 */
public class MenuConsola {

    private final Scanner entrada;
    private final PrintStream salida;
    private List<Proceso> procesos = new ArrayList<>();
    private String origen = "";

    public MenuConsola(InputStream entrada, PrintStream salida) {
        this.entrada = new Scanner(entrada, java.nio.charset.StandardCharsets.UTF_8);
        this.salida = salida;
    }

    public List<Proceso> getProcesos() {
        return List.copyOf(procesos);
    }

    /** Carga un archivo al iniciar, si se pasó como argumento. */
    public void cargarArchivoInicial(String ruta) {
        cargarArchivo(ruta);
    }

    public void ejecutar() {
        salida.println("==================================================");
        salida.println(" Simulador de planificación de procesos");
        salida.println(" Estrategias: FIFO y Round Robin");
        salida.println("==================================================");
        try {
            boolean seguir = true;
            while (seguir) {
                mostrarOpciones();
                String opcion = leerLinea("Opción: ");
                switch (opcion) {
                    case "1" -> ingresarPorTeclado();
                    case "2" -> cargarArchivo(leerLinea("Ruta del archivo: "));
                    case "3" -> listarProcesos();
                    case "4" -> simular(new PlanificadorFIFO());
                    case "5" -> {
                        if (hayProcesos()) {
                            leerQuantum().ifPresent(q -> simular(new PlanificadorRoundRobin(q)));
                        }
                    }
                    case "6" -> comparar();
                    case "0" -> seguir = false;
                    default -> salida.println("Opción no válida. Escriba un número del 0 al 6.");
                }
            }
        } catch (NoSuchElementException e) {
            // Se acabó la entrada: se sale sin error.
        }
        salida.println("Fin del simulador.");
    }

    private void mostrarOpciones() {
        salida.println();
        salida.println("Procesos cargados: " + procesos.size() + (origen.isEmpty() ? "" : " (" + origen + ")"));
        salida.println("1. Ingresar procesos por teclado");
        salida.println("2. Cargar procesos desde archivo");
        salida.println("3. Ver procesos cargados");
        salida.println("4. Simular con FIFO");
        salida.println("5. Simular con Round Robin");
        salida.println("6. Comparar FIFO y Round Robin");
        salida.println("0. Salir");
    }

    private void ingresarPorTeclado() {
        Optional<Integer> cantidad = leerEnteroPositivo("¿Cuántos procesos va a ingresar? ");
        if (cantidad.isEmpty()) {
            return;
        }
        salida.println("Para cada proceso escriba las ráfagas separadas por espacios, alternando");
        salida.println("CPU y E/S y empezando y terminando en CPU. Ejemplo: 3 2 2 = CPU 3, E/S 2, CPU 2.");
        List<Proceso> nuevos = new ArrayList<>();
        for (int i = 1; i <= cantidad.get(); i++) {
            salida.println("Proceso " + i + ":");
            nuevos.add(leerProceso("P" + i, nuevos));
        }
        procesos = nuevos;
        origen = "ingresados por teclado";
        salida.println("Se cargaron " + procesos.size() + " procesos.");
    }

    private Proceso leerProceso(String idSugerido, List<Proceso> yaIngresados) {
        while (true) {
            String id = leerLinea("  Identificador [" + idSugerido + "]: ");
            if (id.isEmpty()) {
                id = idSugerido;
            }
            final String elegido = id;
            if (yaIngresados.stream().anyMatch(p -> p.getId().equals(elegido))) {
                salida.println("  Ese identificador ya existe. Use otro.");
                continue;
            }
            String llegada = leerLinea("  Instante de llegada: ");
            String rafagas = leerLinea("  Ráfagas: ");
            String linea = id + ";" + llegada + ";" + String.join(";", rafagas.trim().split("\\s+"));
            try {
                return LectorArchivoProcesos.interpretarLinea(linea, 0);
            } catch (ErrorFormatoProcesos e) {
                salida.println("  Dato inválido: " + e.getMessage() + ". Ingrese de nuevo este proceso.");
            }
        }
    }

    private void cargarArchivo(String ruta) {
        try {
            procesos = new ArrayList<>(LectorArchivoProcesos.leer(Path.of(ruta.strip())));
            origen = Path.of(ruta.strip()).getFileName().toString();
            salida.println("Se cargaron " + procesos.size() + " procesos desde " + origen + ".");
            listarProcesos();
        } catch (ErrorFormatoProcesos e) {
            salida.println("No se pudo cargar el archivo. " + e.getMessage());
        } catch (IOException | RuntimeException e) {
            salida.println("No se pudo leer el archivo: " + e.getMessage());
        }
    }

    private void listarProcesos() {
        if (procesos.isEmpty()) {
            salida.println("No hay procesos cargados.");
            return;
        }
        salida.printf("%-10s %8s  %s%n", "Proceso", "Llegada", "Ráfagas");
        for (Proceso p : procesos) {
            StringBuilder rafagas = new StringBuilder();
            for (Rafaga r : p.getRafagas()) {
                rafagas.append(r).append(' ');
            }
            salida.printf("%-10s %8d  %s%n", p.getId(), p.getLlegada(), rafagas.toString().trim());
        }
    }

    private void simular(Planificador planificador) {
        if (!hayProcesos()) {
            return;
        }
        ResultadoSimulacion r = planificador.simular(procesos);
        salida.println();
        salida.println("=== " + r.estrategia() + " ===");
        salida.println("Diagrama de Gantt:");
        salida.print(ImpresoraResultados.gantt(r));
        salida.println();
        salida.print(ImpresoraResultados.tablaMetricas(CalculadoraMetricas.calcular(r)));
        String verTraza = leerLinea("¿Mostrar la traza de cambios de estado? (s/n): ");
        if (verTraza.equalsIgnoreCase("s")) {
            salida.print(ImpresoraResultados.traza(r));
        }
    }

    private void comparar() {
        if (!hayProcesos()) {
            return;
        }
        List<Integer> quantums = leerQuantums();
        if (quantums.isEmpty()) {
            return;
        }
        List<ResumenMetricas> resumenes = new ArrayList<>();
        resumenes.add(CalculadoraMetricas.calcular(new PlanificadorFIFO().simular(procesos)));
        for (int q : quantums) {
            resumenes.add(CalculadoraMetricas.calcular(new PlanificadorRoundRobin(q).simular(procesos)));
        }
        salida.println();
        salida.println("=== Comparación ===");
        salida.print(ImpresoraResultados.tablaComparativa(resumenes));
    }

    private boolean hayProcesos() {
        if (procesos.isEmpty()) {
            salida.println("Primero cargue procesos (opción 1 o 2).");
            return false;
        }
        return true;
    }

    private Optional<Integer> leerQuantum() {
        return leerEnteroPositivo("Quantum: ");
    }

    private List<Integer> leerQuantums() {
        String texto = leerLinea("Quantum o quantums separados por coma (ejemplo 2,4): ");
        List<Integer> quantums = new ArrayList<>();
        for (String parte : texto.split(",")) {
            try {
                int q = Integer.parseInt(parte.strip());
                if (q <= 0) {
                    throw new NumberFormatException();
                }
                if (!quantums.contains(q)) {
                    quantums.add(q);
                }
            } catch (NumberFormatException e) {
                salida.println("Quantum inválido: \"" + parte.strip() + "\". Debe ser un entero mayor que cero.");
                return List.of();
            }
        }
        return quantums;
    }

    private Optional<Integer> leerEnteroPositivo(String mensaje) {
        String texto = leerLinea(mensaje);
        try {
            int valor = Integer.parseInt(texto);
            if (valor > 0) {
                return Optional.of(valor);
            }
        } catch (NumberFormatException e) {
            // se informa abajo
        }
        salida.println("Valor inválido: \"" + texto + "\". Debe ser un entero mayor que cero.");
        return Optional.empty();
    }

    private String leerLinea(String mensaje) {
        salida.print(mensaje);
        salida.flush();
        String linea = entrada.nextLine().strip();
        return linea;
    }
}
