package co.edu.unicartagena.planificador.entrada;

/**
 * Error en los datos de un proceso. Indica la línea del archivo cuando aplica.
 */
public class ErrorFormatoProcesos extends Exception {

    private final int linea;

    public ErrorFormatoProcesos(int linea, String mensaje) {
        super(linea > 0 ? "Línea " + linea + ": " + mensaje : mensaje);
        this.linea = linea;
    }

    public int getLinea() {
        return linea;
    }
}
