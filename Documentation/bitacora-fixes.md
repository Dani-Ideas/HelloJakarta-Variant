# Bitácora de problemas y fixes

Registro de cada incidente real que se presentó armando este proyecto — el problema tal
cual apareció, cómo se diagnosticó, y qué lo resolvió. Útil para reconocer el mismo patrón
si se repite en el proyecto real del trabajo.

---

## 1. `pom.xml` mal armado al copiar la configuración a mano

**Síntoma**: Maven no compilaba / IntelliJ marcaba error en el `pom.xml`.

**Diagnóstico**: Al leer el archivo se encontraron tres problemas a la vez:
- Dos bloques `<properties>` distintos (uno con `maven.compiler.source/target`, otro con
  `maven.compiler.release`) — inválido, Maven no los fusiona.
- El plugin del WAR tenía `<artifactId>HelloJakarta</artifactId>` (el nombre del proyecto)
  en vez de `<artifactId>maven-war-plugin</artifactId>` — Maven buscaba un plugin que no
  existe.
- Faltaba `<packaging>war</packaging>` por completo → Maven empaquetaba `.jar` por default,
  y GlassFish no puede desplegar eso como app web.

**Fix**: Se reescribió el `pom.xml` con un solo bloque `<properties>`, el `artifactId`
correcto del plugin, y `<packaging>war</packaging>` agregado.

---

## 2. Deploy falla: `persistence_3_1.xsd` no se encuentra

**Síntoma**:
```
org.xml.sax.SAXException: Requested schema
https://jakarta.ee/xml/ns/persistence/persistence_3_1.xsd is not found in local repository
```

**Diagnóstico**: Se revisó el catálogo local de esquemas de GlassFish
(`glassfish7/glassfish/lib/schemas/`) y solo llegaba hasta `persistence_3_0.xsd` — la 3.1
no está incluida en esta versión. Se probó la URL remota con `curl` y devolvía `404` (el
archivo tampoco existe ya en ese hosting).

**Fix**: Se cambió `persistence.xml` de `version="3.1"` con su XSD a `version="3.0"` con
`persistence_3_0.xsd` (disponible localmente). No se perdió ninguna funcionalidad porque no
se usaba ninguna característica exclusiva de la 3.1.

---

## 3. GlassFish no aparecía en la lista de Application Servers de IntelliJ

**Síntoma**: En `Settings → Application Servers → +` solo salían WildFly, Tomcat, TomEE —
GlassFish no estaba en la lista, ni siquiera en la edición Ultimate.

**Diagnóstico**: Búsqueda confirmó que en versiones recientes de IntelliJ IDEA, el soporte
de GlassFish dejó de venir incluido en el plugin bundleado de "Application Servers" — hay
que instalarlo aparte.

**Fix**: `Settings → Plugins → Marketplace → "GlassFish" → Install` + reinicio del IDE.
Después sí apareció en la lista de servidores.

---

## 4. `ping-connection-pool DerbyPool` falla: Connection refused puerto 1527

**Síntoma**:
```
Connection could not be allocated because: java.net.ConnectException :
Error connecting to server localhost on port 1,527 with message Connection refused.
```

**Diagnóstico**: `start-domain` arranca GlassFish pero **no** arranca la base de datos
Derby — es un proceso completamente aparte que hay que levantar manualmente.

**Fix**: `./asadmin start-database`. Después el `ping-connection-pool` funcionó.

---

## 5. `./ij: Permission denied`

**Síntoma**: El cliente de consola de Derby (`javadb/bin/ij`) no se podía ejecutar
directamente.

**Diagnóstico**: El script venía sin el bit de ejecución activado en el zip descargado.

**Fix**: Se invocó con `sh ij` en vez de `./ij` (alternativa sin necesidad de `chmod`).

---

## 6. `@Getter` / `@Setter` de Lombok no reconocidos

**Síntoma**: Al escribir `@Getter`/`@Setter` en las clases, IntelliJ no las reconocía
("no me deja").

**Diagnóstico**: Esas anotaciones no son de Java ni de Jakarta EE — son de la librería
Lombok, que no estaba agregada al proyecto ni instalada como plugin del IDE.

**Fix**:
1. Se agregó la dependencia `org.projectlombok:lombok:1.18.46` (scope `provided`) al
   `pom.xml`.
2. Se instaló el plugin de Lombok en IntelliJ (`Settings → Plugins → Marketplace →
   "Lombok"`).
3. Se reemplazaron los getters/setters escritos a mano por `@Getter @Setter` en las
   entidades y los DTO.

---

## 7. App responde `404` después de reiniciar la PC (el más largo, 3 pasos)

**Síntoma inicial**: `curl http://localhost:8080/HelloJakarta/api/productos` devolvía la
página de error `404 - Not Found` de GlassFish, aunque `list-applications` mostraba
`HelloJakarta` como desplegada.

**Diagnóstico (paso 1)**: Se revisó `server.log` y se encontró la causa raíz real:
```
Initialization failed for Singleton DatosIniciales
Caused by: ... Connection refused ... port 1,527
Application deployment failed: Exception while loading the app
```
Al reiniciar la PC, `start-domain` intentó recargar automáticamente `HelloJakarta` (esto es
comportamiento normal — GlassFish persiste qué apps tenías desplegadas), pero el
`@Singleton @Startup DatosIniciales` consulta la base de datos apenas arranca la app, y
Derby todavía no estaba levantada en ese momento → el deploy completo falló y quedó en
estado roto.

**Intento 1 (insuficiente)**: Se corrió `start-database` y luego se intentó
`asadmin deploy --force=true` de nuevo. **Falló distinto**:
```
IllegalStateException: Attempting to execute an operation on a closed EntityManagerFactory
```

**Diagnóstico (paso 2)**: El primer fallo dejó un `EntityManagerFactory` marcado como
cerrado, **cacheado en memoria dentro del proceso de GlassFish** (no en disco, no en la
app) — ni `undeploy` ni un `deploy` nuevo limpian ese caché, porque vive a nivel de la JVM
del servidor completo, no del módulo desplegado.

**Fix definitivo**: `asadmin restart-domain` — reinicia todo el proceso de GlassFish,
limpiando cualquier estado en memoria. Con el dominio recién reiniciado y Derby ya
corriendo, `asadmin deploy --force=true` funcionó a la primera. Confirmado con
`curl` → `200 OK` con los 4 productos.

**Moraleja para la próxima vez**: si un deploy falla por `EntityManagerFactory` cerrado, no
insistir en loop con `undeploy`/`deploy` — ir directo a `restart-domain`.

---

## 8. Frontend embebido en el WAR: `index.html` cargaba, pero JS/CSS/favicon daban `404`

**Contexto**: al armar la variante monolítica (frontend compilado dentro del mismo `.war`
que el backend, ver `frontend.md`), se configuró Vite para escribir el build directo en
`src/main/webapp/` y se desplegó bajo el context root `/HelloJakarta-variante/`.

