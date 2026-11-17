package co.edu.unicartagena.planificador.planificador;

/**
 * Intervalo [inicio, fin) en el que la CPU estuvo ocupada por un proceso
 * o estuvo ociosa.
 */
public record TramoGantt(String procesoId, int inicio, int fin) {

    /** Identificador que se usa para los intervalos sin proceso en la CPU. */
    public static final String OCIOSO = "ocioso";

    public TramoGantt {
        if (fin <= inicio) {
            throw new IllegalArgumentException("Un tramo debe durar al menos una unidad de tiempo");
        }
    }

    public boolean esOcioso() {
        return OCIOSO.equals(procesoId);
    }

    public int duracion() {
        return fin - inicio;
    }

    TramoGantt extenderHasta(int nuevoFin) {
        return new TramoGantt(procesoId, inicio, nuevoFin);
    }

    @Override
    public String toString() {
        return procesoId + " [" + inicio + ", " + fin + ")";
    }
}
