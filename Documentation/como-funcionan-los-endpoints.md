# Cómo funcionan los endpoints — de verdad, sin saltarse nada

Este documento existe porque la primera explicación del flujo (`GET /api/productos`) asumía
demasiado conocimiento previo de Java/Jakarta. Aquí está reconstruido desde cero, sin dar
nada por sentado, incluyendo las partes internas (generics, herencia, inyección) que se
quedaron sin explicar la primera vez.

---

## 1. Qué es GlassFish, realmente

**No existe "un SDK especial de Java EE"** que instales en vez del JDK normal — eso no es
así desde hace años. Lo que hay:

- Tu **JDK 17** — compila y ejecuta código Java. No sabe nada de "endpoints" ni "REST" por
  sí solo.
- **GlassFish** — **no es un tipo especial de Java, es un programa Java más**, que tú
  ejecutas con tu mismo JDK normal. Prueba de esto, literal, de la salida de
  `./asadmin start-domain`:
  ```
  Executing: nohup /usr/lib/jvm/java-17-openjdk/bin/java -cp .../glassfish.jar ...
  ```
  Es `java -cp glassfish.jar ...` — el mismo comando que usarías para correr cualquier
  programa Java tuyo. GlassFish es, por dentro, un `.jar` gigante lleno de clases Java
  compiladas, igual que las tuyas.

Lo que hace "especial" a GlassFish no es magia del lenguaje — es que su trabajo específico
es leer TUS clases compiladas (tu `.war`) y organizarlas para funcionar como servidor web,
usando una capacidad **normal** de Java: **reflection**.

## 2. Qué es reflection, y cómo arma la URL a partir de las anotaciones

**Reflection** es una capacidad estándar del lenguaje Java (de cualquier JDK, nada de "EE"):
un programa puede, mientras se ejecuta, **abrir y examinar otra clase** — ver qué métodos
tiene, qué anotaciones le pusieron, qué campos tiene — sin código explícito para eso.
GlassFish usa reflection para leer tus anotaciones (`@Path`, `@GET`, etc.) al momento del
`deploy`, y arma una tabla interna ("libreta de direcciones") que dice "si llega una
petición para tal URL, hay que llamar tal método de tal clase".

La URL `/api/productos` se arma sumando 3 piezas:

```java
// ApplicationConfig.java
@ApplicationPath("/api")     // pieza 1: "todo lo mio empieza con /api"
public class ApplicationConfig extends Application { }
```

```java
// ProductoResource.java
@Path("/productos")           // pieza 2: "esta clase atiende /productos"
public class ProductoResource {

    @GET                       // pieza 3: "si es GET sin nada mas, este metodo"
    public List<ProductoDTO> listar() { ... }
}
```

`/api` + `/productos` + (nada extra) = `/api/productos`, solo para `GET`. Esta "libreta" se
arma **una sola vez, en el momento del deploy**, y queda en memoria mientras GlassFish sigue
corriendo.

## 3. Cómo llega la petición de verdad (pura red, nada de Jakarta todavía)

1. Al arrancar (`start-domain`), GlassFish abre un **socket** — un "oído" abierto en el
   puerto 8080, esperando datos. Esto es networking básico, cualquier programa lo puede
   hacer.
2. `curl http://localhost:8080/HelloJakarta-variante/api/productos` manda un mensaje HTTP
   por la red a `localhost:8080`.
3. El sistema operativo entrega esos bytes al proceso escuchando ahí — GlassFish.
4. GlassFish lee el mensaje: método `GET`, ruta `/HelloJakarta-variante/api/productos`.
5. Reconoce `HelloJakarta-variante` como el nombre de la app desplegada (el context root,
   definido por el `finalName` del WAR).
6. Busca en su "libreta" (la del punto 2) y encuentra: `ProductoResource.listar()`.
7. Ejecuta ese método Java **normal** — no hay nada especial en cómo se ejecuta, es una
   llamada de método común.

## 4. Cómo verificar cada pieza tú mismo (no confíes, compruébalo)

