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
