# SQL Server real (Docker) — reemplazo de `DerbyPool`

Este documento explica cómo quedó armada la base de datos real de este proyecto: un SQL
Server de verdad, corriendo en un contenedor Docker, en vez de la Derby embebida que trae
GlassFish. El objetivo: poder conectarte tú mismo (Azure Data Studio, DBeaver, `sqlcmd`,
lo que uses) y ver/editar los datos directamente, en vez de que la única forma de "ver la
base" sea a través de los endpoints REST.

**Requisito previo, importante**: este proyecto se despliega en **GlassFish 8** (JDK 21),
no GlassFish 7 — ver `DOCUMENTATION.md` sección 1.1, e incidente #16 de
`bitacora-fixes.md` (se encontró, de pura casualidad, que el server que llevaba un rato
corriendo era GF7 con una build vieja). Todo lo de abajo asume que ya estás parado sobre
GF8.

**Sobre los puertos de GF8**: como GF7 se dejó apagado (nunca corren los dos a la vez), GF8
usa los puertos **default** de cualquier dominio nuevo — HTTP `8080`, admin `4848`. Una
versión anterior de `DOCUMENTATION.md` (sección 2) sugería puertos "corridos" (`8081`/`4849`)
para poder tener GF7 y GF8 corriendo en paralelo — eso nunca se llegó a configurar así en la
práctica y ya se corrigió en ese documento. La app real está en
`http://localhost:8080/HelloJakarta-variante/`, no en el 8081. El único puerto "nuevo" de
todo este cambio es el **1433** de SQL Server (sección 1), que no tiene nada que ver con los
de GlassFish.

---

## 1. El contenedor — `docker-compose.yml` (raíz del repo)

```yaml
services:
  sqlserver:
    image: mcr.microsoft.com/mssql/server:2022-latest
    container_name: hellojakarta-sqlserver
    environment:
      ACCEPT_EULA: "Y"
      MSSQL_SA_PASSWORD: "HelloJakarta_2026!"
      MSSQL_PID: "Developer"
    ports:
      - "1433:1433"
    volumes:
      - sqlserver-data:/var/opt/mssql
    restart: unless-stopped

volumes:
  sqlserver-data:
```

- **`MSSQL_PID: Developer`** → edición Developer, gratis para uso no productivo (todas las
  features de Enterprise, sin licencia — perfecta para un proyecto de práctica).
- **`volumes: sqlserver-data:/var/opt/mssql`** → los datos sobreviven aunque borres y
  vuelvas a crear el contenedor (`docker compose down` sin `-v`). Solo se pierden con
  `docker compose down -v` a propósito.
- **Contraseña del `sa` a la vista, en texto plano**: aceptable acá porque es un contenedor
  100% local, solo para desarrollo — nunca expuesto a la red, nunca con datos reales. Si
  esto fuera a correr en un servidor compartido o con datos reales, la contraseña iría en
  una variable de entorno fuera del repo, no hardcodeada en el `docker-compose.yml`.

### Comandos básicos

```bash
cd /home/robute/IdeaProjects/HelloJakarta-variante
docker compose up -d         # levanta el contenedor (o lo reinicia si ya existía)
docker compose logs -f sqlserver   # ver el arranque -- tarda ~10-20s la primera vez
docker compose stop          # apaga sin borrar datos
docker compose down          # apaga y borra el contenedor (el volumen de datos SOBREVIVE)
docker compose down -v       # apaga y borra TODO, incluidos los datos -- destructivo
```

### La base de datos de la app no viene sola

El contenedor arranca con las bases de sistema (`master`, `tempdb`, `model`, `msdb`) pero
**sin** ninguna base propia — a diferencia de Derby, donde GlassFish ya trae
`sun-appserv-samples` lista. Hubo que crearla a mano, una sola vez:

```bash
docker exec hellojakarta-sqlserver /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P 'HelloJakarta_2026!' -C \
  -Q "CREATE DATABASE HelloJakartaDB;"
```

(`-C` = confiar en el certificado autofirmado del contenedor; sin esto `sqlcmd` rechaza la
conexión TLS. `mssql-tools18` es el nombre de la carpeta en la imagen `2022-latest` —
versiones más viejas de la imagen la llamaban `mssql-tools` a secas.)

Las **tablas** (`PRODUCTO`, `FACTURA`, `FACTURA_DETALLE`, `SESION_CAJA`, `USUARIO`) sí las
crea EclipseLink solo, igual que con Derby — ver sección 4.

