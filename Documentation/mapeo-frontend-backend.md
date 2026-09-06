# Mapeo completo: página → botón → `api/client.ts` → Controller

Este documento conecta **cada botón/página del frontend** con la petición HTTP exacta que
dispara, y con el método del `Controller` que la recibe del lado del backend. Todo lo de
abajo está verificado leyendo el código real (`frontend/src/`), no es una descripción
genérica.

---

## 1. El mecanismo de conexión — una sola vez, aplica a TODO

Todo pasa por un único archivo: `frontend/src/api/client.ts`. Es el **único lugar** de
todo el frontend que llama `fetch()`.

```ts
// api/client.ts
const API_BASE = "/HelloJakarta-variante/api";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!response.ok) throw new Error(await parseErrorBody(response));
  if (response.status === 204) return undefined as T;
  return response.json();
}
```

**Aquí está la sintaxis exacta donde se "conecta" con el backend**: la línea
`` `${API_BASE}${path}` `` — es concatenación de strings, nada más. `API_BASE` es fijo
(`/HelloJakarta-variante/api`), y cada función exportada (`fetchProductos`, `createProducto`,
etc.) le pasa el pedazo que falta (`/productos`, `/facturas/5`, etc.). El resultado final es
literalmente la URL que el navegador pide.

Encima de `request()`, 5 funciones concretas (todo lo que existe hoy):

```ts
export function fetchProductos(): Promise<ProductoDTO[]> {
  return request<ProductoDTO[]>("/productos");                          // GET
}
export function fetchFacturas(): Promise<FacturaDTO[]> {
  return request<FacturaDTO[]>("/facturas");                            // GET
}
export function createProducto(producto: ProductoInput): Promise<ProductoDTO> {
  return request<ProductoDTO>("/productos", { method: "POST", body: JSON.stringify(producto) });
}
export function updateProducto(id: number, producto: ProductoInput): Promise<ProductoDTO> {
  return request<ProductoDTO>(`/productos/${id}`, { method: "PUT", body: JSON.stringify(producto) });
}
export function deleteProducto(id: number): Promise<void> {
  return request<void>(`/productos/${id}`, { method: "DELETE" });
}
```

Nota: **no existe todavía** `createFactura`/`updateFactura`/`deleteFactura` — el frontend
solo lee facturas (`fetchFacturas`), no las crea ni edita desde la UI.

### Del `fetch()` al `Controller` — la cadena completa

```
fetch("/HelloJakarta-variante/api/productos")
   │
   ▼
Navegador manda: GET /HelloJakarta-variante/api/productos
   │
   ▼
GlassFish ve el contexto "/HelloJakarta-variante"  <- viene del <finalName> en pom.xml
   │
   ▼
ControllerRegistry: @ApplicationPath("/api")        <- consume "/api"
   │
   ▼
ProductoController: @Path("/productos")             <- consume "/productos", coincide exacto
   │
   ▼
@GET public Response listar() { ... }                <- GET sin @Path adicional = este metodo
```

Los 3 pedazos (`finalName`, `@ApplicationPath`, `@Path` del `Controller`) se concatenan
para formar la URL completa — si cualquiera de los 3 no coincide, no hay match y da `404`.

### Quién dispara `request()` — TanStack Query, no el componente directo

Ningún componente llama `fetchProductos()` a mano. Lo hace `useQuery`/`useMutation`:

```ts
// se dispara SOLO -- en cuanto el componente se monta, si no hay nada cacheado para
// la queryKey ["productos"]
useQuery({ queryKey: ["productos"], queryFn: fetchProductos });

// se dispara SOLO cuando llamas a crearMutation.mutate(datos) -- nunca antes
useMutation({ mutationFn: createProducto, onSuccess: () => invalidar() });
```

`onSuccess: invalidar()` es la pieza que conecta una escritura con que la tabla se
actualice sola: marca `["productos"]` como "viejo", y TanStack Query vuelve a llamar
`fetchProductos` automáticamente — sin que el componente tenga que hacerlo a mano.

