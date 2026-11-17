package co.edu.unicartagena.planificador.modelo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Cola de procesos listos. Se atiende en orden de llegada a la cola:
 * el primero que entra es el primero que sale.
 */
public class ColaListos {

    private final Deque<Proceso> cola = new ArrayDeque<>();

    /** Agrega un proceso al final de la cola. Debe estar en estado listo. */
    public void encolar(Proceso proceso) {
        if (proceso.getEstado() != EstadoProceso.LISTO) {
            throw new IllegalStateException("Solo se encolan procesos listos: " + proceso.getId()
                    + " está " + proceso.getEstado());
        }
        if (cola.contains(proceso)) {
            throw new IllegalStateException("El proceso " + proceso.getId() + " ya está en la cola de listos");
        }
        cola.addLast(proceso);
    }

    /** Saca el proceso del frente de la cola, si hay alguno. */
    public Optional<Proceso> desencolar() {
        return Optional.ofNullable(cola.pollFirst());
    }

    public Optional<Proceso> verSiguiente() {
        return Optional.ofNullable(cola.peekFirst());
    }

    public boolean estaVacia() {
        return cola.isEmpty();
    }

    public int tamano() {
        return cola.size();
    }

    /** Copia del contenido en orden, del frente al final. */
    public List<Proceso> comoLista() {
        return List.copyOf(cola);
    }
}
