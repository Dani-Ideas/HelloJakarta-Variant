# HelloJakarta — Documentación de referencia

Proyecto de práctica para familiarizarse con **Jakarta EE 10** (RESTful Web Services 3.1,
Enterprise Beans 4.0, Persistence 3.1) sobre **GlassFish 7.0.26**, antes de tocar el
proyecto real del trabajo. Nada de esto es producción.

- Java: 17
- Build tool: Maven
- Servidor: Eclipse GlassFish 7.0.26 (Jakarta EE Platform, full profile)
- Base de datos: Apache Derby embebida en GlassFish (`DerbyPool` / `jdbc/__default`)

---

## 0. Arranque después de reiniciar la PC

GlassFish **persiste en disco** qué apps tienes desplegadas (`domain1/applications/`), así
que al volver a arrancar el dominio, `HelloJakarta` reaparece solo en `list-applications`
**sin que hagas `deploy` de nuevo**. Lo único que sí hay que prender manualmente cada vez
es la base de datos (Derby no queda persistido como "encendido"):

```bash
cd /home/robute/Documentos/codes/SanboxTEST/glassfish7/glassfish/bin
./asadmin start-domain      # levanta el servidor + las apps ya desplegadas
./asadmin start-database    # levanta Derby (aparte, siempre hace falta)
```

Solo vuelves a correr `asadmin deploy --force=true ...` cuando cambies y recompiles código.

**Para mandarle una petición HTTP a la app** (es 100% backend, no tiene pantalla propia,
solo responde JSON):

- Navegador → sirve para `GET`: `http://localhost:8080/HelloJakarta/api/productos`
- `curl` → el más práctico para `POST` con body desde terminal
- Postman → cliente gráfico dedicado a probar APIs
- Panel **Endpoints** de IntelliJ Ultimate → detecta los `@Path` automáticamente
- Consola admin (`http://localhost:4848` → Applications) → para confirmar visualmente que
  está desplegada

---

## 1. Arquitectura del proyecto

```
org/example/
├── model/      Entity (JPA)         → mapea tablas: Producto, Factura, FacturaDetalle
├── dto/        DTO                  → contrato JSON hacia el cliente (React u otro)
├── mapper/     Mapper               → traduce Entity <-> DTO (clases estaticas, sin estado)
├── service/    EJB (@Stateless)     → logica de negocio + transacciones
└── rest/       JAX-RS (@Path)       → expone HTTP, solo habla en DTO
```

Flujo de una petición (`POST /api/facturas`):

```
HTTP request
   │
   ▼
JAX-RS (Jersey)  ──deserializa JSON──▶  FacturaDTO
   │
   ▼
FacturaResource.crear(dto)
   │  FacturaMapper.toEntity(dto) → Factura + FacturaDetalle
   ▼
FacturaService.crear(factura)   [EJB @Stateless]
   │  el contenedor abre la transaccion JTA automaticamente
   │  em.find(Producto.class, id) → SELECT real, precio recalculado en servidor
   │  em.persist(factura)          → INSERT en commit
   ▼
Derby (jdbc/__default → DerbyPool)
   │
   ▼
FacturaMapper.toDTO(creada) → JSON de respuesta (201 Created)
```

Regla de oro: **el Resource nunca deja salir una Entity directamente**, siempre pasa por
el Mapper. El Service nunca sabe nada de HTTP. El Resource nunca sabe nada de SQL.

---

## 2. Comandos de GlassFish (cheat sheet)

Todos se corren desde:

```bash
cd /home/robute/Documentos/codes/SanboxTEST/glassfish7/glassfish/bin
```

| Comando | Qué hace |
|---|---|
| `./asadmin start-domain` | Arranca el servidor (dominio `domain1`) |
| `./asadmin stop-domain` | Lo apaga |
| `./asadmin restart-domain` | Reinicia |
| `./asadmin start-database` | Arranca Derby en modo Network Server (puerto 1527) — **necesario aparte del dominio** |
| `./asadmin stop-database` | Apaga Derby |
| `./asadmin list-applications` | Qué WARs están desplegados |
| `./asadmin deploy --force=true <ruta.war>` | Despliega (o redepliega) un WAR |
| `./asadmin undeploy <nombre-app>` | Quita una app desplegada |
| `./asadmin list-jdbc-connection-pools` | Pools de conexión configurados |
| `./asadmin list-jdbc-resources` | Recursos JNDI (`jdbc/...`) configurados |
| `./asadmin ping-connection-pool DerbyPool` | Prueba que la conexión a la BD funciona |
| `./asadmin get "resources.jdbc-connection-pool.DerbyPool.property.*"` | Ver host/puerto/usuario/BD real del pool |

