# Desmenuzando `persistence.xml` y la cadena hasta DerbyPool

Archivo real del proyecto: `src/main/resources/META-INF/persistence.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd"
             version="3.0">

    <persistence-unit name="HelloJakartaPU" transaction-type="JTA">
        <jta-data-source>jdbc/__default</jta-data-source>
        <properties>
            <property name="eclipselink.ddl-generation" value="create-or-extend-tables"/>
            <property name="eclipselink.target-database" value="Derby"/>
            <property name="eclipselink.logging.level" value="FINE"/>
        </properties>
    </persistence-unit>

</persistence>
```

## Línea por línea

**Declaración XML y raíz** — nada específico de Jakarta, es XML estándar. `version="3.0"`
en vez de `3.1` porque el XSD de la 3.1 no está disponible en el catálogo local de esta
versión de GlassFish (ver `DOCUMENTATION.md`, sección de troubleshooting de deploy).

**`<persistence-unit name="HelloJakartaPU" transaction-type="JTA">`** — una "unidad de
persistencia" con nombre propio. Ese mismo nombre (`HelloJakartaPU`) es el que usas en
`@PersistenceContext(unitName = "HelloJakartaPU")` dentro de los EJB de `service/`.
`transaction-type="JTA"` = las transacciones las abre/cierra el contenedor EJB solo, nunca
las manejas a mano.

**`<jta-data-source>jdbc/__default</jta-data-source>`** — la línea más importante de
entender. No hay ningún dato de conexión real aquí (ni host, ni puerto, ni usuario) — solo
un **nombre lógico JNDI**. Es una indirección a propósito:

```
persistence.xml
   │ jta-data-source = "jdbc/__default"
   ▼
GlassFish · JDBC Resource "jdbc/__default"     (asadmin list-jdbc-resources)
   │ apunta al pool:
   ▼
GlassFish · JDBC Connection Pool "DerbyPool"   (asadmin list-jdbc-connection-pools)
   │ tiene las propiedades reales:
   │   serverName=localhost, portNumber=1527,
   │   databaseName=sun-appserv-samples, user=APP, password=APP
   │ (confirmado con: asadmin get "resources.jdbc-connection-pool.DerbyPool.property.*")
   ▼
Proceso real de Derby Network Server, puerto 1527
   (se prende con: ./asadmin start-database)
```

**Por qué se diseña así**: el código Java y el `persistence.xml` nunca tienen credenciales
ni hosts hardcodeados. Si este mismo `.war` se despliega en otro ambiente (staging,
producción) con otra base de datos, **no se toca ni una línea de código** — solo cambia la
configuración del pool `jdbc/__default` en ese servidor.

**Consecuencia práctica que ya nos pasó**: si el último eslabón de esa cadena (el proceso
real de Derby) no está corriendo cuando la app intenta usarlo — típicamente al arrancar,
porque el `@Singleton @Startup` `DatosIniciales` consulta la base apenas se despliega la
app — la aplicación completa puede quedar en un estado roto, y cualquier endpoint responde
`404` como si la app no existiera.

**`<properties>`** — configuración específica de **EclipseLink** (la implementación de JPA
que trae GlassFish, no parte del estándar):

| Propiedad | Qué hace |
|---|---|
| `eclipselink.ddl-generation=create-or-extend-tables` | Crea/actualiza tablas automáticamente según las clases `@Entity` |
| `eclipselink.target-database=Derby` | Le dice a EclipseLink qué dialecto SQL generar |
| `eclipselink.logging.level=FINE` | Nivel de detalle del SQL real logueado (bajar en producción) |