**Síntoma**: `curl http://localhost:8080/HelloJakarta-variante/` devolvía el `index.html`
sin problema (`200 OK`), dando la falsa impresión de que todo funcionaba. Pero al revisar
el contenido real del HTML:
```html
<script type="module" crossorigin src="/assets/index-D57PWcLP.js"></script>
<link rel="stylesheet" crossorigin href="/assets/index-DZ0PbiBn.css">
<link rel="icon" type="image/svg+xml" href="/favicon.svg" />
```
Esas rutas empiezan con `/` — es decir, absolutas **desde la raíz del dominio**
(`http://localhost:8080/assets/...`), no desde el context root del WAR
(`http://localhost:8080/HelloJakarta-variante/assets/...`). Un navegador real habría cargado
una página en blanco, sin estilos y sin JavaScript, pidiendo esos archivos en el lugar
equivocado.

**Diagnóstico**: `curl` a secas no detecta este tipo de bug porque solo pide el documento
HTML, nunca sigue las referencias a `<script>`/`<link>` como haría un navegador. Se
encontró leyendo el HTML devuelto con atención, no por un error explícito en ningún log —
**moraleja aparte**: cuando pruebes una página con `curl`, revisa el contenido, no solo el
código de estado HTTP.

Causa raíz: Vite, por default, asume que la app se sirve desde la raíz del dominio (`/`).
Como este WAR se despliega bajo un context root con nombre propio
(`/HelloJakarta-variante/`, definido por el `finalName` del `pom.xml`), todas las rutas que
Vite generó automáticamente para los assets quedaron mal.

**Fix**: agregar `base: "/HelloJakarta-variante/"` en `vite.config.ts`, coincidiendo
exactamente con el context root real del WAR. Tras recompilar (`mvn package`) y redesplegar,
el HTML generado quedó correcto:
```html
<script type="module" crossorigin src="/HelloJakarta-variante/assets/index-D57PWcLP.js"></script>
```
Se verificó no solo el código de estado del HTML, sino pidiendo directamente la URL del
`.js` referenciado (`200 OK`), confirmando que el archivo real carga en esa ruta.

**Moraleja para la próxima vez**: si el WAR se despliega bajo un context root que no sea la
raíz del dominio, `base` en `vite.config.ts` **siempre** tiene que coincidir con ese context
root (que a su vez lo define el `finalName` del `pom.xml`). Si se cambia uno, hay que
cambiar el otro.

---

## 9. Rutas de TanStack Router dan `404` al pedirlas directo (F5, o escribir la URL a mano)

**Contexto**: se agregó navegación con TanStack Router (`/`, `/productos`, `/facturas`) —
ver `tanstack.md`, Parte 3. Esas URLs solo existen en el navegador, nunca como archivos
reales dentro del WAR.

**Síntoma**: navegar haciendo clic en los links del menú funcionaba perfecto. Pero pedir
`http://localhost:8080/HelloJakarta-variante/productos` **directo** (escribiéndolo en la
barra, o dando F5 estando parado ahí) daba `404` — porque GlassFish solo tiene
`index.html`, `/assets/*` y `/api/*` como recursos reales; `/productos` no es ninguno de
esos.

**Diagnóstico**: es un problema clásico de cualquier SPA con rutas del lado del cliente
(no específico de GlassFish ni de TanStack) — el servidor no sabe que `/productos` es
"válido" porque esa validez la decide JavaScript, no el servidor.

**Fix**: agregar un `web.xml` con:
```xml
<error-page>
    <error-code>404</error-code>
    <location>/index.html</location>
</error-page>
```
Cualquier `404` cae de vuelta a `index.html`; ahí React + el router arrancan de nuevo y
leen la URL actual para mostrar la página correcta.

**Detalle importante de dónde vive ese `web.xml`**: no se puso dentro de
`src/main/webapp/`, porque esa carpeta la borra y regenera completa `npm run build`
(`emptyOutDir: true`) en cada `mvn package` — se habría perdido. Se puso en
`back/src/main/webxml/web.xml` (carpeta aparte, no tocada por Vite), y se le dijo a
`maven-war-plugin` dónde encontrarlo:
```xml
<webXml>${project.basedir}/src/main/webxml/web.xml</webXml>
```

**Error repetido durante este fix** (dos veces, en dos archivos distintos): volví a poner
`--` dentro de un comentario XML (una vez en `pom.xml`, otra vez dentro del propio
`web.xml`) — XML no permite `--` en ningún lado de un comentario, solo al cerrarlo con
`-->`. Mismo error que ya había pasado antes con el `frontend-maven-plugin` (ver
`frontend.md`). **Moraleja que claramente necesito grabarme**: nunca usar `--` como guión
largo dentro de un comentario XML — usar coma o punto y aparte en su lugar.

**Verificación** (con el WAR ya desplegado):
```bash
# Home normal
curl -s -o /dev/null -w "status:%{http_code}\n" http://localhost:8080/HelloJakarta-variante/
# → status:200

# Caso duro: pedir /productos DIRECTO, sin haber navegado ahi por los links
curl -s -o /dev/null -w "status:%{http_code}\n" http://localhost:8080/HelloJakarta-variante/productos
# → status:404 (esperado -- el error-page preserva el codigo 404 original)

# Pero el CONTENIDO que llega es el index.html real, no una pagina de error generica:
curl -s http://localhost:8080/HelloJakarta-variante/productos | grep -E "title|script"
# → <title>HelloJakarta</title>
# → <script type="module" crossorigin src="/HelloJakarta-variante/assets/index-....js">

# Y ese script SI existe y carga:
curl -s -o /dev/null -w "status:%{http_code}\n" http://localhost:8080/HelloJakarta-variante/assets/index-....js
# → status:200

# La API no se vio afectada por nada de esto:
curl -s -o /dev/null -w "status:%{http_code}\n" http://localhost:8080/HelloJakarta-variante/api/productos
# → status:200
```

**Por qué el `404` en el status no es un problema real**: el navegador renderiza el `body`
de la respuesta sin importar el código de estado (mientras el `Content-Type` sea HTML) —
para la persona usando la app, la página carga normal, el JavaScript arranca, y TanStack
Router lee la URL actual y muestra `ProductosPage` correctamente. El `404` solo importa
para herramientas que sí revisan el código de estado (crawlers de buscadores, monitoreo) —
irrelevante para este proyecto de práctica.

---

## 10. Reestructuración a Repository Pattern (`lib`/`ejb`): dos bugs reales, dos causas distintas

**Contexto**: se reestructuró todo el backend de `service/` (herencia directa,
`CrudService`) a `lib/` (interfaces) + `ejb/` (implementaciones), con una capa `Repository`
nueva entre `Service` y `EntityManager` — ver `DOCUMENTATION.md` sección 1. Al probar el
CRUD completo después del cambio, aparecieron dos bugs.

### Bug A — el `POST` ya no traía el `id` en la respuesta

**Síntoma**: `POST /api/productos` devolvía `201` con el producto creado, pero **sin el
campo `id`** (`{"nombre":"...", "precio":..., ...}`, sin `"id":N`). El registro sí se
guardaba bien en la base de datos (confirmado con un `GET` inmediatamente después, que sí
mostraba el `id` real) — el bug era solo en el objeto que se devolvía en el momento del
`POST`.

**Diagnóstico**: antes de la reestructuración, `crear()` se llamaba directo dentro de la
misma clase (`CrudService.crear()` invocado desde el propio `ProductoService`). Ahora
`ProductoServiceImpl.crear()` llama a `productoRepository.crear()` — **un bean EJB
distinto**, inyectado por interfaz. Con `GenerationType.IDENTITY`, EclipseLink necesita
ejecutar el `INSERT` real para conocer el id generado — pero al moverse la llamada a
`em.persist()` un nivel más adentro (dentro de la llamada anidada Service → Repository),
el id ya no quedaba sincronizado en el objeto a tiempo para el `return` sin forzar el
flush explícitamente.

