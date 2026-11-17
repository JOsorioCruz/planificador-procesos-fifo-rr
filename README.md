# Planificador de procesos FIFO y Round Robin

Simulador de consola, escrito en Java con orientación a objetos, que reproduce cómo un sistema operativo reparte el procesador entre varios procesos usando dos estrategias de planificación:

- **FIFO** (primero en llegar, primero en ser despachado): los procesos se atienden en el orden de la cola de listos y, una vez reciben el procesador, lo usan hasta que terminan o hasta que piden una operación de entrada/salida.
- **Round Robin**: mismo orden, pero cada proceso usa el procesador como máximo un tiempo fijo llamado *quantum*. Deja el procesador si agota el quantum, si pide entrada/salida o si termina.

Lo desarrollamos como Trabajo Colaborativo Contextualizado de la asignatura Sistemas Operativos (Ingeniería de Software, Universidad de Cartagena, programas a distancia, sede Cereté). Lo aplicamos a un caso del Centro Tutorial de Cereté: la saturación de la sala de cómputo en semanas de parciales y entregas, cuando muchas tareas de los estudiantes compiten por el mismo equipo.

## Qué muestra el simulador

Para cada simulación:

- **Diagrama de Gantt en texto**: qué proceso tuvo el procesador en cada intervalo. Los intervalos sin trabajo aparecen como `--`.
- **Tabla por proceso**: llegada, tiempo de CPU, tiempo de entrada/salida, inicio, salida y tres tiempos:
  - *tiempo del sistema*: salida menos llegada;
  - *tiempo de espera*: tiempo total en la cola de listos;
  - *tiempo de respuesta*: primer instante en el procesador menos llegada.
- **Promedios**, porcentaje de **utilización de la CPU** y número de **cambios de contexto** (veces que el procesador pasa de un proceso a otro).
- **Traza** opcional con cada cambio de estado (nuevo, listo, ejecutando, bloqueado, terminado) y su causa.
- **Tabla comparativa** de FIFO frente a Round Robin con uno o varios quantums sobre los mismos procesos.

## Requisitos

- Java 21 o superior.
- Maven 3.9 o superior (también sirve el que trae IntelliJ IDEA).

## Cómo compilar, probar y ejecutar

Desde la carpeta del proyecto:

```bash
mvn package
java -jar target/planificador-procesos-fifo-rr-1.0.0.jar
```

`mvn package` compila el proyecto, corre las pruebas unitarias y genera el ejecutable. Para correr solo las pruebas:

```bash
mvn test
```

Se puede cargar un archivo de procesos desde el inicio pasándolo como argumento:

```bash
java -jar target/planificador-procesos-fifo-rr-1.0.0.jar ejemplos/sala_computo_hora_pico.txt
```

En IntelliJ IDEA basta con abrir la carpeta del proyecto y ejecutar la clase `App`.

## Menú

```
1. Ingresar procesos por teclado
2. Cargar procesos desde archivo
3. Ver procesos cargados
4. Simular con FIFO
5. Simular con Round Robin
6. Comparar FIFO y Round Robin
0. Salir
```

En la opción 6 se pueden escribir varios quantums separados por coma, por ejemplo `2,4,8`, y se obtiene una columna por cada uno.

El programa valida lo que se escribe: opciones inexistentes, texto donde va un número, valores negativos o en cero, identificadores repetidos y archivos inexistentes o mal formados. En todos los casos muestra un mensaje y sigue funcionando.

## Formato del archivo de procesos

Un proceso por línea:

```
identificador;llegada;ráfaga1;ráfaga2;ráfaga3;...
```

- `llegada` es el instante en que el proceso entra al sistema.
- Las ráfagas alternan CPU y entrada/salida, empiezan y terminan en CPU. Por eso siempre hay un número impar de ráfagas.
- Se ignoran las líneas vacías y las que empiezan con `#`.

