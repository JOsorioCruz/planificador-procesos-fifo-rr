package co.edu.unicartagena.planificador.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RafagaTest {

    @Test
    void creaRafagasDeCpuYDeEntradaSalida() {
        assertTrue(Rafaga.cpu(3).esCpu());
        assertFalse(Rafaga.entradaSalida(2).esCpu());
        assertEquals(3, Rafaga.cpu(3).getDuracion());
    }

    @Test
    void rechazaDuracionCeroONegativa() {
        assertThrows(IllegalArgumentException.class, () -> Rafaga.cpu(0));
        assertThrows(IllegalArgumentException.class, () -> Rafaga.entradaSalida(-1));
    }
}