**URLs útiles:**
- App: `http://localhost:8080/HelloJakarta/api/...`
- Consola admin: `http://localhost:4848`
- Log en vivo: `tail -f ../domains/domain1/logs/server.log`

---

## 3. Sintaxis rápida de Jakarta EE

### Jakarta Persistence (JPA)

```java
@Entity
@Table(name = "PRODUCTO")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaDetalle> detalles;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;
}
```

- `@PersistenceContext(unitName = "HelloJakartaPU") EntityManager em;` → dentro de un EJB
- `em.persist(x)` / `em.find(Clase.class, id)` / `em.merge(x)` / `em.remove(x)`
- `em.createQuery("SELECT p FROM Producto p", Producto.class).getResultList();`

### Jakarta Enterprise Beans (EJB)

```java
@Stateless                 // instancia pooled, sin estado entre llamadas
public class ProductoService { ... }

@Singleton @Startup        // una sola instancia, se crea al arrancar la app
public class DatosIniciales {
    @PostConstruct
    public void cargarDatos() { ... }
}
```

- `@EJB private ProductoService productoService;` → inyección en otro EJB o en un Resource JAX-RS
- Transacciones: por default son **CMT** (Container-Managed) con `REQUIRED` — el contenedor
  abre/cierra/rollbackea la transacción solo, no hace falta código manual.

### Jakarta RESTful Web Services (JAX-RS)

```java
@ApplicationPath("/api")               // en una clase que extiende Application
public class ApplicationConfig extends Application { }

@Path("/productos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductoResource {

    @GET
    public List<ProductoDTO> listar() { ... }

    @GET @Path("/{id}")
    public Response buscar(@PathParam("id") Long id) { ... }

    @POST
    public Response crear(ProductoDTO dto) { ... }
}
```

### Jakarta Bean Validation

Se valida en el borde (los DTO que llegan por REST), no en las entidades. No requiere
dependencia nueva — ya viene dentro de `jakarta.jakartaee-api`.

```java
public class ProductoDTO {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull @Positive(message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    @PositiveOrZero
    private int stock;
}
```

- `@NotNull` / `@NotBlank` (strings) / `@NotEmpty` (colecciones/strings)
- `@Positive` / `@PositiveOrZero` / `@Min` / `@Max` / `@Size(min=, max=)`
- `@Valid` en un campo `List<OtroDTO>` → **cascada** la validación a cada elemento de la lista
- `@Valid` en el parámetro del método del `Resource` (`crear(@Valid ProductoDTO dto)`) → activa
  la validación antes de que se ejecute el método
- Si falla, Jersey lanza `ConstraintViolationException` → la captura nuestro
  `ValidationExceptionMapper` (`rest/ValidationExceptionMapper.java`) y devuelve `400` con JSON
  legible en vez de la página de error genérica de GlassFish

### Lombok

No es parte de Jakarta EE ni de Java — es una librería aparte que genera getters/setters/
constructores en tiempo de compilación para no escribirlos a mano. Ya está agregada al
`pom.xml` (`org.projectlombok:lombok`, scope `provided`).

```java
@Getter
@Setter
@NoArgsConstructor          // constructor vacio (JPA/JSON-B lo necesitan)
@AllArgsConstructor         // constructor con todos los campos, en orden de declaracion
public class ProductoDTO {
    private Long id;
    private String nombre;
    ...
}
```

- `@Getter` / `@Setter` a nivel de clase → genera los accessors de **todos** los campos
- `@Data` (no la usamos aquí) → junta `@Getter @Setter @ToString @EqualsAndHashCode` —
  **evitarla en entidades JPA**, el `equals`/`hashCode` automático da problemas con proxies
  de Hibernate/EclipseLink y con el `id` que cambia de `null` a un valor real al persistir
- **Requisito en el IDE**: sin el plugin de Lombok instalado en IntelliJ (`Settings → Plugins
  → Marketplace → "Lombok"`), el IDE va a marcar en rojo cualquier `producto.getNombre()`
  aunque Maven compile perfecto — el plugin es solo para que el editor "entienda" el código
  generado, no afecta el build real.

---

## 4. Qué revisar si algo falla (por tecnología)

### Maven falla

1. `mvn clean package` primero — descarta artefactos viejos corruptos.
2. `mvn -X package` para log verboso si el error no es claro.
3. Errores típicos que ya nos pasaron en este proyecto:
   - **Tags `<properties>` o `<plugin>` duplicados** en el `pom.xml` → revisa que no haya
     dos bloques iguales.
   - **`artifactId` mal puesto en un plugin** (le pusiste el nombre del proyecto en vez del
     nombre del plugin, ej. `maven-war-plugin`) → build tronado buscando un plugin que no existe.
   - **`mvn -o` (modo offline) falla** con "Cannot access central" → simplemente corre sin
     `-o`, necesita bajar algo que no está en caché local.
