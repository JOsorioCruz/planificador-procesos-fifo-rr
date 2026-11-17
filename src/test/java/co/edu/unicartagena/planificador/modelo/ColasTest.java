package co.edu.unicartagena.planificador.modelo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ColasTest {

    private Proceso listo(String id) {
        Proceso p = new Proceso(id, 0, List.of(Rafaga.cpu(1), Rafaga.entradaSalida(2), Rafaga.cpu(1)));
        p.admitir();
        return p;
    }

    private Proceso bloqueado(String id, int duracionEs) {
        Proceso p = new Proceso(id, 0, List.of(Rafaga.cpu(1), Rafaga.entradaSalida(duracionEs), Rafaga.cpu(1)));
        p.admitir();
        p.despachar(0);
        p.ejecutarUnaUnidad();
        p.finalizarRafagaCpu(1);
        return p;
    }

    @Test
    void colaDeListosRespetaElOrdenDeEntrada() {
        ColaListos cola = new ColaListos();
        Proceso a = listo("A");
        Proceso b = listo("B");
        cola.encolar(a);
        cola.encolar(b);
        assertEquals(2, cola.tamano());
        assertSame(a, cola.verSiguiente().orElseThrow());
        assertSame(a, cola.desencolar().orElseThrow());
        assertSame(b, cola.desencolar().orElseThrow());
        assertTrue(cola.desencolar().isEmpty());
        assertTrue(cola.estaVacia());
    }

    @Test
    void colaDeListosRechazaProcesosNoListosYDuplicados() {
        ColaListos cola = new ColaListos();
        Proceso nuevo = new Proceso("N", 0, List.of(Rafaga.cpu(1)));
        assertThrows(IllegalStateException.class, () -> cola.encolar(nuevo));
        Proceso a = listo("A");
        cola.encolar(a);
        assertThrows(IllegalStateException.class, () -> cola.encolar(a));
    }

    @Test
    void bloqueadosAvanzanALaVezYSalenEnOrdenDeBloqueo() {
        ListaBloqueados lista = new ListaBloqueados();
        Proceso largo = bloqueado("Largo", 2);
        Proceso corto = bloqueado("Corto", 1);
        Proceso otro = bloqueado("Otro", 2);
        lista.agregar(largo);
        lista.agregar(corto);
        lista.agregar(otro);

        assertEquals(List.of(corto), lista.avanzarUnaUnidad());
        assertEquals(EstadoProceso.LISTO, corto.getEstado());

        assertEquals(List.of(largo, otro), lista.avanzarUnaUnidad());
        assertTrue(lista.estaVacia());
    }

    @Test
    void listaDeBloqueadosRechazaProcesosNoBloqueados() {
        ListaBloqueados lista = new ListaBloqueados();
        assertThrows(IllegalStateException.class, () -> lista.agregar(listo("A")));
    }
}
