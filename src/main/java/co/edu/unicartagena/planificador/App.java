package co.edu.unicartagena.planificador;

import co.edu.unicartagena.planificador.consola.MenuConsola;

/**
 * Punto de entrada del simulador de planificación de procesos.
 * Si se pasa la ruta de un archivo como argumento, se carga al iniciar.
 */
public class App {

    public static void main(String[] args) {
        MenuConsola menu = new MenuConsola(System.in, System.out);
        if (args.length > 0) {
            menu.cargarArchivoInicial(args[0]);
        }
        menu.ejecutar();
    }
}