4. `mvn dependency:tree` → para ver conflictos de versiones entre dependencias.
5. Si una dependencia de Jakarta no resuelve: confirma `groupId=jakarta.*` (no `javax.*`,
   esa es la API vieja) y que la versión corresponda a la spec pedida (`10.0.0` para EE 10).

### Consultar la base de datos Derby que vive en GlassFish

Con la base arrancada (`./asadmin start-database`), usa el cliente `ij` que trae Derby:

```bash
cd /home/robute/Documentos/codes/SanboxTEST/glassfish7/javadb/bin
sh ij
```

(El script no trae el bit de ejecución activado, por eso `sh ij` en vez de `./ij` — si prefieres
`./ij` directo, antes corre `chmod +x ij`.)

Dentro de `ij`:

```sql
-- conectarse (datos reales del pool DerbyPool, confirmados con asadmin get)
connect 'jdbc:derby://localhost:1527/sun-appserv-samples;user=APP;password=APP';

show tables;
select * from PRODUCTO;
select * from FACTURA;
select * from FACTURA_DETALLE;

exit;
```

Alternativa visual (más cómoda): en IntelliJ Ultimate, **View → Tool Windows → Database →
+ → Data Source → Apache Derby (Network)**, usando los mismos datos (`localhost`, puerto
`1527`, BD `sun-appserv-samples`, usuario/clave `APP`/`APP`).

### Debugging en IntelliJ

1. **Run → Edit Configurations → + → GlassFish Server → Local**, selecciona el artifact
   `HelloJakarta:war` para desplegar.
2. Pon breakpoints haciendo clic en el margen izquierdo de la línea de código.
3. Corre con el ícono de **Debug** (el bicho 🐛), no con Run — así IntelliJ se conecta al
   puerto de depuración (JPDA) que GlassFish expone.
4. Controles de depuración (atajos default de IntelliJ, Linux/keymap estándar):
   - `F8` → Step Over
   - `F7` → Step Into
   - `Shift+F8` → Step Out
   - `F9` → Resume Program
   - `Ctrl+F8` → Poner/quitar breakpoint en la línea actual
   - `Alt+F8` → Evaluate Expression (probar una expresión en caliente)

### Herramientas y atajos generales de IntelliJ

- `Shift` `Shift` (doble shift) → Search Everywhere (buscar cualquier cosa: clase, archivo, acción)
- `Ctrl+N` → Ir a una clase
- `Ctrl+Shift+N` → Ir a un archivo
- `Shift+F10` → Run
- `Shift+F9` → Debug
- `Alt+Insert` → Generar código (getters/setters/constructores) — muy útil en las entidades
- `Ctrl+Alt+L` → Reformatear código
- `Ctrl+Alt+O` → Optimizar imports (quita los que no uses)
- `Ctrl+B` (o `Ctrl+Click`) → Ir a la declaración
- `Alt+F7` → Buscar usos de algo
- `Shift+F6` → Renombrar (refactor seguro, actualiza todas las referencias)
- Panel **Maven** (lateral derecho) → ícono de recargar (flechas circulares) para releer el
  `pom.xml` después de editarlo a mano
- Panel **Endpoints** (Ultimate) → detecta automáticamente tus `@Path` de JAX-RS y te deja
  probarlos con un cliente HTTP integrado, sin salir del IDE ni usar `curl`

---

## 5. Checklist rápido de triage

| Síntoma | Primeras cosas a revisar |
|---|---|
| GlassFish no arranca | ¿Puerto 4848/8080 ya ocupado? `lsof -i :8080`. Revisar `server.log`. |
| `deploy` falla | Leer el mensaje exacto de consola. Revisar `packaging=war` y scope `provided` en el `pom.xml`. Revisar que la versión del `persistence.xml` (`schemaLocation`/`version`) tenga su XSD disponible localmente en `glassfish7/glassfish/lib/schemas/`. |
| `ping-connection-pool` falla | ¿Corriste `start-database`? ¿Puerto 1527 libre? |
| Endpoint da `404` | ¿Coincide `@ApplicationPath` + `@Path`? ¿La app aparece en `list-applications`? ¿El context root (nombre del WAR) es el correcto en la URL? |
| Endpoint da `500` | Revisar `server.log` — normalmente `NullPointerException` por una relación JPA sin inicializar, o una transacción que falló. |
