# `generar_capas.py` — de `@Entity` a Repository/DTO/Mapper/Service/Resource

Script: `back/scripts/generar_capas.py`. Resuelve un problema real de este proyecto: cada
entidad nueva necesita **6 archivos** que siguen siempre el mismo patrón mecánico
(`Repository`, `DTO`, `Mapper`, `Service`, `ServiceImpl`, `Resource`) — pura repetición de
tipeo, sobre todo cuando aparecen muchas tablas de golpe (el caso real que lo disparó: se
conectó una base con varias tablas y hacía falta escribir esa misma capa una vez por cada
una).

---

## 1. El hueco que tapa (y por qué no hay una herramienta ya hecha para esto)

IntelliJ (Ultimate) sí trae un generador nativo para el primer paso: **Persistence tool →
"Generate Persistence Mapping"** crea las clases `@Entity` a partir del esquema real de una
base de datos ya conectada (ver `Documentation/sqlserver.md` sección 2.1 para cómo se
conecta la base). Ese paso funciona bien y no hace falta reemplazarlo — las entidades se
generan solas, tú solo las verificas.

El salto siguiente (`@Entity` → `Repository`/`Mapper`/`Service`/`Resource`) **no lo cubre
ninguna herramienta estándar todavía**:

- **JHipster** (el generador de stack completo más conocido en el mundo Java) está
  construido sobre **Spring Boot + Spring Data JPA** — adoptarlo implicaría migrar de
  framework completo, no es un generador aparte que se pueda usar sobre este proyecto.
- **JPA Buddy** (plugin de pago de IntelliJ) sí genera DTOs/Mappers/Repository desde una
  `@Entity` con una UI dentro del IDE, pero está pensado para repositorios de **Spring
  Data** — no hay certeza de que reconozca `jakarta.data.repository.CrudRepository` igual
  de bien.
- **Jakarta Data** (la especificación detrás de `CrudRepository` en este proyecto) es parte
  de **Jakarta EE 11**, publicada apenas en 2024 — el ecosistema de tooling (IDEs, plugins,
  generadores) todavía no la cubre, a diferencia de Spring Data JPA, que lleva más de una
  década de adopción masiva.

Por eso este script no es "reinventar la rueda" — tapa un hueco real de esta combinación
específica de tecnologías (Jakarta Data + MapStruct + EJB + JAX-RS), no algo que ya viniera
resuelto en otro lado.

---

## 2. Qué genera (mecánico, siempre igual) y qué NO (decisiones de negocio)

