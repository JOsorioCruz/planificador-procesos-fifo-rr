package co.edu.unicartagena.planificador.modelo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcesoTest {

    private Proceso procesoConEntradaSalida() {
        // CPU 2, E/S 1, CPU 1
        return new Proceso("P1", 0, List.of(Rafaga.cpu(2), Rafaga.entradaSalida(1), Rafaga.cpu(1)));
    }

    @Test
    void empiezaEnEstadoNuevoConSuPrimeraRafaga() {
        Proceso p = procesoConEntradaSalida();
        assertEquals(EstadoProceso.NUEVO, p.getEstado());
        assertEquals(Rafaga.cpu(2), p.getRafagaActual());
        assertEquals(2, p.getRestanteRafagaActual());
        assertEquals(3, p.getTotalCpu());
        assertEquals(1, p.getTotalEntradaSalida());
        assertFalse(p.haSidoEjecutado());
    }

    @Test
    void recorreElCicloCompletoConEntradaSalida() {
        Proceso p = procesoConEntradaSalida();
        p.admitir();
        p.despachar(0);
        assertFalse(p.ejecutarUnaUnidad());
        assertTrue(p.ejecutarUnaUnidad());
        assertEquals(EstadoProceso.BLOQUEADO, p.finalizarRafagaCpu(2));
        assertTrue(p.avanzarEntradaSalida());
        p.finalizarEntradaSalida();
        assertEquals(EstadoProceso.LISTO, p.getEstado());
        p.despachar(5);
        assertTrue(p.ejecutarUnaUnidad());
        assertEquals(EstadoProceso.TERMINADO, p.finalizarRafagaCpu(6));
        assertEquals(0, p.getPrimeraEjecucion());
        assertEquals(6, p.getFinalizacion());
    }

    @Test
    void laPrimeraEjecucionNoCambiaEnDespachosPosteriores() {
        Proceso p = new Proceso("P2", 1, List.of(Rafaga.cpu(3)));
        p.admitir();
        p.despachar(4);
        p.ejecutarUnaUnidad();
        p.expulsarPorQuantum();
        p.despachar(9);
        assertEquals(4, p.getPrimeraEjecucion());
    }

    @Test
    void expulsionPorQuantumDejaElRestoDeLaRafaga() {
        Proceso p = new Proceso("P3", 0, List.of(Rafaga.cpu(5)));
        p.admitir();
        p.despachar(0);
        p.ejecutarUnaUnidad();
        p.ejecutarUnaUnidad();
        p.expulsarPorQuantum();
        assertEquals(EstadoProceso.LISTO, p.getEstado());
        assertEquals(3, p.getRestanteRafagaActual());
    }

    @Test
    void rechazaTransicionesInvalidas() {
        Proceso p = procesoConEntradaSalida();
        assertThrows(IllegalStateException.class, () -> p.despachar(0));
        assertThrows(IllegalStateException.class, p::ejecutarUnaUnidad);
        p.admitir();
        assertThrows(IllegalStateException.class, p::admitir);
        assertThrows(IllegalStateException.class, p::avanzarEntradaSalida);
        p.despachar(0);
        assertThrows(IllegalStateException.class, () -> p.finalizarRafagaCpu(0));
    }

    @Test
    void noSeExpulsaPorQuantumSiLaRafagaYaTermino() {
        Proceso p = new Proceso("P4", 0, List.of(Rafaga.cpu(1)));
        p.admitir();
        p.despachar(0);
        p.ejecutarUnaUnidad();
        assertThrows(IllegalStateException.class, p::expulsarPorQuantum);
    }

    @Test
    void validaLosDatosDeEntrada() {
        assertThrows(IllegalArgumentException.class, () -> new Proceso(" ", 0, List.of(Rafaga.cpu(1))));
        assertThrows(IllegalArgumentException.class, () -> new Proceso("P", -1, List.of(Rafaga.cpu(1))));
        assertThrows(IllegalArgumentException.class, () -> new Proceso("P", 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Proceso("P", 0, List.of(Rafaga.entradaSalida(1), Rafaga.cpu(1))));
        assertThrows(IllegalArgumentException.class,
                () -> new Proceso("P", 0, List.of(Rafaga.cpu(1), Rafaga.entradaSalida(1))));
        assertThrows(IllegalArgumentException.class,
                () -> new Proceso("P", 0, List.of(Rafaga.cpu(1), Rafaga.cpu(1))));
    }

    @Test
    void consultasDeTiemposFallanAntesDeTiempo() {
        Proceso p = procesoConEntradaSalida();
        assertThrows(IllegalStateException.class, p::getPrimeraEjecucion);
        assertThrows(IllegalStateException.class, p::getFinalizacion);
    }

    @Test
    void copiaNuevaVuelveAlEstadoInicial() {
        Proceso p = procesoConEntradaSalida();
        p.admitir();
        p.despachar(0);
        p.ejecutarUnaUnidad();
        Proceso copia = p.copiaNueva();
        assertNotSame(p, copia);
        assertEquals(EstadoProceso.NUEVO, copia.getEstado());
        assertEquals(2, copia.getRestanteRafagaActual());
        assertEquals(p.getRafagas(), copia.getRafagas());
    }
}
