package co.edu.unicartagena.planificador.modelo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Procesos bloqueados esperando que termine su entrada/salida.
 * Cada proceso hace su propia E/S, así que todos avanzan a la vez.
 * Si varios terminan en la misma unidad de tiempo, salen en el orden
 * en que se bloquearon.
 */
public class ListaBloqueados {

    private final List<Proceso> bloqueados = new ArrayList<>();

    public void agregar(Proceso proceso) {
        if (proceso.getEstado() != EstadoProceso.BLOQUEADO) {
            throw new IllegalStateException("Solo se agregan procesos bloqueados: " + proceso.getId()
                    + " está " + proceso.getEstado());
        }
        if (bloqueados.contains(proceso)) {
            throw new IllegalStateException("El proceso " + proceso.getId() + " ya está bloqueado");
        }
        bloqueados.add(proceso);
    }

    /**
     * Avanza una unidad de tiempo la E/S de todos los bloqueados.
     * Los que terminan pasan a listo y se devuelven en el orden en que se bloquearon.
     */
    public List<Proceso> avanzarUnaUnidad() {
        List<Proceso> desbloqueados = new ArrayList<>();
        Iterator<Proceso> it = bloqueados.iterator();
        while (it.hasNext()) {
            Proceso proceso = it.next();
            if (proceso.avanzarEntradaSalida()) {
                proceso.finalizarEntradaSalida();
                it.remove();
                desbloqueados.add(proceso);
            }
        }
        return desbloqueados;
    }

    public boolean estaVacia() {
        return bloqueados.isEmpty();
    }

    public int tamano() {
        return bloqueados.size();
    }

    public List<Proceso> comoLista() {
        return List.copyOf(bloqueados);
    }
}
