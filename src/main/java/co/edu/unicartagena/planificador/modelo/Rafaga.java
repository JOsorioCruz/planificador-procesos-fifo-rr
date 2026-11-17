package co.edu.unicartagena.planificador.modelo;

import java.util.Objects;

/**
 * Tramo de trabajo de un proceso: una cantidad de unidades de tiempo
 * de CPU o de entrada/salida. Es inmutable.
 */
public final class Rafaga {

    private final TipoRafaga tipo;
    private final int duracion;

    public Rafaga(TipoRafaga tipo, int duracion) {
        this.tipo = Objects.requireNonNull(tipo, "El tipo de ráfaga es obligatorio");
        if (duracion <= 0) {
            throw new IllegalArgumentException("La duración de una ráfaga debe ser mayor que cero");
        }
        this.duracion = duracion;
    }

    public static Rafaga cpu(int duracion) {
        return new Rafaga(TipoRafaga.CPU, duracion);
    }

    public static Rafaga entradaSalida(int duracion) {
        return new Rafaga(TipoRafaga.ENTRADA_SALIDA, duracion);
    }

    public TipoRafaga getTipo() {
        return tipo;
    }

    public int getDuracion() {
        return duracion;
    }

    public boolean esCpu() {
        return tipo == TipoRafaga.CPU;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rafaga otra)) return false;
        return duracion == otra.duracion && tipo == otra.tipo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, duracion);
    }

    @Override
    public String toString() {
        return (esCpu() ? "CPU" : "E/S") + "(" + duracion + ")";
    }
}
