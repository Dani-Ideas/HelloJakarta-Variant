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