Ejemplo: `P1;0;3;2;2` es un proceso que llega en 0, usa la CPU 3 unidades, hace entrada/salida durante 2 y vuelve a usar la CPU 2 unidades.

Al ingresar procesos por teclado se piden los mismos datos; las ráfagas se escriben separadas por espacios (`3 2 2`).

## Ejemplos incluidos

| Archivo | Contenido |
|---|---|
| `ejemplos/video_fifo.txt` | Ejemplo FIFO de la clase: Word, Excel, Paint y Calculadora |
| `ejemplos/video_round_robin.txt` | Ejemplo Round Robin de la clase: cuatro procesos, para usar con quantum 2 |
| `ejemplos/ejercicio_entrada_salida.txt` | Tres procesos con peticiones de entrada/salida |
| `ejemplos/sala_computo_jornada_normal.txt` | Sala de cómputo del Centro Tutorial en una jornada normal: seis tareas espaciadas |
| `ejemplos/sala_computo_hora_pico.txt` | Sala de cómputo en hora pico de entregas: diez tareas que llegan casi juntas |

Los valores de los escenarios de la sala de cómputo son supuestos que definimos para el análisis (1 unidad = 1 segundo). No son mediciones.

## Reglas de la simulación

- El tiempo avanza en unidades enteras y hay un solo procesador.
- En un mismo instante entran a la cola de listos, en este orden: los procesos que llegan, los que terminan su entrada/salida y el proceso expulsado por agotar su quantum.
- Cada proceso hace su propia entrada/salida, en paralelo con los demás. No se modela una cola de dispositivo.
- Al terminar la entrada/salida el proceso vuelve al final de la cola de listos.
- El cambio de contexto no consume tiempo; solo se cuenta.

## Diseño

El proyecto está organizado en paquetes bajo `co.edu.unicartagena.planificador`:

| Paquete | Clases | Responsabilidad |
|---|---|---|
| `modelo` | `Proceso`, `Rafaga`, `TipoRafaga`, `EstadoProceso`, `ColaListos`, `ListaBloqueados` | Procesos, sus ráfagas, sus estados y las colas |
| `planificador` | `Planificador`, `PlanificadorFIFO`, `PlanificadorRoundRobin`, `ResultadoSimulacion`, `TramoGantt`, `EventoTraza`, `CausaEvento` | Ciclo de simulación y estrategias |
| `metricas` | `CalculadoraMetricas`, `MetricasProceso`, `ResumenMetricas` | Tiempos, promedios, utilización y cambios de contexto |
| `entrada` | `LectorArchivoProcesos`, `ErrorFormatoProcesos` | Lectura y validación de procesos |
| `consola` | `MenuConsola`, `ImpresoraResultados` | Menú e impresión de resultados |

`Planificador` es una clase abstracta que contiene el ciclo completo de la simulación. `PlanificadorFIFO` y `PlanificadorRoundRobin` heredan de ella y solo responden a una pregunta: si el proceso que está en el procesador debe dejarlo aunque no haya terminado su ráfaga. FIFO nunca lo expulsa y Round Robin lo expulsa al cumplir el quantum. El menú y las métricas trabajan con un `Planificador` sin saber cuál es, así que agregar otra estrategia solo requiere una nueva subclase.

`Proceso` solo cambia de estado por sus propios métodos, que validan cada transición. Por ejemplo, un proceso listo no puede pasar directamente a bloqueado.

## Pruebas

El proyecto tiene 51 pruebas unitarias con JUnit 5. Entre ellas están los dos ejemplos de clase (FIFO y Round Robin) y ejercicios con entrada/salida que resolvimos a mano.

Al verificar el ejemplo FIFO encontramos que los tiempos de espera de cada proceso son 0, 6, 10 y 12. El promedio es 28 / 4 = 7, y así lo calcula el simulador.

## Autores

- Abraham Isaac Kiriaki López
- Jairo Alonso Osorio Cruz
