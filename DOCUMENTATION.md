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

**Nota de esta variante**: a diferencia del proyecto original (100% backend, sin pantalla
propia), aquí el WAR también trae el frontend embebido — ver `Documentation/frontend.md`.
`http://localhost:8080/HelloJakarta-variante/` abre la app en el navegador directamente.

**Para mandarle una petición HTTP a la API** (sigue siendo JSON puro bajo `/api/...`):

- Navegador → sirve para `GET`: `http://localhost:8080/HelloJakarta-variante/api/productos`
- `curl` → el más práctico para `POST`/`PUT`/`DELETE` con body desde terminal
- Postman → cliente gráfico dedicado a probar APIs
- Panel **Endpoints** de IntelliJ Ultimate → detecta los `@Path` automáticamente
- Consola admin (`http://localhost:4848` → Applications) → para confirmar visualmente que
  está desplegada

---

## 1. Arquitectura del proyecto

Todo lo de abajo vive bajo `back/src/main/java/` (el backend es una carpeta hermana de
`frontend/` — ver `Documentation/frontend.md` sección 3 para el árbol completo del repo).

```
org/example/
├── model/   Entity (JPA)              → mapea tablas: Producto, Factura, FacturaDetalle
├── dto/     DTO                       → contrato JSON hacia el cliente (React u otro)
├── mapper/  Mapper                    → traduce Entity <-> DTO (clases estaticas, sin estado)
├── lib/     INTERFACES (contratos)    → Repository<T,ID>, Service<D,ID>, y las especificas
│              de cada entidad (ProductoRepository, ProductoService, FacturaRepository,
│              FacturaService). Nadie fuera de ejb/ conoce las clases concretas.
└── ejb/     IMPLEMENTACIONES (@Stateless/@Singleton)
               ├── AbstractRepository<T,ID>  → logica JPA compartida (crear/listar/
               │     buscarPorId/eliminar), heredada por *RepositoryImpl
               ├── ProductoRepositoryImpl, FacturaRepositoryImpl → EntityManager aqui,
               │     nada de logica de negocio (solo persistir lo que se les pase)
               ├── ProductoServiceImpl, FacturaServiceImpl → logica de negocio +
               │     conversion Entity<->DTO (via Mapper) + transacciones
               └── DatosIniciales (@Singleton @Startup) → siembra productos de ejemplo
rest/        JAX-RS (@Path)            → expone HTTP, solo habla en DTO, inyecta las
               interfaces de lib/ (nunca las clases de ejb/ directamente)
```

**Por qué interfaz + implementación separadas** (patrón Repository, con inyección
polimórfica): cualquier bean que necesite un repositorio o servicio inyecta la **interfaz**
(`@EJB private ProductoRepository productoRepository;`, tipo de `lib`) — nunca conoce
`ProductoRepositoryImpl` (la clase real, en `ejb`). GlassFish resuelve solo cuál
implementación concreta usar. Esto es lo mismo que ya viste con `CrudService`
(evitar repetir código entre entidades), pero ahora armado con interfaz + implementación
en vez de solo herencia — así es como lo hacen en proyectos reales de Jakarta EE.

Flujo de una petición (`POST /api/facturas`):

```
HTTP request
   │
   ▼
JAX-RS (Jersey)  ──deserializa JSON──▶  FacturaDTO
   │
   ▼
FacturaResource.crear(dto)              [inyecta lib.FacturaService]
   │
   ▼
FacturaServiceImpl.crear(dto)           [ejb, @Stateless — logica de negocio]
   │  FacturaMapper.toEntity(dto) → Factura + FacturaDetalle
   │  productoRepository.buscarPorId(id) → precio REAL, recalculado en servidor
   │  el contenedor abre la transaccion JTA automaticamente
   ▼
facturaRepository.crear(factura)        [ejb, @Stateless — solo datos]
   │  em.persist(factura) → INSERT en commit (cascada a FacturaDetalle)
   ▼
Derby (jdbc/__default → DerbyPool)
   │
   ▼
FacturaMapper.toDTO(creada) → JSON de respuesta (201 Created)
```