```bash
# 1. GlassFish esta vivo como proceso?
ps aux | grep glassfish.jar

# 2. Tu app esta desplegada?
cd /home/robute/Documentos/codes/SanboxTEST/glassfish7/glassfish/bin
./asadmin list-applications

# 3. El socket del puerto 8080 esta escuchando?
curl -I http://localhost:8080/

# 4. Tu codigo especifico responde?
curl http://localhost:8080/HelloJakarta-variante/api/productos
```

- (1) vacío → GlassFish ni corre, nada más puede funcionar.
- (2) sin tu app → GlassFish corre pero el WAR no está desplegado ahí.
- (3) falla → el puerto ni responde.
- (4) da el JSON → confirmado punta a punta que tu código Java se ejecutó con datos reales.

**Para confirmar que es TU código actualizado** (no una versión vieja): cambia algo visible
en el código Java, vuelve a desplegar, y si el cambio aparece en el `curl`, es prueba
directa — no hay forma de que responda algo que no esté en el `.class` compilado actual.

---

## 5. El flujo completo de `GET /api/productos`, fase por fase

### FASE 1 — Arranca en el navegador (React)

`components/ProductosPanel.tsx`:
```tsx
const { data, isLoading, isError, error } = useQuery({
  queryKey: ["productos"],
  queryFn: fetchProductos,
});
```
`useQuery` ve que no hay nada cacheado para `["productos"]` y llama automáticamente a
`fetchProductos` — nunca escribes el `fetch()` a mano en el componente.

### FASE 2 — Se arma la petición HTTP (`api/client.ts`)

```ts
export function fetchProductos(): Promise<ProductoDTO[]> {
  return request<ProductoDTO[]>("/productos");
}
```
Hace `fetch("/HelloJakarta-variante/api/productos")` — ruta **relativa**, el navegador la
resuelve contra el origen actual (`http://localhost:8080`, porque el frontend vive dentro
del mismo WAR).

### FASE 3 — Llega a GlassFish y se rutea

Ver secciones 2 y 3 de arriba — GlassFish recibe el socket, identifica el context root, y
Jersey (el motor JAX-RS) crea una instancia de `ProductoResource`, inyectándole
`@EJB private ProductoService productoService;`. Ojo: `ProductoService` aquí es una
**interfaz** (vive en `org.example.lib`) — GlassFish resuelve sola cuál es la única
implementación disponible (`org.example.ejb.ProductoServiceImpl`) y te da una instancia de
esa (del pool, porque es `@Stateless`). El `Resource` nunca escribe el nombre
`ProductoServiceImpl` en ningún lado.

### FASE 4 — Entra el Resource, que ya casi no hace nada por sí mismo

```java
// rest/ProductoResource.java
@GET
public List<ProductoDTO> listar() {
    return productoService.listar();
}
```
Nota lo corto que quedó: **el Resource ya no convierte nada** — ni siquiera aparece
`ProductoMapper` en este archivo. Solo traduce "llegó un GET a esta URL" en "llama este
método de la interfaz", y lo que sea que ese método regrese, lo envuelve en la respuesta
HTTP. Toda la conversión Entity↔DTO se movió una capa más adentro (FASE 5).

### FASE 5 — Entra el Service (negocio), que llama al Repository

```java
// ejb/ProductoServiceImpl.java
@Stateless
public class ProductoServiceImpl implements ProductoService {

    @EJB
    private ProductoRepository productoRepository;   // otra interfaz, de lib/

    @Override
    public List<ProductoDTO> listar() {
        return productoRepository.listar().stream()
                .map(ProductoMapper::toDTO)
                .collect(Collectors.toList());
    }
}
```
Dos cosas pasan aquí:
1. `productoService.listar()` (llamado desde el Resource) en realidad ejecuta ESTE método,
   porque `ProductoServiceImpl` es la implementación real de la interfaz `ProductoService`.
2. Aquí adentro se inyecta **otra** interfaz, `ProductoRepository` (también de `lib/`),
   resuelta a `org.example.ejb.ProductoRepositoryImpl`. El contenedor EJB ya abrió una
   transacción JTA por detrás desde que entró a este método (default `REQUIRED`); cuando la
   llamada entra a `ProductoRepositoryImpl` un renglón después, **se une a esa misma
   transacción**, no abre una nueva — es la misma regla que ya conocías, solo que ahora
   cruza un bean más.

### FASE 6 — Entra JPA/EclipseLink y toca la base real