**Fix**: agregar `em.flush()` justo después de `em.persist()` en
`AbstractRepository.crear()`. Esto fuerza el `INSERT` real de inmediato (no espera al
commit de la transacción), garantizando que el `id` generado esté poblado en el objeto
antes de que cualquiera lo use.

### Bug B — `PUT`/`DELETE` a un producto inexistente daban `403`, no `404`

**Síntoma**: `PUT`/`DELETE /api/productos/99999` (id que no existe) devolvían `403
Forbidden` en vez del `404 Not Found` que el propio código ya construye explícitamente
(`Response.status(Response.Status.NOT_FOUND).build()`).

**Diagnóstico (intento 1, incompleto)**: se sospechó del `web.xml` con el `error-page`
para `404` que se había agregado para el fallback de rutas de TanStack Router (ver
incidente 9) — la teoría: ese `error-page` es **global**, así que también intercepta los
`404` que la propia API genera a propósito, no solo los de rutas de React inexistentes.
Al redirigir un `PUT`/`DELETE` hacia `index.html` (un archivo estático), el servidor de
archivos estáticos rechaza esos verbos — de ahí el `403`.

**Fix aplicado**: reemplazar el `error-page` de `web.xml` por un **filtro**
(`SpaFallbackFilter`, `@WebFilter("/*")`) que distingue explícitamente `/api/*` (nunca lo
toca) de rutas de React sin archivo real (esas sí las reenvía a `index.html`). Se borró
`web.xml` y la config `<webXml>` del `pom.xml` por completo.

**Pero el `403` seguía pasando incluso con el filtro ya en el código.** Diagnóstico real:
```bash
unzip -l target/HelloJakarta-variante.war | grep -i web.xml
# → WEB-INF/web.xml SI aparecia, a pesar de haber borrado el archivo fuente
```
La causa real: nunca se corrió `mvn clean package`, solo `mvn package` repetidas veces.
Maven arma el WAR sobre una carpeta intermedia (`target/HelloJakarta-variante/`) y, sin un
`clean`, **no borra archivos que ya no vienen de ninguna fuente actual** — el `web.xml`
viejo seguía copiado ahí de un build anterior, sin que ningún cambio de configuración lo
quitara.

**Fix definitivo**: `mvn clean package`. Confirmado con el mismo `unzip -l` que el
`web.xml` ya no estaba en el WAR nuevo, y las pruebas de `PUT`/`DELETE` a un id inexistente
dieron `404` real.

**Moraleja para la próxima vez (dos, una por bug)**:
- Bug A: si un `id` autogenerado (`IDENTITY`) no aparece después de mover un `persist()`
  detrás de una llamada EJB anidada, sospechar de timing de sincronización y agregar
  `em.flush()` explícito.
- Bug B: **cuando cambias qué archivos entran al WAR (agregar, quitar, o mover algo fuera
  de `src/main/webapp/`), corre `mvn clean package`, no solo `mvn package`** — de otra
  forma Maven puede seguir empaquetando archivos que ya "borraste" en el código fuente,
  porque siguen viviendo en la carpeta intermedia de `target/` de un build anterior.

---



### Lo que falta para el CRUD completo (no lo construyas todavía, es para cuando llegues ahí)

- **`PUT /sesiones-caja/{id}`** (cerrar caja): un `actualizar` en `SesionCajaService`/
  `SesionCajaRepository` que reciba `montoCierre`, ponga `fCierre = LocalDateTime.now()` y
  `cerrada = true` — mismo patrón que `ProductoService.actualizar`, pero tocando solo esos
  3 campos, no todos.
- **`DELETE`**: piénsalo antes de agregarlo — ¿tiene sentido borrar una sesión de caja ya
  cerrada? Mismo criterio que ya aplicamos con `Factura` (no todo necesita los 4 verbos).

### Cómo probarlo cuando termines de escribirlo

```bash
cd back && mvn clean package
# desplegar con asadmin deploy --force=true ...

curl -X POST http://localhost:8080/HelloJakarta-variante/api/sesiones-caja \
  -H "Content-Type: application/json" \
  -d '{"montoApertura": 10.00, "cajero": "cajero1", "locacion": "tienda1"}'

curl http://localhost:8080/HelloJakarta-variante/api/sesiones-caja
```

---

## 11. Migración a Jakarta Data (`CrudRepository`) — GlassFish 7 no alcanza, hubo que subir a GlassFish 8

**Motivo del cambio:** se necesitaba usar `jakarta.data.repository.CrudRepository` de verdad
(requisito no negociable). Ese módulo **no existe en Jakarta EE 10** — es parte de
**Jakarta EE 11**, que GlassFish 7.0.26 no implementa. Se verificó antes de tocar nada:
`glassfish7/glassfish/modules/` no tiene ningún jar `jakarta.data*`, y el EclipseLink que
trae (4.0.5) es anterior a la serie 5.0 que sí implementa Jakarta Data.

### Qué se instaló (sin tocar lo que ya funcionaba)

- **GlassFish 8.0.4** instalado en paralelo a GlassFish 7, en
  `~/Documentos/codes/SanboxTEST/glassfish8/` — GlassFish 7 sigue intacto, sin desplegar
  nada nuevo ahí.
- **JDK 21** (`jdk21-openjdk`, vía pacman) — GlassFish 8 exige JDK 21+. El JDK 26 que ya
  estaba instalado en la máquina **no sirvió**: al arrancar con él, GlassFish 8 tronaba con
  errores de OSGi/Felix (`Unable to resolve ... osgi.ee=JavaSE;version=1.8`) — el
  framework de módulos interno de GlassFish 8 todavía no reconoce JDK 26 como plataforma
  válida. JDK 21 (LTS) sí arranca limpio.
- Puertos de `domain1` de GlassFish 8 **corridos +1 o +100** para poder tener los dos
  servidores levantados al mismo tiempo sin chocar: HTTP `8080→8081`, admin `4848→4849`,
  HTTPS `8181→8182`, JMX `8686→8687`, IIOP `3700/3820/3920 → 3701/3821/3921`, y el puerto
  del Derby Network Server propio de GF8 `1527→1628` (cambiado en dos lugares: la
  propiedad `PortNumber` del `DerbyPool` en `domain.xml`, **y** el flag `--dbport` al
  correr `asadmin start-database` — son dos cosas distintas, cambiar solo una no alcanza).