---

## 2. Página por página, botón por botón

### `/` — `HomePage`

**Ninguna llamada a la API.** Son puros `<Link>` de TanStack Router (navegación entre
páginas) y un SVG decorativo. Los 4 botones ("Ver productos"/"Ver facturas"/"Ver sesiones
de caja"/"Ver usuarios") solo cambian de URL — la llamada real ocurre cuando ya cargó la
página destino (ver abajo). El "Menú de pago" (3 gajos) tampoco toca el backend — son demos
100% front-end.

### `/productos` — `ProductosPage` → `ProductosPanel`

| Acción del usuario | Función de `client.ts` | HTTP | Método del `Controller` |
|---|---|---|---|
| Entrar a la página (automático) | `fetchProductos()` | `GET /productos` | `ProductoController.listar()` |
| Botón **"+ Nuevo producto"** → llenar `ProductoForm` → **"Guardar"** | `createProducto(datos)` | `POST /productos` | `ProductoController.crear()` |
| Botón **"Editar"** (por fila) → `ProductoForm` precargado → **"Guardar"** | `updateProducto(id, datos)` | `PUT /productos/{id}` | `ProductoController.actualizar()` |
| Botón **"Eliminar"** (por fila) → confirmación del navegador | `deleteProducto(id)` | `DELETE /productos/{id}` | `ProductoController.eliminar()` |
| Botón **"Cancelar"** (dentro del form) | — ninguna | — | — solo cierra el formulario, `onCancelar()` es puro estado local de React |

`ProductosPanel.tsx` es el único componente que decide **cuál** mutación disparar — mira
`manejarGuardar()`: si `editando` tiene un producto (viniste de "Editar"), llama
`actualizarMutation`; si no, llama `crearMutation`. `ProductoForm.tsx` no sabe nada de la
API, solo junta los valores del formulario en un objeto y llama `onGuardar(datos)` — la
decisión de crear vs actualizar vive un nivel arriba.

**Nota**: como `Producto` **no** tiene `PATCH` conectado en el frontend (aunque el backend
sí lo expone desde el incidente #14) — la edición siempre manda el objeto completo por
`PUT`, nunca un cambio parcial.

### `/facturas` — `FacturasPage` → `FacturasTable`

| Acción del usuario | Función de `client.ts` | HTTP | Método del `Controller` |
|---|---|---|---|
| Entrar a la página (automático) | `fetchFacturas()` | `GET /facturas` | `FacturaController.listar()` |
| Botón **+/−** (expandir fila) | — ninguna | — | Los `detalles` de esa factura **ya vinieron** en el `GET` inicial (`FacturaDto.detalles`, ver incidente #18) — expandir solo muestra/oculta filas que ya están en memoria, `getExpandedRowModel()` de TanStack Table, cero red |

No hay botón de crear/editar/borrar factura en esta página — coincide con que
`client.ts` tampoco tiene esas funciones. Si algún día se agrega un formulario de "Nueva
factura", ahí sí haría falta escribir `createFactura()` en `client.ts` primero.

### `/sesiones-caja` — `SesionCajaPage` → `SesionCajaPanel`

| Acción del usuario | Función de `client.ts` | HTTP | Método del `Controller` |
|---|---|---|---|
| Entrar a la página (automático) | `fetchSesionesCaja()` | `GET /sesiones-caja` | `SesionCajaController.listar()` |
| Botón **"+ Abrir caja"** → llenar `SesionCajaForm` → **"Abrir caja"** | `crearSesionCaja(datos)` | `POST /sesiones-caja` | `SesionCajaController.crear()` |

Sin "Editar"/"Eliminar" — `SesionCajaController` no expone esos verbos (ver
`Documentation/paginas-sesion-caja-y-usuarios.md`, sección 3, para el porqué).

### `/usuarios` — `UsuariosPage` → `UsuariosPanel`

| Acción del usuario | Función de `client.ts` | HTTP | Método del `Controller` |
|---|---|---|---|
| Entrar a la página (automático) | `fetchUsuarios()` | `GET /usuarios` | `UsuarioController.listar()` |
| Botón **"+ Nuevo usuario"** → llenar `UsuarioForm` → **"Guardar"** | `createUsuario(datos)` | `POST /usuarios` | `UsuarioController.crear()` |
| Botón **"Editar"** (por fila) → `UsuarioForm` precargado → **"Guardar"** | `updateUsuario(id, datos)` | `PUT /usuarios/{id}` | `UsuarioController.actualizar()` |
| Botón **"Eliminar"** (por fila) → confirmación del navegador | `deleteUsuario(id)` | `DELETE /usuarios/{id}` | `UsuarioController.eliminar()` |

Mismo patrón exacto que `/productos` — el campo `rol` se llena con un `<select>` que solo
permite los 3 valores del enum `Rol.java` del backend (`ADMIN`/`VENDEDOR`/`CAJERO`).

### `/salir-sitio`, `/formulario-pago`, `/formulario-largo`

Demos de componentes shadcn/ui, **sin ninguna conexión al backend** — no llaman
`client.ts`, no usan `useQuery`/`useMutation`. Están ahí solo para practicar componentes de
UI, no consumen `/api/...` de ningún tipo.

---

## 3. Resumen — mapa completo en una tabla

| Endpoint backend | Controller.método | ¿Usado desde el frontend? | Desde dónde |
|---|---|---|---|
| `GET /productos` | `ProductoController.listar` | ✅ | `ProductosPanel` (al montar) |
| `GET /productos/{id}` | `ProductoController.buscar` | ❌ | no se usa (la tabla ya tiene todo del `listar`) |
| `POST /productos` | `ProductoController.crear` | ✅ | `ProductosPanel` → "+ Nuevo producto" |
| `PUT /productos/{id}` | `ProductoController.actualizar` | ✅ | `ProductosPanel` → "Editar" |
| `PATCH /productos/{id}` | `ProductoController.patch` | ❌ | existe en el backend, sin usar todavía |
| `DELETE /productos/{id}` | `ProductoController.eliminar` | ✅ | `ProductosPanel` → "Eliminar" |
| `GET /facturas` | `FacturaController.listar` | ✅ | `FacturasTable` (al montar) |
| `GET /facturas/{id}` | `FacturaController.buscar` | ❌ | no se usa |
| `POST /facturas` | `FacturaController.crear` | ❌ | sin `createFactura()` en `client.ts` todavía |
| `PUT`/`PATCH /facturas/{id}` | `FacturaController.actualizar`/`patch` | ❌ | sin usar |
| `GET /sesiones-caja` | `SesionCajaController.listar` | ✅ | `SesionCajaPanel` (al montar) |
| `GET /sesiones-caja/{id}` | `SesionCajaController.buscar` | ❌ | no se usa |
| `POST /sesiones-caja` | `SesionCajaController.crear` | ✅ | `SesionCajaPanel` → "+ Abrir caja" |
| `GET /usuarios` | `UsuarioController.listar` | ✅ | `UsuariosPanel` (al montar) |
| `GET /usuarios/{id}` | `UsuarioController.buscar` | ❌ | no se usa |
| `POST /usuarios` | `UsuarioController.crear` | ✅ | `UsuariosPanel` → "+ Nuevo usuario" |
| `PUT /usuarios/{id}` | `UsuarioController.actualizar` | ✅ | `UsuariosPanel` → "Editar" |
| `DELETE /usuarios/{id}` | `UsuarioController.eliminar` | ✅ | `UsuariosPanel` → "Eliminar" |

Las filas en ❌ no son errores — son simplemente funcionalidad del backend que el frontend
todavía no consume (`GET /{id}` no hace falta en ninguna entidad porque las tablas ya
traen todo del `listar`, y `Factura` solo se lee, no se crea/edita desde la UI por ahora).