```java
// ejb/AbstractRepository.java -- heredado por ProductoRepositoryImpl
public List<T> listar() {
    String jpql = "SELECT e FROM " + getEntityClass().getSimpleName() + " e ORDER BY e.id";
    return em.createQuery(jpql, getEntityClass()).getResultList();
}
```
- `em` viene inyectado vía `@PersistenceContext(unitName = "HelloJakartaPU")` → cadena
  completa en `persistencia-derbypool.md`.
- EclipseLink traduce el JPQL a SQL real, lo manda a Derby (puerto 1527), recibe filas
  crudas, y las convierte en objetos `Producto` (hidratación, usando `@Column` para saber
  qué columna va en qué campo).
- Esto regresa un `List<Producto>` (entidades) hacia `ProductoServiceImpl` — que es
  exactamente donde entra el `.map(ProductoMapper::toDTO)` que ya viste en la FASE 5.

### FASE 7 — De Java a JSON, y de vuelta a React

1. `listar()` retorna `List<ProductoDTO>` → Jersey envuelve en `200 OK` (gracias a
   `@Produces(MediaType.APPLICATION_JSON)` en la clase).
2. JSON-B (Yasson) recorre los getters de cada DTO y arma el array JSON.
3. El navegador recibe la respuesta; `response.json()` convierte el texto en objetos JS
   planos (no instancias Java — el tipo `ProductoDTO` de TypeScript es solo una anotación
   de compilación, no valida nada en runtime).
4. TanStack Query cachea el array bajo `["productos"]`, `data` deja de ser `undefined`.
5. `ProductosPanel` renderiza `<ProductosTable productos={data ?? []} ... />` — pasa el
   array como **prop**.
6. `useReactTable({ data: productos, columns, getCoreRowModel: getCoreRowModel() })` calcula
   filas/columnas; el JSX las convierte en `<tr>`/`<td>` reales.
7. React pinta eso en el DOM — el instante en que una fila de un archivo binario de Derby en
   disco termina siendo texto visible en pantalla.

```
Derby (archivo en disco)
   │  SQL
   ▼
EclipseLink (fila → objeto Producto)
   │  JPQL/EntityManager
   ▼
ProductoRepositoryImpl.listar()  [ejb, extiende AbstractRepository -- solo datos]
   │  List<Producto>
   ▼
ProductoServiceImpl.listar()  [ejb, @Stateless -- aqui pasa Entity -> DTO via Mapper]
   │  ProductoMapper.toDTO()  [Producto → ProductoDTO]
   ▼
ProductoResource.listar()  [rest, JAX-RS -- solo delega, ya recibe DTO listo]
   │  JSON-B: List<ProductoDTO> → texto JSON
   ▼
HTTP response (200, application/json)
   │
   ▼
fetch() en api/client.ts  [JSON → objetos JS planos]
   │
   ▼
TanStack Query (useQuery)  [cachea, expone data/isLoading/isError]
   │  prop `productos`
   ▼
ProductosTable + TanStack Table  [arma filas/columnas]
   │
   ▼
JSX → DOM → lo que ves en pantalla
```

---

## 6. Por dentro: cómo interactúan las clases (`lib`/`ejb`, generics, inyección)

Esta sección corrige varios malentendidos comunes al ver este código por primera vez. Se
reescribió cuando el proyecto pasó de una sola clase heredable (`CrudService`) a dos capas
separadas por interfaz: `Repository` (datos) y `Service` (negocio) — ver `DOCUMENTATION.md`
sección 1 para el panorama completo.

### 6.1 Las interfaces de `lib/` NO están "registradas por Jakarta" — son solo contratos

`ProductoRepository`, `ProductoService`, etc. (en `org.example.lib`) son interfaces Java
comunes, **sin ninguna anotación de Jakarta encima**. GlassFish no las "registra" — son
solo la forma/el contrato que algo más debe cumplir.