---

## 2. Cómo te conectas tú mismo a revisar la base

Con cualquier cliente que soporte SQL Server (Azure Data Studio, DBeaver, la extensión
"SQL Server (mssql)" de VS Code, `sqlcmd`, etc.), los datos de conexión son:

| Campo | Valor |
|---|---|
| Servidor / host | `localhost` |
| Puerto | `1433` |
| Usuario | `sa` |
| Contraseña | `HelloJakarta_2026!` |
| Base de datos | `HelloJakartaDB` |
| Cifrado / TLS | "Confiar en el certificado del servidor" / `trustServerCertificate=true` (el contenedor usa un certificado autofirmado) |

Con `sqlcmd` desde fuera del contenedor (si lo tienes instalado en el host) o desde dentro
(como en los ejemplos de este documento, vía `docker exec`), por ejemplo para ver los
productos actuales:

```bash
docker exec hellojakarta-sqlserver /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P 'HelloJakarta_2026!' -C -d HelloJakartaDB \
  -Q "SELECT * FROM PRODUCTO;"
```

---

## 3. El driver JDBC — por qué hubo que reiniciar GlassFish

Java no trae soporte para hablar con SQL Server incluido — hace falta el **driver JDBC de
Microsoft** (`mssql-jdbc`), un `.jar` que traduce entre JDBC (la API estándar de Java) y el
protocolo real de SQL Server (TDS). A diferencia de la app en sí (que corre dentro del WAR),
este `.jar` tiene que vivir en el **classpath del propio GlassFish**, no del WAR — porque el
JDBC Connection Pool es un recurso del *servidor*, compartido entre aplicaciones, no algo
que empaquetas dentro de tu `.war`.

```bash
curl -sL -o "$GLASSFISH8/domains/domain1/lib/mssql-jdbc-12.8.1.jre11.jar" \
  https://repo1.maven.org/maven2/com/microsoft/sqlserver/mssql-jdbc/12.8.1.jre11/mssql-jdbc-12.8.1.jre11.jar
```

**`domain1/lib/`** (no `domain1/lib/ext/` ni ningún otro) es la carpeta que GlassFish
escanea al arrancar para agregar `.jar`s sueltos al *common classloader* — el que comparten
todos los pools/recursos del dominio. La palabra clave es **"al arrancar"**: dejar el `.jar`
ahí con el dominio ya corriendo no alcanza, GlassFish no lo recarga en caliente. Por eso el
paso siguiente fue obligatorio:

```bash
export AS_JAVA=/usr/lib/jvm/java-21-openjdk
$GLASSFISH8/bin/asadmin restart-domain domain1
```

Sin este restart, `ping-connection-pool SQLServerPool` falla con `Class name is wrong or
classpath is not set for : com.microsoft.sqlserver.jdbc.SQLServerDataSource` — el error no
dice "reinicia el server", dice literalmente "no encuentro esta clase", que es la pista real
(la clase existe en el `.jar`, pero el classloader que ya estaba arriba nunca la vio).

---

## 4. El pool y el JNDI — mismo patrón que `DerbyPool`, otro motor atrás

Exactamente la misma cadena de indirección que ya documentaba
`persistencia-derbypool.md`, solo que el último eslabón cambió:

```
persistence.xml
   │ jta-data-source = "jdbc/__default"      <- esto NO cambió
   ▼
GlassFish · JDBC Resource "jdbc/__default"
   │ apunta al pool:
   ▼
GlassFish · JDBC Connection Pool "SQLServerPool"      <- esto SÍ es nuevo
   │ serverName=localhost, portNumber=1433,
   │ databaseName=HelloJakartaDB, user=sa, password=..., trustServerCertificate=true
   ▼
Contenedor Docker "hellojakarta-sqlserver", puerto 1433
```

Comandos reales usados para armar esto (con `AS_JAVA=.../java-21-openjdk` y GF8 corriendo):

```bash
# 1. Crear el pool nuevo, apuntando al contenedor
asadmin create-jdbc-connection-pool \
  --datasourceclassname com.microsoft.sqlserver.jdbc.SQLServerDataSource \
  --restype javax.sql.DataSource \
  --property "serverName=localhost:portNumber=1433:databaseName=HelloJakartaDB:user=sa:password=HelloJakarta_2026\!:encrypt=false:trustServerCertificate=true" \
  SQLServerPool

# 2. Confirmar que sí conecta de verdad (no solo que el comando no truena)
asadmin ping-connection-pool SQLServerPool

# 3. El paso que realmente "cambia de base de datos": repuntar el recurso existente
#    al pool nuevo -- persistence.xml sigue diciendo "jdbc/__default", nunca se tocó
asadmin set resources.jdbc-resource.jdbc/__default.pool-name=SQLServerPool
```

