# Cómo funciona GlassFish por dentro

## 1. Qué es, en una frase

GlassFish es **un único proceso Java** (una JVM, la que ves si corres `ps aux | grep java`)
que aloja adentro de sí mismo *todos* los "motores" de las especificaciones de Jakarta EE
al mismo tiempo: el contenedor de Servlets, el motor de JAX-RS (Jersey), el contenedor de
EJB, el motor de JPA (EclipseLink), etc. No son procesos separados — son módulos dentro del
mismo proceso.

## 2. Los DOS procesos que hay que recordar

Esto ya lo vivimos en carne propia con el error 404 — son procesos **completamente
independientes**, cada uno con su propio ciclo de arranque/apagado:

```
┌─────────────────────────────────────────┐      ┌──────────────────────────────┐
│  Proceso 1: GlassFish (domain1)          │      │  Proceso 2: Derby             │
│  ./asadmin start-domain                  │      │  ./asadmin start-database     │
│                                           │      │                                │
│  puerto 8080  → tus apps (HelloJakarta)  │      │  puerto 1527 → datos reales    │
│  puerto 4848  → consola de administración│      │                                │
└─────────────────────────────────────────┘      └──────────────────────────────┘
```

Reiniciar tu PC apaga ambos. `start-domain` **no** levanta la base de datos — hay que
arrancarla aparte, siempre.

## 3. Arquitectura interna de GlassFish (dónde entra cada spec)

```
                         Peticion HTTP (puerto 8080)
                                   │
                                   ▼
                    ┌───────────────────────────┐
                    │   Conector de red (Grizzly) │
                    └───────────────────────────┘
                                   │
                 ¿la URL matchea un @Path (JAX-RS)?
                 ¿o es un Servlet/JSF tradicional?
                 ┌─────────────┴─────────────┐
                 ▼                           ▼
     ┌────────────────────┐      ┌─────────────────────┐
     │  Jersey (JAX-RS)    │      │  Servlet Container    │
     │  ProductoResource    │      │  (JSF, Servlets, etc.)│
     │  FacturaResource      │      │  no lo usamos aqui    │
     └────────────────────┘      └─────────────────────┘
                 │
                 ▼
     ┌────────────────────────┐
     │  Contenedor EJB          │
     │  ProductoService (@Stateless) │
     │  FacturaService (@Stateless)  │
     │  → abre/cierra transacciones JTA solo │
     └────────────────────────┘
                 │
                 ▼
     ┌────────────────────────┐
     │  EclipseLink (JPA)        │
     │  traduce Entity <-> SQL   │
     └────────────────────────┘
                 │
                 ▼
     ┌────────────────────────┐
     │  Pool JDBC (DerbyPool)    │
     │  jdbc/__default            │
     └────────────────────────┘
                 │
                 ▼
        Proceso Derby (puerto 1527)
```

Todo lo de arriba de la línea "Pool JDBC" vive **dentro del mismo proceso Java** de
GlassFish. Solo el último salto (Pool JDBC → Derby) cruza a otro proceso.

## 4. Estructura de directorios del dominio

Ruta base: `glassfish7/glassfish/domains/domain1/`

| Carpeta | Qué contiene |
|---|---|
| `config/` | `domain.xml` — la configuración maestra de TODO: pools JDBC, puertos, apps registradas, seguridad. Si algo está mal configurado, es aquí donde vive la fuente de verdad. |
| `applications/` | Tus WARs ya **desplegados y descomprimidos** — aquí es donde queda `HelloJakarta` después de un `deploy`. |
| `autodeploy/` | Carpeta especial: cualquier `.war` que se copie aquí se despliega solo, sin correr `asadmin deploy` (no la usamos, pero existe). |
| `docroot/` | Contenido estático de la página default que viste en `http://localhost:8080/` al principio. |
| `generated/` | Artefactos compilados en runtime (JSPs compiladas, stubs, etc.). |
| `lib/` | Librerías adicionales a nivel de dominio (no las que trae tu WAR). |
| `logs/server.log` | **El log más importante** — cualquier excepción de deploy, EJB, JPA, etc. aparece aquí primero. |
| `osgi-cache/` | Caché interno de GlassFish (OSGi es el sistema de módulos con el que está construido GlassFish por dentro). |

## 5. Dónde vive la base de datos, a nivel de directorio

Verificado directamente en el filesystem (no es un dato de memoria, se confirmó viendo el
proceso real corriendo):