Lo que sí se registra son las clases de `org.example.ejb`:
```java
@Stateless
public class ProductoRepositoryImpl extends AbstractRepository<Producto, Long>
        implements ProductoRepository { ... }
```
`@Stateless` le dice a GlassFish "esto sí es un EJB, regístralo". Fíjate que esta clase hace
**dos cosas a la vez**: **hereda** de `AbstractRepository` (para no repetir el código de
`crear`/`listar`/`buscarPorId`/`eliminar`) e **implementa** `ProductoRepository` (para que
otros beans puedan inyectarla sin conocer su nombre real). Son dos mecanismos distintos de
Java funcionando juntos, no el mismo:
- **Herencia** (`extends`) = "tomo prestado código ya escrito".
- **Implementación de interfaz** (`implements`) = "prometo que voy a tener estos métodos,
  para que alguien más pueda usarme sin saber quién soy".

### 6.2 Cómo GlassFish decide qué clase concreta inyectar quiere una interfaz

```java
// ejb/ProductoServiceImpl.java
@EJB
private ProductoRepository productoRepository;
```
`ProductoRepository` es la interfaz. En **todo el proyecto** existe una única clase que la
implementa (`ProductoRepositoryImpl`) — por eso GlassFish puede resolverlo sin ambigüedad:
al arrancar, escaneó (con reflection, igual que con `@Path`) todas las clases del WAR,
encontró que `ProductoRepositoryImpl implements ProductoRepository`, y anotó esa relación en
su "libreta interna" (la misma idea que ya viste con las rutas HTTP, aplicada aquí a
inyección de dependencias en vez de URLs). Si algún día existieran DOS implementaciones de
la misma interfaz sin decirle a GlassFish cuál usar, ahí sí daría un error de deploy por
ambigüedad — no es magia infinita, solo automático mientras sea único.

### 6.3 El `EntityManager` se inyecta HACIA la clase, no se "registra en" él

```java
// ejb/AbstractRepository.java
@PersistenceContext(unitName = "HelloJakartaPU")
protected EntityManager em;
```
Flujo real:
1. GlassFish necesita crear una instancia de `ProductoRepositoryImpl` (por `@Stateless`).
2. Revisa **todos los campos del objeto — incluyendo los heredados de `AbstractRepository`**
   — buscando anotaciones de inyección.
3. Encuentra `em` (heredado) con `@PersistenceContext`, y le **entrega** un `EntityManager`
   ya funcional.

No es que `AbstractRepository` "se registre en" el EntityManager — GlassFish **regala** un
EntityManager a cualquier bean que lo pida, sin importar si el campo está declarado directo
en la clase o heredado.

### 6.4 Cómo se arma el JPQL (y cuándo se conecta de verdad a la base)

```java
public List<T> listar() {
    String jpql = "SELECT e FROM " + getEntityClass().getSimpleName() + " e ORDER BY e.id";
    return em.createQuery(jpql, getEntityClass()).getResultList();
}
```
- `getEntityClass()` es abstracto — cada hijo lo implementa obligatoriamente:
  ```java
  @Override
  protected Class<Producto> getEntityClass() {
      return Producto.class;
  }
  ```
  `Producto.class` es "la clase Producto, como dato que se puede pasar de un lado a otro"
  (un objeto `Class`). `.getSimpleName()` da el nombre en texto: `"Producto"`.
- Para `ProductoRepositoryImpl`, la línea arma literalmente:
  `"SELECT e FROM Producto e ORDER BY e.id"`.
- **Esto NO es SQL** — es **JPQL** (Jakarta Persistence Query Language), habla de clases
  Java (`Producto`), no de tablas (`PRODUCTO`). EclipseLink lo traduce a SQL real después.
- **Armar el texto no toca la base de datos.** Es manipulación de strings en memoria, sin
  red. La conexión real sucede hasta `.getResultList()` — esa es la línea que manda la
  consulta por la red hacia Derby y trae las filas.

### 6.5 Dos formas distintas de "resolver algo", en dos momentos distintos

Esto es nuevo respecto a la versión anterior de este documento — vale la pena que quede
clarísima la diferencia:

| | `extends AbstractRepository<Producto, Long>` | `@EJB private ProductoRepository ...` |
|---|---|---|
| Mecanismo | Herencia (generics) | Inyección de dependencias (interfaz) |
| ¿Cuándo se resuelve? | **En compilación** (`javac`) | **En deploy** (GlassFish, con reflection) |
| ¿Quién lo resuelve? | El compilador de Java | El contenedor EJB de GlassFish |
| ¿Qué pasa si falla? | No compila, ni siquiera generas el `.class` | Compila bien, pero el deploy del WAR falla |