Reglas de oro: **el Resource nunca deja salir una Entity directamente**, siempre pasa por
el Mapper (ahora eso pasa dentro del Service, no del Resource). El Repository nunca sabe
nada de negocio (solo persiste). El Service nunca sabe nada de HTTP. Todo el mundo inyecta
interfaces (`lib`), nunca implementaciones concretas (`ejb`).

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
- App (frontend + API): `http://localhost:8080/HelloJakarta-variante/`
- Solo API: `http://localhost:8080/HelloJakarta-variante/api/...`
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
// lib/ProductoService.java -- el contrato, lo que TODOS inyectan
public interface ProductoService extends Service<ProductoDTO, Long> {
    ProductoDTO actualizar(Long id, ProductoDTO dto);
    boolean eliminar(Long id);
}

// ejb/ProductoServiceImpl.java -- la implementacion real, nadie fuera de ejb/ la conoce
@Stateless                 // instancia pooled, sin estado entre llamadas
public class ProductoServiceImpl implements ProductoService { ... }

@Singleton @Startup        // una sola instancia, se crea al arrancar la app
public class DatosIniciales {
    @PostConstruct
    public void cargarDatos() { ... }
}
```

- `@EJB private ProductoService productoService;` → inyección **de la interfaz**, en otro
  EJB o en un Resource JAX-RS. GlassFish resuelve solo cuál es la única implementación
  disponible (`ProductoServiceImpl`) — esto es lo que permite polimorfismo real: el que
  inyecta nunca depende de la clase concreta.
- Transacciones: por default son **CMT** (Container-Managed) con `REQUIRED` — el contenedor
  abre/cierra/rollbackea la transacción solo, no hace falta código manual. Cuando un
  `ServiceImpl` llama a un `RepositoryImpl` (otro EJB), la llamada **se une a la misma
  transacción** que ya estaba abierta — no abre una nueva.

### Patrón Repository (no es parte del estándar Jakarta EE, es una convención de diseño)

Separa "cómo se guardan/leen los datos" (Repository) de "qué hacer con esos datos"
(Service). El `Repository` nunca decide nada de negocio — solo sabe hablar con la base:

```java
// lib/Repository.java -- contrato generico compartido por cualquier entidad
public interface Repository<T, ID> {
    T crear(T entidad);
    List<T> listar();
    T buscarPorId(ID id);
    boolean eliminar(ID id);
}

// ejb/AbstractRepository.java -- implementacion compartida (EntityManager, JPQL generico)
public abstract class AbstractRepository<T, ID> implements Repository<T, ID> {
    @PersistenceContext(unitName = "HelloJakartaPU")
    protected EntityManager em;
    // ...
}

// ejb/ProductoRepositoryImpl.java -- el unico que sabe que es "Producto" especificamente
@Stateless
public class ProductoRepositoryImpl extends AbstractRepository<Producto, Long>
        implements ProductoRepository { ... }
```

El `Service` (capa de negocio) inyecta el `Repository` **por interfaz**, nunca la clase
concreta — así el Service ni se entera de si por debajo hay JPA, otra base de datos, o
incluso datos de prueba en memoria (útil, entre otras cosas, para tests).

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

**Ojo con la ubicación**: en esta variante `pom.xml` vive en `back/`, no en la raíz del
repo — todos los comandos `mvn` de aquí abajo asumen que estás parado en `back/`.

1. `mvn clean package` primero — descarta artefactos viejos corruptos.
2. `mvn -X package` para log verboso si el error no es claro.
3. Errores típicos que ya nos pasaron en este proyecto:
   - **Tags `<properties>` o `<plugin>` duplicados** en el `pom.xml` → revisa que no haya
     dos bloques iguales.
   - **`artifactId` mal puesto en un plugin** (le pusiste el nombre del proyecto en vez del
     nombre del plugin, ej. `maven-war-plugin`) → build tronado buscando un plugin que no existe.
   - **`mvn -o` (modo offline) falla** con "Cannot access central" → simplemente corre sin
     `-o`, necesita bajar algo que no está en caché local.
   - **En esta variante, esto aplica todavía más**: el `frontend-maven-plugin` descarga su
     propio Node.js y corre `npm install` como parte del `mvn package` — la primera vez
     necesita internet sí o sí, sin importar el caché de Maven. Ver `Documentation/frontend.md`.
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
   `HelloJakarta-variante:war` para desplegar (IntelliJ va a necesitar reimportar el módulo
   Maven desde `back/pom.xml` tras la reestructuración a carpetas — si no lo detecta solo,
   click derecho en `back/pom.xml` → "Add as Maven Project").
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