- Comandos remotos de `asadmin` contra GF8 necesitan `--port 4849` explícito (si no, por
  default intenta `4848`, que es el puerto de GF7).

  **Actualización (ver incidente #16):** este esquema de puertos corridos ya no está en
  pie — en algún momento posterior el `domain1` de GF8 quedó con los puertos default de
  nuevo (`8080`/`4848`/`1527`), y como en la práctica GF7 y GF8 nunca corren al mismo
  tiempo, se decidió no volver a correr los puertos: más simple dejar que GF8 use los
  default y mantener GF7 apagado. `DOCUMENTATION.md` sección 2 ya refleja esto.

### El proyecto ahora apunta a Jakarta EE 11 / GlassFish 8

- `back/pom.xml`: `jakarta.jakartaee-api` de `10.0.0` → `11.0.0`, `maven.compiler.release`
  de `17` → `21` (hay que compilar con `JAVA_HOME=.../java-21-openjdk`, si no
  `mvn` usa el JDK 17 default de la máquina y falla al pedir `--release 21`).
- `persistence.xml`: schema `persistence_3_0.xsd` → `persistence_3_2.xsd`, `version="3.2"`.

### Bug real encontrado: `insert()` no trae el `id` en la respuesta (otra vez)

Mismo síntoma que el incidente #10 (bug A) — el `POST` respondía sin `id` aunque el
`INSERT` sí se ejecutaba bien en la base (se veía el registro correcto en el `GET`
siguiente). Se probó `save()` en vez de `insert()`: mismo problema. Root cause: el
`CrudRepository` que genera el proveedor de Jakarta Data (EclipseLink 5.0.1, vía el puente
JNoSQL que usa GlassFish 8 para exponer también entidades JPA/relacionales, no solo NoSQL)
no fuerza un flush inmediato — igual que antes, la fila se inserta en la transacción, pero
el id `IDENTITY` no se sincroniza de vuelta al objeto devuelto hasta el commit.

**No hay forma de arreglarlo dentro del Repository** como la vez pasada (`AbstractRepository`
ya no existe para esa entidad — Jakarta Data no deja escribir cuerpo de método en la
interfaz). Fix real: inyectar un `EntityManager` en el `ServiceImpl` **solo** para forzar
el `flush()` justo después de `insert()`:

```java
@PersistenceContext(unitName = "HelloJakartaPU")
private EntityManager em;

// ...
Producto creado = productoRepository.insert(entidad);
em.flush();   // sin esto, creado.getId() es null
```

Funciona porque ese `EntityManager` inyectado comparte el mismo contexto de persistencia
que usa el repositorio generado — misma transacción JTA, misma unidad de persistencia
(`HelloJakartaPU`) — así que el flush de un lado sincroniza al otro.

### Otros cambios de sintaxis, ya confirmados con pruebas reales (`curl`)

- **Inyección**: `@EJB` → `@Inject` para cualquier `lib.XRepository` — ya no son
  `@Stateless` escritos a mano, son beans CDI generados por el proveedor.
- **`findAll()` devuelve `Stream<T>`, no `List<T>`** — quita el `.stream()` que antes se
  encadenaba después de `listar()`.
- **`findById(id)` devuelve `Optional<T>`**, no la entidad directa o `null` — usar
  `.orElse(null)` para conservar el mismo contrato que tenían `ProductoService`/
  `FacturaService` (null → 404 en el Resource).
- **Nombres de método**: `crear`→`insert` (o `save`), `listar`→`findAll`,
  `buscarPorId`→`findById`, `eliminar`→`deleteById` (recibe el `id`, no la entidad;
  también existe `delete(entidad)`), y `actualizar`→`update` (recibe la entidad completa
  ya modificada, no hace merge selectivo de campos).
- **El copiado selectivo de campos en `actualizar`** (ej. solo tocar `numero`/`fecha`/
  `cliente` de una `Factura`, sin tocar `detalles`/`total`) ya no puede vivir en el
  Repository (no hay dónde escribirlo) — se movió al `ServiceImpl`: buscar la entidad con
  `findById`, mutar los campos permitidos a mano, y recién ahí llamar `update(entidad)`.
- **Las excepciones de negocio (ej. conflicto de FK al borrar) se siguen envolviendo en
  `EJBException`**, exactamente igual que antes — porque quien las deja escapar sigue
  siendo un método `@Stateless` (`ProductoServiceImpl.eliminar`), sin importar que el
  `Repository` que inyecta ya no sea un EJB escrito a mano. Confirmado revisando
  `server.log` de GlassFish 8 tras forzar el conflicto real con `curl`.

### Estado actual — migración completa en el Repository

Las 4 entidades (`Producto`, `Factura`, `SesionCaja`, `Usuario`) ya usan `CrudRepository`.
`AbstractRepository.java` y el `Repository<T,ID>` genérico viejo se borraron — no los usa
nadie. Probado end-to-end contra GlassFish 8 real: `Producto`/`Factura` (GET/POST/PUT/
DELETE, 404, 409 por FK) y `SesionCaja` (GET/POST, con el `id` correcto en la respuesta
gracias al mismo fix del `flush()`). `Usuario` solo tiene el `Repository` convertido —
`Service`/`Resource`/`DTO`/`Mapper` de esa entidad siguen sin construirse, es aparte de
esta migración.

Nota aparte: al agregar la dependencia explícita `jakarta.data-api` en `pom.xml`, el
comentario que se escribió tenía un `--` suelto dentro del bloque `<!-- -->` — el mismo
error de XML documentado en el incidente #9. Recordatorio de que la regla aplica siempre,
no solo quien escribe el `pom.xml` por primera vez.

---

## 12. DTOs a `record` + Mapper con MapStruct (Producto/Factura)

**Motivo:** en el trabajo real, los DTO son `record` (no clases con Lombok) y el Mapper
usa MapStruct (interfaz + implementación generada), no una clase estática escrita a mano.
Se replicó ese patrón para `Producto`/`Factura` — `SesionCaja`/`Usuario` quedaron fuera,
son de quien los está construyendo.

**El mismo error del `--` en comentarios XML del `pom.xml` (incidente #9) volvió a pasar,
dos veces en la misma sesión** — una vez al agregar la dependencia de MapStruct, otra al
agregar el `maven-compiler-plugin`. Van tres veces documentadas ya entre incidente #9, la
nota de arriba, y esta. Si vuelve a pasar una cuarta vez, revisar con doble cuidado
cualquier comentario XML que se escriba de corrido sin pensar en la puntuación.

**Único archivo tocado fuera de alcance:** `SesionCajaMapper.java` — tenía
`.map(FacturaMapper::toDTO)` (referencia a un método `static` que ya no existe, porque
`FacturaMapper` pasó de clase a interfaz MapStruct). Se cambió a
`.map(FacturaMapper.INSTANCE::toDTO)` — una sola línea, sin tocar nada más de la lógica de
`SesionCaja`. No había forma de evitarlo: el tipo de `FacturaMapper` cambió de raíz, y
`SesionCajaMapper` lo usa.

**Detalle técnico de la integración Lombok + MapStruct**, por si se repite en otro
proyecto: declarar `annotationProcessorPaths` en `maven-compiler-plugin` para agregar
`mapstruct-processor` **apaga el escaneo automático de otros procesadores de anotaciones**
— si no se lista Lombok ahí también, deja de correr sin ningún error visible (los
`@Getter`/`@Setter` de las `Entity` simplemente dejan de generarse, y aparecen errores de
"cannot find symbol" en cascada, parecido al bug ya documentado de Lombok). Hace falta
además `lombok-mapstruct-binding` como tercer processor, para que MapStruct vea los
getters que Lombok genera en las `Entity` (sin eso, MapStruct corre su ronda antes de que
existan esos métodos).

Probado end-to-end contra GlassFish 8: `POST`/`PATCH` de `Producto`, `POST` de `Factura`
con mapeo de relación (`productoId` → `Producto`, incluyendo el precio recalculado en
servidor), `PATCH` de `Factura`, `DELETE` con conflicto de FK (409), y `GET /sesiones-caja`
para confirmar que el cambio de una línea en `SesionCajaMapper` no rompió nada.

---

## 13. GlassFish 8 reinicia y la app queda "enabled" pero rota: orden de arranque

**Síntoma:** después de reiniciar el dominio de GF8 (`start-domain`), `list-applications`
mostraba `HelloJakarta-variante <ejb, web> enabled`, pero cualquier endpoint de la API
(`GET /api/productos`) respondía `404` — ni siquiera el error 500 esperado de un problema
de base de datos, un 404 plano como si la app no existiera.

**Root cause:** GlassFish, al arrancar el dominio, intenta recargar automáticamente
cualquier app que ya tenía desplegada de una sesión anterior — eso es normal y esperado
(no hace falta re-`deploy` cada vez que se reinicia la máquina). El problema es el orden:
esa recarga ocurre **antes** de que Derby (que corre como proceso de red aparte,
`start-database`) esté disponible, si Derby no se había arrancado todavía. El log
(`server.log`) mostraba `java.net.ConnectException: Connection refused` al intentar
conectar al pool `DerbyPool`, seguido de `Application deployment failed: Exception while
loading the app`. La app queda registrada (por eso `enabled` en el listado) pero nunca
terminó de cargar de verdad — cualquier petición cae en un 404 genérico del servlet
container, no en un error de la aplicación.

**Fix:** una vez confirmado que Derby ya responde (`ping-connection-pool DerbyPool` da
`Command ... executed successfully`), volver a desplegar el mismo `.war` con
`deploy --force=true` —
no hace falta recompilar nada, el archivo no cambió, la app solo necesitaba cargar de
nuevo con la base ya disponible.

**Regla para la próxima vez que se reinicie la máquina/el dominio:** siempre
`start-database` **antes** de `start-domain`, nunca al revés. Documentado en la sección 2
de `DOCUMENTATION.md`, con el orden correcto en el cheat sheet.

---

## 14. `ProductoResource` mezclaba HTTP con manejo de excepciones de negocio — refactor a `ExceptionMapper`

**Motivo:** el `@DELETE` de `ProductoResource` tenía un `try/catch (EJBException e)` adentro
del Resource, asumiendo a ciegas que cualquier `EJBException` significaba conflicto de FK.
Dos problemas reales con eso: (1) el `Resource` (que solo debería traducir HTTP↔método)
terminaba sabiendo detalles de EJB e infraestructura, y (2) si la `EJBException` viniera de
otra causa (timeout, conexión caída), igual se le devolvía al cliente el mensaje de
"producto en uso", que sería falso.

**Fix**, siguiendo el mismo patrón que ya existía en el proyecto (`ValidationExceptionMapper`,
para los 400 de Bean Validation):

- Nueva excepción de negocio `lib/RecursoEnUsoException`, marcada con
  `@ApplicationException(rollback = true)` — sin esa anotación, el contenedor EJB envuelve
  CUALQUIER `RuntimeException` que escape de un método `@Stateless` en una `EJBException`
  genérica (así es como EJB trata "excepciones de sistema" por default). Con la anotación,
  el contenedor la deja pasar intacta.
- Nuevo `rest/RecursoEnUsoExceptionMapper` (`@Provider`), que traduce esa excepción a `409`
  con un JSON limpio — el `Resource` ya no tiene ningún `try/catch`.
- `ProductoServiceImpl.eliminar()` ya no asume que cualquier fallo es un conflicto de FK:
  recorre la cadena real de causas (`getCause()` en bucle) buscando específicamente
  `SQLIntegrityConstraintViolationException`. Si no es eso, deja pasar la excepción
  original tal cual — no inventa un mensaje de negocio que no aplica.
- `POST` de `Producto`/`Factura` ahora agrega el header `Location` con la URI del recurso
  creado (`Response.created(uri)` en vez de `Response.status(CREATED)`), y `GET listar()`
  en ambos se estandarizó a devolver `Response.ok(...)` en vez de una `List` cruda —
  consistencia con el resto de los endpoints.

**Bug real encontrado al probar el fix** (con `curl`, no en teoría): el primer intento del
`DELETE` con conflicto de FK devolvió un `500` crudo de GlassFish con el stack trace
completo expuesto en el HTML de error — exactamente el tipo de fuga de información que se
quería evitar. Causa: `productoRepository.deleteById(id)` (Jakarta Data) **no ejecuta el
`DELETE` real de inmediato** — JPA difiere la escritura hasta el flush/commit de la
transacción, que en un bean CMT ocurre **después** de que el método `eliminar()` ya había
regresado `true`. El `catch` nunca llegaba a ver la excepción porque ya había salido del
`try`. Fix: agregar `em.flush()` justo después de `deleteById(id)`, dentro del mismo
`try` — mismo patrón que ya se usa en `crear()` para sincronizar el `id` generado (ver
incidente #11), aquí para sincronizar el momento en que el error de integridad puede
atraparse. Confirmado con `curl` después del fix: `409` limpio en conflicto, `204` en
borrado real, `404` en id inexistente.

**Revisión posterior — se simplificó de nuevo**: el `try/catch` + `em.flush()` del
`DELETE` de arriba se reemplazó por algo más simple: un `EJBExceptionMapper` (`@Provider`)
que atrapa la `EJBException` en el límite HTTP, sin importar si el fallo real ocurre
dentro del método o al hacer commit después de que ya regresó. Con esto,
`ProductoServiceImpl.eliminar()` volvió a ser trivial — sin `try/catch`, sin
`EntityManager`, sin la excepción custom `RecursoEnUsoException` (se borró, ya no hace
falta). Este mapper de paso también cubre cualquier otra `EJBException` no reconocida en
cualquier Resource con un mensaje genérico, en vez de dejar escapar un `500` crudo con el
stack trace completo — eso sí era una fuga de información real.

**Lo que NO se pudo eliminar, y por qué (probado con `curl`, no asumido)**: el `em.flush()`
de `crear()` (`insert()` seguido de `flush()`) sí sigue siendo necesario. Se probó
quitándolo y mandando 4 `POST` reales — las 4 volvieron con `id: null` en la respuesta,
consistente, no intermitente. Causa raíz: `Producto`/`Factura` usan
`GenerationType.IDENTITY`, y con esa estrategia el id no se conoce hasta que el `INSERT`
real se ejecuta contra la base — EclipseLink lo difiere hasta el commit por default, y
`CrudRepository` de Jakarta Data no cambia esa regla (usa el mismo `EntityManager` con las
mismas reglas de JPA por debajo). La única forma de quitar esta necesidad de raíz sería
cambiar a `GenerationType.SEQUENCE` (reserva el id antes del `INSERT`, sin necesidad de
forzar nada) — eso es un cambio de esquema real (afecta a las 5 entidades, requiere
verificar que Derby+EclipseLink lo generen bien en una tabla que ya existe con `IDENTITY`),
no se hizo en esta sesión.

---

## 15. `EntityManager`/`flush()` eliminados de raíz: `IDENTITY` → `SEQUENCE` en Producto/Factura/FacturaDetalle

Cierre del incidente #11/#14: se implementó el cambio de estrategia que ahí se dejó
pendiente. `Producto.id`, `Factura.id`, `FacturaDetalle.id` pasaron de
`GenerationType.IDENTITY` a `GenerationType.SEQUENCE` (con `@SequenceGenerator`,
`allocationSize = 1` para que los ids sigan siendo consecutivos y fáciles de leer, en vez
del comportamiento por default de JPA que reserva bloques de 50).

**Por qué esto sí elimina la necesidad de `EntityManager`/`flush()` de raíz**: con
`SEQUENCE`, el id se reserva **antes** del `INSERT` (una llamada a `nextval` aparte) — el
proveedor lo conoce de inmediato al llamar `insert()`, sin tener que forzar que el `INSERT`
real se ejecute. Con `IDENTITY`, el id solo se conoce cuando el motor de base de datos
ejecuta el `INSERT` de verdad, y EclipseLink lo difiere hasta el commit por default — de
ahí venía la necesidad del `flush()` manual.

**Requirió un paso destructivo, autorizado explícitamente antes de ejecutarlo**: las tablas
`PRODUCTO`/`FACTURA`/`FACTURA_DETALLE` ya existían en Derby con la columna `ID` marcada
`IDENTITY` a nivel de motor — `create-or-extend-tables` no puede alterar esa propiedad en
una columna existente, solo agrega columnas faltantes. Se borraron esas 3 tablas
(`DROP TABLE`, vía `ij`) y se dejó que EclipseLink las regenerara solas al desplegar,
junto con las secuencias nuevas (`PRODUCTO_SEQ`, `FACTURA_SEQ`, `FACTURA_DETALLE_SEQ`).
Esto borró los datos de prueba que hubiera en esas 3 tablas — aceptable porque eran solo
datos sintéticos de prueba, nunca datos reales. `SESION_CAJA` y `USUARIO` NO se tocaron
(siguen con `IDENTITY`, fuera de alcance).

**Resultado, confirmado con `curl`** (4 `POST` reales a `/api/productos`, todos con el `id`
poblado correctamente y consecutivo — antes, sin el `flush()`, los mismos 4 `POST`
volvían con `id: null`): `ProductoServiceImpl` y `FacturaServiceImpl` quedaron sin ningún
`EntityManager`, sin `PersistenceContext`, sin `flush()` — solo inyectan su
`Repository` (Jakarta Data) y su `Mapper` (MapStruct), nada más. `DELETE` con conflicto de
FK (`409`, vía `EJBExceptionMapper`), `DELETE` limpio (`204`), y `POST` de `Factura` con
relación a `Producto` — todo verificado funcionando después del cambio.

---

## 16. Migración a SQL Server: el servidor que respondía en el 8080/4848 era GlassFish 7 (obsoleto), no GF8

**Contexto:** al armar la base de datos real en SQL Server (ver `Documentation/sqlserver.md`),
antes de tocar nada se verificó qué server estaba realmente sirviendo la app en ese momento
— por costumbre, no porque se sospechara nada raro.

**Síntoma/hallazgo:** `GET /api/productos` en `http://localhost:8080/...` respondía `200`
con datos reales. Todo parecía normal. Pero el proceso Java escuchando en el puerto 4848
(confirmado con `ss -ltnp`) resultó ser **GlassFish 7** (`java-17-openjdk`), no GlassFish 8
— contradice directamente la sección 1.1 de `DOCUMENTATION.md` ("el proyecto corre sobre
Jakarta EE 11 / GlassFish 8, no GlassFish 7"). Confirmado también inspeccionando el bytecode
real de una clase ya desplegada (`ProductoServiceImpl.class`, primeros bytes
`CA FE BA BE 00 00 00 3D` → `0x3D` = 61 = **class file version de Java 17**, no 65/Java 21).

**Root cause:** el WAR que GF7 tenía desplegado (y seguía sirviendo sin quejarse, porque
GlassFish recarga automáticamente lo que ya tenía desplegado al reiniciar el dominio) era
de una build **anterior** a la migración a Jakarta Data/GlassFish 8 — probablemente GF7 se
arrancó por costumbre en algún momento y nunca se volvió a apagar. Que respondiera bien con
`CrudRepository` no era prueba de nada: esa build específica simplemente nunca se
reconstruyó con el `pom.xml` actual (`maven.compiler.release=21`, que bajo JDK 17 ni
siquiera debería poder compilar — otra señal de que era una build vieja).

**Por qué esto SÍ importaba para la tarea de SQL Server:** de haber configurado el pool
nuevo sobre ese GF7 sin darse cuenta, habría quedado funcionando "por ahora" contra una
build obsoleta — y el día que alguien compilara el código actual (`mvn package` con JDK 21,
que genera bytecode versión 65) e intentara desplegarlo ahí, GF7 (corriendo con JDK 17)
habría fallado con `UnsupportedClassVersionError` al cargar las clases, un error confuso de
rastrear si no se sabía que el server activo era el equivocado.

**Fix:** `stop-domain` en GF7, `start-domain` en GF8 (`AS_JAVA=.../java-21-openjdk`),
`mvn package` con `JAVA_HOME=.../java-21-openjdk` (regenera el WAR con bytecode Java 21 de
verdad), y `deploy --force=true` de ese WAR nuevo sobre GF8. Confirmado con el mismo truco
del class file (`0x41` = 65 = Java 21 en las clases ya desplegadas) y con `curl` contra los
endpoints reales.

**Nota aparte, resuelta después:** la sección 2 de `DOCUMENTATION.md` describía GF8
corriendo en puertos corridos (`8081`/`4849`/`1628`) para convivir con GF7 en paralelo —
pero el `domain.xml` real de GF8 siempre tuvo los puertos default (`8080`/`4848`), ese
esquema nunca se llegó a configurar así de verdad. Como GF7 quedó apagado (nunca hace falta
tenerlo corriendo a la vez), se corrigió la documentación para reflejar los puertos
default, en vez de mover los puertos reales de GF8 para que coincidieran con lo que decía
el documento — ver `DOCUMENTATION.md` sección 2 y `Documentation/sqlserver.md`.

---

## 17. Migración de SQL Server (Docker) a H2 embebida — y un bug real de EclipseLink+H2 2.x

**Motivo:** se decidió quitar la dependencia de Docker por completo. H2 embebida
(`AUTO_SERVER=TRUE`) da lo mismo que se buscaba con SQL Server (poder conectarte tú mismo
con un cliente mientras GlassFish también tiene la base abierta) sin necesitar un
contenedor ni ningún proceso de servidor que arrancar/apagar a mano. Detalle completo,
paso a paso, en `Documentation/h2.md`.

**Bug real encontrado** (no un error de configuración): con la versión estable más
reciente de H2 (`2.4.240`), el deploy fallaba al crear `SESION_CAJA`/`USUARIO` —
las únicas 2 entidades que siguen usando `GenerationType.IDENTITY` (`Producto`/`Factura`/
`FacturaDetalle` ya usan `SEQUENCE` desde el incidente #15, y esas nunca tuvieron problema):

```
Syntax error in SQL statement "CREATE TABLE SESION_CAJA (ID BIGINT [*]IDENTITY NOT NULL, ...)"
expected "..., GENERATED, ..."
```

Causa: EclipseLink 5.0.1 genera `BIGINT IDENTITY NOT NULL` para columnas `IDENTITY` — sintaxis
corta que H2 aceptaba hasta la serie 1.4.x, pero que el parser reescrito de H2 2.x ya no
reconoce (2.x quiere la forma larga, `GENERATED BY DEFAULT AS IDENTITY`). EclipseLink 5.0.1
todavía no genera esa forma nueva — incompatibilidad real entre esas dos versiones
específicas.

**Fix**: usar `h2-1.4.200.jar` en vez de la `2.4.240` más reciente. Verificado con `curl`
real: `POST /sesiones-caja` (`IDENTITY`) y `POST /productos` (`SEQUENCE`) ambos funcionando
sobre la misma base, mismo deploy.

**Segundo error, más simple, encontrado antes que el de arriba**: `eclipselink.target-database`
no acepta `H2` a secas (a diferencia de `SQLServer`/`Derby`, que sí son alias reconocidos) —
hace falta el nombre completo de la clase:
`org.eclipse.persistence.platform.database.H2Platform`. Sin eso, el deploy falla con
`Database platform class [H2] not found` antes de siquiera llegar al bug de arriba.

**Nota sobre el orden de los hallazgos**: `SQLServerPool` y `DerbyPool` se dejaron
configurados en GlassFish sin borrar, mismo criterio que ya se venía siguiendo (volver a
cualquiera de los dos motores anteriores es un solo comando `asadmin set
resources.jdbc-resource.jdbc/__default.pool-name=...` + la línea correspondiente en
`persistence.xml` + redeploy).

---

## 18. Regeneración con JPA Buddy rompió `model`/`dto`/`mapper`/`ejb`/`rest` — reconstrucción completa

**Contexto:** se usó JPA Buddy (plugin de IntelliJ) para regenerar `model`, `dto` y
`mapper` de las 5 entidades desde cero, con nombres nuevos (`*Ety`, `*Dto`). El resto de
las capas (`lib`, `ejb`, `rest`) no se actualizó a la par, y la propia regeneración
introdujo varios bugs reales, no solo de nombres.

**Bloqueantes de compilación** (síntoma: ~80 errores `cannot find symbol`):
- `ejb/*ServiceImpl.java` y `rest/*Resource.java` seguían usando los nombres viejos
  (`Producto`, `ProductoDTO`, etc.) que ya no existían.
- `FacturaService.java` (interfaz) usaba `ProductoDto` en vez de `FacturaDto` en las 3
  firmas — copy/paste de `ProductoService.java` sin terminar de adaptar.
- `ProductoMapper`/`UsuarioMapper` tenían el método `toEntity(...)` declarado dos veces
  en el mismo archivo.
- Faltaban `ProductoPatchDto`/`FacturaPatchDto` — se perdieron en la regeneración.

**Bugs reales que NO tronaban al compilar** (más peligrosos — silenciosos):
- **Los 5 `@Id` quedaron como `BigDecimal`** (JPA Buddy los leyó así al hacer ingeniería
  inversa de la base), pero los 5 `Repository` seguían diciendo `CrudRepository<X, Long>`
  — Java no valida eso en compilación. Se corrigió devolviendo `Long` a las 5 entidades
  (es lo que el resto del proyecto ya esperaba).
- **`UsuarioEty` quedó con campos prefijados** (`idUsuario`, `nombreUsuario`,
  `rolUsuario`) que no coincidían con `UsuarioDto` (`id`, `nombre`, `rol`) — y como el
  `@Mapper` tenía `unmappedTargetPolicy = ReportingPolicy.IGNORE`, MapStruct no avisaba:
  generaba un mapper que compilaba perfecto y no copiaba ningún campo. Se corrigieron los
  nombres de campo de `UsuarioEty` (consistente con las otras 4 entidades) y se quitó
  `IGNORE` de los 5 mappers — mejor que un mapeo roto falle en compilación a que falle en
  silencio.
- **`Usuario.rol` dejó de ser el `enum Rol`** (se había armado a propósito para no poder
  guardar texto suelto), quedó como `String` plano. Se recreó `model/Rol.java` (se había
  borrado) y se regresó el `@Enumerated(EnumType.STRING)`.
- **`FacturaEty` perdió el lado inverso de la relación con `FacturaDetalleEty`** (el
  `@OneToMany(mappedBy = "factura")`) y `FacturaDto` no tenía ningún campo `detalles` — una
  Factura ya no podía exponer sus líneas. Se recuperó la relación en la entidad y el campo
  en el DTO (mismo arreglo en `SesionCajaEty.facturas`, que también se había perdido).
- **Bug nuevo, encontrado ya con todo compilando, solo al probar con `curl`**:
  `FacturaDetalleMapper` reusaba `ProductoMapper.toEntity()` (que ignora el `id` a
  propósito, es para crear un Producto nuevo) también para el caso de **referenciar** un
  Producto que ya existe por su id (`{"producto": {"id": 1}, "cantidad": 2}` al crear una
  Factura) — el id se perdía en el mapeo, y `FacturaServiceImpl.crear()` tronaba con
  `NullPointerException: id is required` al buscar el producto. Fix: método `default`
  aparte en `FacturaDetalleMapper` (`referenciaProducto`, con `@Named` +
  `qualifiedByName`) que solo copia el id, sin pasar por el mapeo de "creación".

**Limpieza adicional** (no rompía nada, pero se aprovechó para dejarlo consistente):
- Los 5 `Mapper` quedaron todos con el mismo patrón (`@Mapper` simple + `INSTANCE`, sin
  CDI, sin `IGNORE`) — antes 2 usaban un patrón y 3 usaban otro.
- Nombres de campo sin camelCase (`montoapertura`, `preciounitario`) corregidos a
  `montoApertura`/`precioUnitario` en `SesionCajaEty`/`FacturaDetalleEty` y sus DTOs.

**Verificado con `curl` real contra las 4 entidades, de punta a punta**: `POST`/`PATCH` de
`Producto` y `Factura` (con `detalles` anidados y referencia a `Producto` existente),
`DELETE` con conflicto de FK (`409`), `POST` de `Usuario` (con `Rol` enum) y `SesionCaja`
(ambas con `IDENTITY` + `flush()`, incidente #15).

---

## 19. `Service` dividido en `ReadService`/`WriteService`, y registro explícito de JAX-RS

Dos cambios de arquitectura, imitando el proyecto real del trabajo:

**1. `lib/Service.java` pasó a ser un marcador vacío.** Antes traía `crear`/`listar`/
`buscarPorId` directo. Ahora esos 3 se repartieron en dos interfaces nuevas:

```java
public interface ReadService<D, ID> extends Service<D, ID> {
    List<D> listar();
    D buscarPorId(ID id);
}

public interface WriteService<D, ID> extends Service<D, ID> {
    D crear(D dto);
}
```

Cada `XService` de entidad ahora extiende `ReadService<Dto, Long>, WriteService<Dto, Long>`
en vez de solo `Service<Dto, Long>`. `actualizar`/`patch`/`eliminar` siguen sin estar en
`WriteService` a propósito — no todas las entidades los necesitan (`SesionCaja` no expone
ninguno de los tres, `Factura` no expone `eliminar`) — cada `XService` los agrega directo
si le hacen falta, exactamente igual que ya se hacía antes del split. Cero cambios en
ningún `*ServiceImpl.java` — el split es puramente de interfaces, las implementaciones ya
cumplían ambos contratos de todas formas.

**2. `ApplicationConfig.java` pasó de registro implícito a explícito.** Antes:
`extends Application` sin overridear nada — Jersey registraba cualquier `@Path`/`@Provider`
que encontrara por classpath scanning. Ahora:

```java
@Override
public Set<Class<?>> getClasses() {
    return Set.of(
            ProductoResource.class, FacturaResource.class,
            SesionCajaResource.class, UsuarioResource.class,
            CorsFilter.class, ValidationExceptionMapper.class, EJBExceptionMapper.class
    );
}
```

Si una clase con `@Path`/`@Provider` no está en este `Set`, ya no se expone — aunque el
código compile perfecto. `SpaFallbackFilter` NO va en esta lista: es un `@WebFilter` de
Servlet, no un `@Provider` de JAX-RS, lo registra el contenedor de Servlet por su cuenta,
no `Application`.

**Verificado con `curl`** que los 3 `@Provider` (que antes se registraban solos) siguen
activos con el registro explícito: `CorsFilter` (header `Access-Control-Allow-Origin`
presente), `ValidationExceptionMapper` (`400` con errores de campo en un `POST` inválido),
`EJBExceptionMapper` (`409` en conflicto de FK) — y los 4 `Resource` responden normal.

---

## 20. Service dividido en 3 implementaciones reales por entidad (Read/Write/General), no solo interfaces

Ajuste sobre el incidente #19: ahí solo se dividieron las **interfaces**
(`ReadService`/`WriteService`), pero seguía habiendo **una sola implementación** por
entidad (`ProductoServiceImpl` hacía todo el trabajo directo). El patrón real que se quería
imitar tiene **3 clases `@Stateless` por entidad**:

```
ProductoReadServiceImpl   implements ProductoReadService   -- solo listar/buscarPorId
ProductoWriteServiceImpl  implements ProductoWriteService  -- solo crear/actualizar/patch/eliminar
ProductoServiceImpl       implements ProductoService       -- FACHADA: @EJB a los dos de
                                                                arriba, cada metodo solo delega
```

El `Resource` sigue inyectando un solo `@EJB ProductoService` (cero cambios en `rest/`) —
la fachada es la que decide, método por método, si le toca al `Read` o al `Write`, e
internamente inyecta ambos. Los dos (`Read` y `Write`) usan el **mismo** `XMapper.INSTANCE`
— los mappers no se dividen por lectura/escritura, solo los `Service`.

Se replicó para las 4 entidades: 8 interfaces nuevas (`XReadService`/`XWriteService` en
`lib/`), 8 implementaciones nuevas (`XReadServiceImpl`/`XWriteServiceImpl` en `ejb/`), y
los 4 `XServiceImpl.java` existentes se reescribieron como fachadas puras (sin lógica
propia, solo `@EJB` + delegar). La lógica real (cálculo de precios en `Factura`,
`em.flush()` en `SesionCaja`/`Usuario` por `IDENTITY`) se movió tal cual a cada
`XWriteServiceImpl`, sin cambios de comportamiento.

**Nota sobre `@Stateless` vs `@Singleton`**: se consideró `@Singleton` para estas 12
implementaciones nuevas, pensando que evitaría "caos" con peticiones concurrentes — es al
revés. `@Stateless` usa un *pool* de instancias intercambiables (el mecanismo real que evita
colisiones entre peticiones paralelas). `@Singleton` es una sola instancia compartida por
toda la app, y sus métodos son de bloqueo de escritura exclusivo por default
(`@Lock(WRITE)` implícito) — hubiera serializado *todas* las peticiones concurrentes a un
mismo Service, incluidos los `GET` de puro lectura. Se quedó `@Stateless` en las 12 clases
nuevas. La única `@Singleton` real del proyecto sigue siendo `DatosIniciales`, que sí
necesita correr una sola vez al arrancar.

**Verificado con `curl`** contra las 4 entidades: `GET`/`POST`/`PATCH` de `Producto`,
`GET`/`POST` de `Factura` (con `detalles` anidados), `POST` de `SesionCaja` y `Usuario`
(ambos `IDENTITY` + `flush()`, funcionando desde el `WriteServiceImpl` correspondiente).

---

## 21. El cambio de forma de `FacturaDetalleDto` (incidente #18) rompió el frontend

**Síntoma:** la reconstrucción del incidente #18 cambió `FacturaDetalleDto` de campos
sueltos (`productoId: Long, nombreProducto: String`) a un objeto anidado
(`producto: ProductoDto`) — cambio de diseño real, no un error, pero el frontend nunca se
actualizó para reflejarlo.

**Dónde se rompía:** `frontend/src/components/FacturasTable.tsx` (línea 110) leía
`detalle.nombreProducto` — un campo que ya no existe en el JSON real. No tronaba (TypeScript
no valida nada en runtime, el `interface` de `types.ts` solo es una anotación de
compilación) — la columna "Producto" de la tabla expandida simplemente se veía **vacía**,
sin ningún error visible en consola.

**Fix**, 2 archivos:

```ts
// api/types.ts
export interface FacturaDetalleDTO {
  id: number;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  producto: ProductoDTO;   // objeto anidado, ya no productoId/nombreProducto sueltos
}
```
```tsx
// components/FacturasTable.tsx
<td>{detalle.producto.nombre}</td>   {/* antes: detalle.nombreProducto */}
```

**Verificado** con `curl` a `/api/facturas/1` que el JSON real (`detalles[0].producto.nombre`)
coincide exactamente con lo que el componente ahora lee, y con `npx tsc --noEmit` que el
frontend compila sin errores de tipo. No se pudo confirmar visualmente en navegador en esta
sesión (sin herramienta de browser disponible) — pendiente que el usuario lo confirme
abriendo la app y expandiendo una fila de la tabla de Facturas.

**Lección general**: un cambio en la forma de un DTO del backend (agregar/quitar/anidar un
campo) **no rompe la compilación de Java** — pero sí puede romper el frontend en silencio,
porque TypeScript solo valida tipos en tiempo de compilación, no contra el JSON real que
llega en runtime. Cada vez que cambie la forma de un DTO, hay que revisar a mano si algún
componente de React lo consume directo.

---

## 22. `Resource` → `Controller`, y `ApplicationConfig` → `ControllerRegistry`

Renombrado de vocabulario, sin cambio de comportamiento — imitando la terminología del
proyecto real:

- `ProductoResource` → `ProductoController`, y lo mismo con `Factura`/`SesionCaja`/`Usuario`.
- `ApplicationConfig` → `ControllerRegistry` — mismo archivo, mismo `@ApplicationPath("/api")`,
  mismo `getClasses()` explícito del incidente #19, solo que ahora el nombre de la clase
  refleja lo que hace: es el "controller de controllers", el único que conoce a todos los
  `Controller` (la relación va en un solo sentido — ningún `Controller` conoce al registro).

Cero cambios de lógica, solo nombres de clase/archivo + las referencias dentro del `Set` de
`ControllerRegistry`. Verificado con `curl` que los 4 endpoints (`/productos`, `/facturas`,
`/sesiones-caja`, `/usuarios`) siguen respondiendo `200` después del rename.