`class ProductoRepositoryImpl extends AbstractRepository<Producto, Long>` se resuelve
**cuando compilas** (`mvn package` → `javac`) — el compilador entiende, en ese momento, que
el `.class` final debe incluir todo el comportamiento de `AbstractRepository`, con `T`
reemplazado por `Producto` y `ID` reemplazado por `Long`. Queda fijo en el archivo
compilado, sin ningún proceso en runtime buscando esta relación.

En cambio, `@EJB private ProductoRepository productoRepository;` se resuelve **hasta que
GlassFish despliega el WAR** — es en ese momento (no antes) cuando decide "la única clase
que implementa esto es `ProductoRepositoryImpl`, te doy una instancia de esa". Por eso un
error de inyección (ej. ninguna clase implementa la interfaz) nunca lo ves como error de
compilación — el código compila perfecto, y el problema solo aparece al desplegar.

### 6.6 Generics desde cero

Ejemplo de juguete, sin Jakarta:
```java
abstract class Fabrica<Cosa, Codigo> {
    public abstract Cosa fabricar();
    public abstract Codigo etiquetar(Cosa c);
}
```
`<Cosa, Codigo>` son **dos espacios en blanco** ("parámetros de tipo") — nombres inventados,
solo etiquetas para "un tipo que todavía no sé cuál es". Al heredar, hay que decir con qué
tipos reales se rellenan:
```java
class FabricaDePan extends Fabrica<Pan, String> {
    public Pan fabricar() { return new Pan(); }
    public String etiquetar(Pan p) { return "PAN-001"; }
}
```
`Cosa` se volvió `Pan` en todos lados, `Codigo` se volvió `String` en todos lados — es como
si `Fabrica` hubiera estado escrita, solo para `FabricaDePan`, así desde el principio:
```java
abstract class FabricaDePan {
    public abstract Pan fabricar();
    public abstract String etiquetar(Pan p);
}
```

**Exactamente lo mismo pasa con `Repository<T, ID>` (la interfaz) y `AbstractRepository<T, ID>` (la implementación compartida):**
```java
public interface Repository<T, ID> {
    T crear(T entidad);
    List<T> listar();
    T buscarPorId(ID id);
    boolean eliminar(ID id);
}

public abstract class AbstractRepository<T, ID> implements Repository<T, ID> {
    public T crear(T entidad) { ... }
    public List<T> listar() { ... }
    public T buscarPorId(ID id) { ... }
    public boolean eliminar(ID id) { ... }
}
```
`class ProductoRepositoryImpl extends AbstractRepository<Producto, Long> implements ProductoRepository`
es como si todo eso se hubiera escrito, solo para `ProductoRepositoryImpl`, así (esto no lo
escribes tú, lo resuelve el compilador):
```java
public Producto crear(Producto entidad) { ... }
public List<Producto> listar() { ... }
public Producto buscarPorId(Long id) { ... }
public boolean eliminar(Long id) { ... }
```

### 6.7 Aclaración puntual: `Long` no tiene NADA que ver con `List`

Malentendido común: pensar que `Long` (en `Repository<Producto, Long>`) es "parte de la
lista". Son dos cosas **completamente distintas**, solo comparten la sintaxis `<...>`:

- **`Long`** — la clase de Java que representa **un número entero** guardado como objeto.
  Se usa aquí únicamente porque así se declaró el campo `id`:
  ```java
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;   // <- por esto "Long" como segundo parametro de Repository<T, ID>
  ```
  Si una entidad futura tuviera un `id` de tipo texto (ej. un UUID), sería
  `Repository<OtraEntidad, String>` en vez de `Long`.
- **`List`** — una interfaz de Java totalmente distinta: "una colección ordenada de cosas".
  Aparece en `listar()`, cuyo retorno es `List<T>` → para `ProductoRepository`,
  `List<Producto>` ("una lista de Productos").

Ambos usan `<...>` porque ambos usan **generics** (la misma característica del lenguaje,
nada de Jakarta) — pero `Long` responde "¿de qué tipo es el ID?", y `List` responde "¿qué
forma tiene la colección que regresa `listar()`?". Coincide la sintaxis, no el propósito.