**`DerbyPool` (el original) se dejó intacto, sin borrar** — sigue existiendo, simplemente ya
no lo apunta nadie. Volver a Derby, si algún día hiciera falta, es un solo comando:
`asadmin set resources.jdbc-resource.jdbc/__default.pool-name=DerbyPool` + volver
`eclipselink.target-database` a `Derby` en `persistence.xml` + redeploy.

**`encrypt=false`**: el contenedor no trae certificado TLS propio configurado más allá del
autofirmado por defecto; para una conexión local de desarrollo, desactivar el cifrado
forzado evita líos de certificados sin ganar nada real en seguridad (el tráfico nunca sale
de `localhost`). `trustServerCertificate=true` es el mismo espíritu, del lado del driver.

### El único cambio de código real: `persistence.xml`

```xml
<property name="eclipselink.target-database" value="SQLServer"/>
```

Antes decía `Derby`. Esta es la única línea que **sí** tuvo que cambiar en el código para
todo este cambio de motor de base de datos — le dice a EclipseLink qué dialecto SQL generar
(tipos de datos, sintaxis de `CREATE TABLE`, cómo pedir el siguiente valor de una secuencia,
etc.). Todo lo demás (`jta-data-source`, las entidades, los repositorios Jakarta Data) siguió
exactamente igual — ver `persistencia-derbypool.md` para por qué el diseño está pensado así
a propósito.

**Por qué las entidades no necesitaron ningún cambio**: ninguna usa SQL nativo ni sintaxis
específica de un motor — todo pasa por JPQL generado (Jakarta Data `CrudRepository`) o por
anotaciones estándar (`@GeneratedValue(strategy = GenerationType.SEQUENCE)` en
Producto/Factura/FacturaDetalle, `IDENTITY` en SesionCaja/Usuario) — ambas estrategias
existen igual en SQL Server, EclipseLink solo necesitaba saber (vía
`eclipselink.target-database=SQLServer`) cómo traducirlas.

---

## 5. Verificado funcionando de punta a punta

Con `mvn package` (JDK 21) + `asadmin deploy --force=true` del WAR resultante sobre GF8:

- **`GET /api/productos`** → EclipseLink creó solo las 5 tablas
  (`PRODUCTO`/`FACTURA`/`FACTURA_DETALLE`/`SESION_CAJA`/`USUARIO`) en `HelloJakartaDB`
  (`create-or-extend-tables`, igual que con Derby), y `DatosIniciales` sembró los 3
  productos de siempre — confirmado tanto por el JSON de la API como por
  `SELECT * FROM PRODUCTO` directo en SQL Server.
- **`POST /api/productos`** → id nuevo generado por la secuencia `PRODUCTO_SEQ` (creada
  también sola por EclipseLink dentro de `HelloJakartaDB`), consecutivo, sin `flush()`
  manual (ver incidente #15 de `bitacora-fixes.md` — ese fix ya no dependía de qué motor de
  base de datos hubiera atrás).
- **`DELETE /api/productos/{id}`** → `204`, fila realmente borrada en SQL Server.

---

## 6. Reproducir esto desde cero (si se reinstala GlassFish, o en otra máquina)

1. `docker compose up -d` (usa el `docker-compose.yml` de la raíz del repo).
2. Crear la base una sola vez (sección 1) — `CREATE DATABASE HelloJakartaDB;` vía `sqlcmd`.
3. Copiar `mssql-jdbc-*.jar` a `.../glassfish8/glassfish/domains/domain1/lib/` (sección 3).
4. `asadmin restart-domain domain1` (con `AS_JAVA` apuntando a JDK 21).
5. Crear el pool + repuntar `jdbc/__default` (sección 4, los 3 comandos de `asadmin`).
6. Confirmar que `persistence.xml` diga `eclipselink.target-database=SQLServer` (ya
   commiteado, no hace falta tocarlo de nuevo).
7. `mvn package` (JDK 21) + `asadmin deploy --force=true` del WAR.
