package co.edu.unicartagena.planificador.modelo;

import org.junit.jupiter.api.Test;

import static co.edu.unicartagena.planificador.modelo.EstadoProceso.*;
import static org.junit.jupiter.api.Assertions.*;

class EstadoProcesoTest {

    @Test
    void transicionesPermitidas() {
        assertTrue(NUEVO.puedePasarA(LISTO));
        assertTrue(LISTO.puedePasarA(EJECUTANDO));
        assertTrue(EJECUTANDO.puedePasarA(LISTO));
        assertTrue(EJECUTANDO.puedePasarA(BLOQUEADO));
        assertTrue(EJECUTANDO.puedePasarA(TERMINADO));
        assertTrue(BLOQUEADO.puedePasarA(LISTO));
    }

    @Test
    void transicionesNoPermitidas() {
        assertFalse(NUEVO.puedePasarA(EJECUTANDO));
        assertFalse(LISTO.puedePasarA(BLOQUEADO));
        assertFalse(LISTO.puedePasarA(TERMINADO));
        assertFalse(BLOQUEADO.puedePasarA(EJECUTANDO));
        for (EstadoProceso destino : EstadoProceso.values()) {
            assertFalse(TERMINADO.puedePasarA(destino));
        }
    }
}