| Archivo | Se genera | Detalle |
|---|---|---|
| `lib/<Entidad>Repository.java` | Sí | Interfaz vacía, `extends CrudRepository<Entidad, TipoId>` — Jakarta Data implementa el cuerpo solo, en tiempo de despliegue. |
| `dto/<Entidad>DTO.java` | Sí | `record` con un componente por cada campo simple de la entidad (mismo nombre y tipo). |
| `mapper/<Entidad>Mapper.java` | Sí | Interfaz MapStruct (`toDTO`/`toEntity`), `id` ignorado al construir una entidad nueva — igual que `ProductoMapper`. |
| `lib/<Entidad>Service.java` | Sí | Interfaz, `extends Service<DTO, TipoId>` + `actualizar()` (+ `eliminar()` si pides `--con-eliminar`). |
| `ejb/<Entidad>ServiceImpl.java` | Sí | `@Stateless`, inyecta `Repository`, usa `Mapper.INSTANCE`, implementa `crear/listar/buscarPorId/actualizar[/eliminar]` — sin `EntityManager` ni `flush()` (ver incidente #15 de `bitacora-fixes.md`, aplica igual aquí porque asume `GenerationType.SEQUENCE`). |
| `rest/<Entidad>Resource.java` | Sí | JAX-RS: `GET` lista, `GET /{id}`, `POST`, `PUT /{id}` (+ `DELETE /{id}` si pides `--con-eliminar`) — mismo patrón que `ProductoResource`. |
| Validaciones del DTO (`@NotBlank`, `@Positive`, etc.) | **No** | Se dejan como comentario `TODO` — son reglas de negocio reales, no se adivinan. |
| `patch()` / `<Entidad>PatchDTO` | **No** | Solo algunas entidades lo necesitan (ver `FacturaPatchDTO`) — es una decisión, no boilerplate universal. |
| El `@Path` correcto en plural | **Parcial** | Usa `entidad.lower() + "s"` como adivinanza simple. Para nombres compuestos (`SesionCaja` → `sesiones-caja`, no `sesioncajas`) hay que corregirlo a mano — pluralizar español no se automatiza bien de forma genérica. |
| Campos con relación (`@OneToMany`/`@ManyToOne`/`@ManyToMany`/`@OneToOne`/`@JsonbTransient`) | **No** | Se detectan y se **excluyen** del DTO/Mapper a propósito — ver sección 4, es la parte más importante de leer antes de correrlo sobre una entidad con relaciones. |

---

## 3. Cómo usarlo

```bash
cd back

# una entidad puntual
python3 scripts/generar_capas.py Usuario --con-eliminar

# TODAS las @Entity que encuentre en model/ de un jalón (el caso de "conecté 20 tablas")
python3 scripts/generar_capas.py --todas --con-eliminar

# --forzar sobrescribe archivos que ya existan (sin él, nunca toca lo que ya está)
python3 scripts/generar_capas.py Usuario --forzar
```

- **`--con-eliminar`**: sin esta bandera, el `Service`/`ServiceImpl`/`Resource` generado NO
  trae `eliminar()`/`DELETE` — replica el patrón de `Factura` (que deliberadamente no
  expone `DELETE`, ver `FacturaRepository.java`), no el de `Producto`. Si la entidad sí debe
  poder borrarse, agrega la bandera.
- **Nunca pisa archivos existentes** por default — corre seguro sobre un proyecto con
  entidades ya armadas a mano (`Producto`/`Factura`/`SesionCaja`), solo rellena lo que
  falte.
- El script imprime, al final, la misma lista de "pendiente a mano" de la sección 2 — no es
  un `crear y olvidar`, es un punto de partida que hay que revisar (mismo espíritu que
  verificar las `@Entity` que genera IntelliJ).

---

## 4. Caso real verificado: `Usuario`

Al correr `--todas` sobre el estado real de este repo, `Usuario` tenía `@Entity` +
`Repository` (generados antes, a mano) pero le faltaba todo lo demás. El script generó:

```
creado: src/main/java/org/example/dto/UsuarioDTO.java
creado: src/main/java/org/example/mapper/UsuarioMapper.java
creado: src/main/java/org/example/lib/UsuarioService.java
creado: src/main/java/org/example/ejb/UsuarioServiceImpl.java
creado: src/main/java/org/example/rest/UsuarioResource.java
```

Compiló limpio a la primera (`mvn package`, MapStruct generó `UsuarioMapperImpl` sin
advertencias). Verificado end-to-end contra la base real (ver `Documentation/sqlserver.md`)
con `curl`:

```bash
curl -X POST http://localhost:8080/HelloJakarta-variante/api/usuarios \
  -H "Content-Type: application/json" -d '{"nombre":"Dani","rol":"ADMIN"}'
# -> {"nombre":"Dani","rol":"ADMIN"}   (201, con Location)

curl http://localhost:8080/HelloJakarta-variante/api/usuarios
# -> [{"id":1,"nombre":"Dani","rol":"ADMIN"}]
```

`rol` es un `enum` (`Rol.java`) — el script lo detectó como "tipo no primitivo" y le agregó
el import de `org.example.model.Rol` solo, sin configuración adicional (MapStruct mapea
enums iguales en ambos lados automáticamente).

---

## 5. Caso límite real: por qué se descartó para `FacturaDetalle`

Correr `--todas` también encontró que `FacturaDetalle` no tenía Repository/Mapper/
Service/Resource. El script los generó, **compilaron**, pero se borraron después de
revisarlos — este es el caso que justifica por qué las relaciones se excluyen a propósito
(sección 2):

- `FacturaDetalleDTO` **ya existía**, escrito a mano, con esta forma:
  `id, productoId, nombreProducto, cantidad, precioUnitario, subtotal` — pensado para venir
  **anidado dentro de `FacturaDTO`** (una factura trae su lista de detalles), no para un
  CRUD independiente.
- La entidad `FacturaDetalle` en cambio tiene un campo `producto` (`@ManyToOne`), no
  `productoId`/`nombreProducto` sueltos — el generador correctamente **no intenta
  adivinar** cómo llenar esos dos campos a partir de la relación (sección 2, última fila).
- Resultado: el `FacturaDetalleMapper` generado compilaba, pero con
  `productoId`/`nombreProducto` **siempre en `null`** — MapStruct lo señaló solo, en el log
  del build:

  ```
  [WARNING] .../FacturaDetalleMapper.java:[17,23] Unmapped target properties: "productoId, nombreProducto".
  [WARNING] .../FacturaDetalleMapper.java:[22,20] Unmapped target properties: "factura, producto".
  ```

- Además, `FacturaDetalle` ya vive **dentro del agregado `Factura`**
  (`cascade = CascadeType.ALL, orphanRemoval = true`, ver `Factura.java`) — exponerle un
  `Resource` propio (`/facturadetalles`) abriría una segunda puerta para crear/borrar
  detalles por fuera de su factura, sin pasar por la lógica que sí vive en
  `FacturaServiceImpl` (recalcular `total`, etc.).

**Regla general que deja este caso**: antes de correr el script sobre una entidad, revisa
si tiene relaciones (`@ManyToOne`/`@OneToMany`) y si ya existe un DTO hecho a mano para
ella con otra forma — si alguna de las dos es cierta, lo más seguro es generar solo lo que
de verdad haga falta (o nada) y escribir esa capa a mano, como ya está hecho para
`SesionCajaMapper.java` (el único mapper de este proyecto escrito a mano, justo porque
`SesionCaja` sí necesita mapear su relación con `Factura`).

---

## 6. Cómo funciona por dentro (para cuando falle o haga falta ajustarlo)

El parseo de cada `model/<Entidad>.java` es un **regex, no un parser real de Java**
(`parsear_entidad()` en el script) — suficiente para el estilo de este proyecto (Lombok
`@Getter`/`@Setter`, campos `private Tipo nombre;` con anotaciones arriba), pero no
pretende cubrir cualquier `.java` del mundo. Cosas a saber si algo no matchea:

- Detecta el campo `@Id` por buscar literalmente `@Id` en las anotaciones que preceden al
  campo — si el `@Id` estuviera en una línea rara o con formato inusual, no lo va a
  encontrar y el script aborta con `No se encontro ningun campo @Id`.
- Excluye campos por **substring** en las anotaciones (`@OneToMany`, `@ManyToOne`, etc.) y
  por **prefijo de tipo** (`List<`, `Set<`, `Collection<`) — una relación declarada de otra
  forma (ej. un `Map<>`) no se detectaría como relación y se colaría al DTO.
- Los imports del DTO se resuelven con una lista corta de tipos conocidos
  (`BigDecimal`, `LocalDate`, etc.) más una heurística: cualquier tipo que no sea
  primitivo/`java.lang` se asume que vive en `org.example.model` (como `Rol`) — si algún
  día una entidad usa un tipo de otro paquete, el import generado quedará mal y hay que
  corregirlo a mano (el build fallaría con `cannot find symbol`, fácil de detectar).

`listar_entidades()` (usado por `--todas`) encuentra las `@Entity` buscando `class Nombre`
dentro de cualquier `.java` de `model/` que contenga la anotación `@Entity` — no distingue
`class` de `interface`/`record`, pero como solo mira archivos con `@Entity` (que siempre son
clases), no ha sido un problema en la práctica.