```
glassfish7/glassfish/databases/sun-appserv-samples/
├── seg0/                          ← los archivos de datos reales (las tablas)
├── log/                           ← transaction log de Derby (write-ahead log)
├── service.properties             ← metadata de la base
├── db.lck / dbex.lck              ← archivos de bloqueo mientras la BD está corriendo
└── README_DO_NOT_TOUCH_FILES.txt  ← advertencia de Derby: no tocar nada a mano aqui
```

`sun-appserv-samples` es el nombre de la base configurado en el pool `DerbyPool`
(propiedad `DatabaseName`). Si alguna vez quieres "empezar de cero" con la base de datos,
sería borrar esta carpeta completa **con el proceso de Derby apagado** — pero para eso
mejor pregúntame antes de hacerlo, es una operación destructiva.

## 6. Tutorial rápido: consola de administración (`http://localhost:4848`)

Con `start-domain` corriendo, entra a esa URL. El panel izquierdo ("Common Tasks") es el
mapa de todo:

- **Applications** → lista de WARs desplegados (aquí verías `HelloJakarta`). Desde aquí
  puedes: ver detalles, **Undeploy**, **Disable/Enable** (apagar una app sin desplegarla,
  útil para depurar sin borrar nada), o subir un WAR nuevo arrastrándolo (alternativa
  gráfica a `asadmin deploy`).
- **Resources → JDBC → JDBC Connection Pools** → aquí está `DerbyPool` visualmente; puedes
  editar sus propiedades (host, puerto, usuario) sin tocar XML, y hay un botón **Ping** que
  hace lo mismo que `asadmin ping-connection-pool`.
- **Resources → JDBC → JDBC Resources** → aquí está `jdbc/__default`, mostrando a qué pool
  apunta (el primer eslabón de la cadena que vimos en `persistencia-derbypool.md`).
- **Configurations → server-config → HTTP Service** → aquí se ven/cambian los puertos
  (8080, 4848, etc.).
- **Configurations → server-config → Logger Settings** → nivel de detalle del log, y desde
  aquí también hay un visor de logs sin tener que hacer `tail` en terminal.

Regla práctica: **cualquier cosa que hiciste con `asadmin` en terminal, tiene un
equivalente visual en algún lugar de esta consola** — son dos caminos al mismo destino
(la config vive en `domain.xml` sin importar cuál uses).

## 7. Ciclo de vida resumido

```
./asadmin start-domain      → arranca el proceso grande de GlassFish (todo menos la BD)
./asadmin start-database    → arranca Derby aparte (puerto 1527)
./asadmin deploy app.war    → copia tu WAR a domain1/applications/ y lo registra en domain.xml
      │
      ▼
(reinicias tu PC — ambos procesos mueren)
      │
      ▼
./asadmin start-domain      → relee domain.xml, vuelve a cargar HelloJakarta SOLO
./asadmin start-database    → necesario de nuevo, no quedó persistido
      │
      ▼
Si el @Startup de la app corrio ANTES de que la BD estuviera lista → app puede quedar rota
      → posible arreglo: asadmin undeploy + deploy otra vez, con la BD ya arriba
```

### Caso real que nos pasó (y cómo se resolvió)

1. Reinicio de PC → al volver, `start-domain` intentó recargar `HelloJakarta` solo, pero
   Derby todavía no estaba arriba → `DatosIniciales` (`@Startup`) falló al conectar →
   **el deploy completo de la app falló**, quedó registrada como rota. Todo endpoint
   devolvía `404`.
2. Se corrió `start-database` — pero **eso solo no arregla nada**, GlassFish no reintenta
   cargar una app que ya falló antes.
3. Se intentó `asadmin deploy --force=true` de nuevo → **falló distinto**: ya no era
   "conexión rechazada", era `IllegalStateException: Attempting to execute an operation on
   a closed EntityManagerFactory`. El primer fallo dejó un `EntityManagerFactory` marcado
   como cerrado **cacheado en memoria dentro del proceso de GlassFish** (no en disco, no en
   la app) — ni `undeploy` ni un `deploy` nuevo lo limpian, porque ese caché vive a nivel
   de JVM del servidor, no del módulo desplegado.
4. **Lo que sí lo arregló**: `asadmin restart-domain` — al reiniciar todo el proceso de
   GlassFish, se limpia cualquier estado en memoria (incluyendo ese caché de
   `EntityManagerFactory`). Con el dominio recién reiniciado y Derby ya corriendo, el
   `deploy` funcionó a la primera.

**Moraleja para la próxima vez que esto pase**: si un `deploy`/`redeploy` falla con algo
sobre un `EntityManagerFactory` cerrado, no sigas intentando `undeploy`/`deploy` en loop —
ve directo a `asadmin restart-domain` y despliega después de eso.
