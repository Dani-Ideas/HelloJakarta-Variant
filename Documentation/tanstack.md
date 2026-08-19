# TanStack — solo lo que se usa en este proyecto

Tres librerías distintas de la familia TanStack, cada una resolviendo un problema distinto.
Ninguna trae estilos visuales — todas son "headless" (sin cabeza, sin apariencia propia):
te dan la lógica y el estado, tú pones el JSX/CSS. Por eso el `index.css` que armamos es
100% nuestro.

---

## Parte 1 — TanStack Query (`@tanstack/react-query`)

### Qué problema resuelve

Sin esta librería, para traer datos de una API tendrías que escribir a mano, en cada
componente: una variable de estado para los datos, otra para "está cargando", otra para
"hubo un error", un `useEffect` que dispare el `fetch`, y lógica para evitar pedir lo mismo
dos veces. TanStack Query hace todo eso por ti con un solo hook, y además **cachea** los
resultados (si dos componentes piden lo mismo, solo se hace una petición real).

### `QueryClient` + `QueryClientProvider` (`main.tsx`)

```tsx
const queryClient = new QueryClient();

<QueryClientProvider client={queryClient}>
  <RouterProvider router={router} />
</QueryClientProvider>
```

`QueryClient` es el objeto que guarda toda la caché de datos de la app. Se crea **una sola
vez** y se lo pasas a `QueryClientProvider`, que envuelve toda la aplicación — así cualquier
componente adentro (sin importar qué tan anidado esté) puede acceder a esa caché compartida.

### `useQuery` (`ProductosTable.tsx`, `FacturasTable.tsx`)

```tsx
const { data, isLoading, isError, error } = useQuery({
  queryKey: ["productos"],
  queryFn: fetchProductos,
});
```

- **`queryFn`**: la función que realmente trae los datos — en este proyecto, una de las
  funciones de `api/client.ts` que hace `fetch()` al backend.
- **`queryKey`**: un identificador único para *esta* consulta específica dentro de la
  caché — un array (`["productos"]`, `["facturas"]`). Si en algún lado del código volvieras
  a usar `useQuery({queryKey: ["productos"], ...})`, TanStack Query reutilizaría el mismo
  dato cacheado en vez de volver a pedirlo al servidor.
- Lo que retorna siempre trae (entre otras cosas que no usamos aquí):
  - `data` → los datos, una vez que llegaron (antes de eso, `undefined`)
  - `isLoading` → `true` mientras la primera petición está en curso
  - `isError` / `error` → si el `fetch` falló (por ejemplo, backend caído o CORS mal
    configurado)

Este patrón es el que se repite igual en `ProductosTable` (con `fetchProductos`) y en
`FacturasTable` (con `fetchFacturas`) — es la razón por la que ninguno de los dos
componentes tiene que manejar loading/error a mano.

---

## Parte 2 — TanStack Table (`@tanstack/react-table`, v8)

### Qué problema resuelve

Construir una tabla con columnas, filas, y comportamientos (como expandir una fila) tiene
mucha lógica repetitiva. TanStack Table calcula toda esa lógica (qué columnas hay, qué filas
mostrar, cuáles están expandidas) — pero **no dibuja ni un solo `<table>` ni `<tr>`**, eso
lo escribes tú en JSX. Por eso se ve más código del que esperarías de una "librería de
tablas": la parte visual es 100% nuestra.

> Nota de versión: se instaló a propósito la **v8** (no la v9, que ya estaba disponible pero
> con una API totalmente distinta y muy nueva) porque es la versión estable y bien
> documentada.

### Definir columnas — `createColumnHelper`

```tsx
const columnHelper = createColumnHelper<ProductoDTO>();

const columns = [
  columnHelper.accessor("nombre", { header: "Nombre" }),
  columnHelper.accessor("precio", {
    header: "Precio",
    cell: (info) => formatoMoneda.format(info.getValue()),
  }),
];
```

`createColumnHelper<T>()` es solo una fábrica de definiciones de columna con autocompletado
correcto para el tipo `T` (en este caso `ProductoDTO`). Dos formas de columna usadas:
- **`.accessor("campo", {...})`** → columna que muestra un campo real del objeto. El
  `header` es el texto de la cabecera; `cell` es opcional — si lo defines, controla cómo se
  ve el valor (aquí, formateado como moneda en vez del número crudo).
- **`.display({...})`** (usado en `FacturasTable.tsx` para la columna del botón `+`/`−`) →
  columna que **no** viene de ningún campo del dato, existe solo para mostrar algo
  calculado/interactivo (el botón de expandir).

### El hook central — `useReactTable`

```tsx
const table = useReactTable({
  data: data ?? [],
  columns,
  getCoreRowModel: getCoreRowModel(),
});
```

Este hook junta tus datos (`data`, lo que vino de `useQuery`) con tu definición de columnas,
y retorna el objeto `table` que vas a usar para pintar todo. `getCoreRowModel()` es
obligatorio siempre — es la función que calcula "cuáles son las filas base" (sin esto, la
tabla no sabe ni iterar sus propias filas).

### Pintar la tabla con el objeto `table`

Este patrón se repite igual en ambos componentes:

```tsx
table.getHeaderGroups().map((headerGroup) =>
  headerGroup.headers.map((header) =>
    flexRender(header.column.columnDef.header, header.getContext())
  )
)

table.getRowModel().rows.map((row) =>
  row.getVisibleCells().map((cell) =>
    flexRender(cell.column.columnDef.cell, cell.getContext())
  )
)
```

- **`getHeaderGroups()`** → las cabeceras a pintar (un array, porque TanStack soporta
  cabeceras agrupadas en varios niveles; aquí solo usamos un nivel).
- **`getRowModel().rows`** → las filas ya calculadas, listas para iterar.
- **`getVisibleCells()`** → las celdas de una fila.
- **`flexRender(definicion, contexto)`** → por qué existe esta función en vez de simplemente
  escribir `header.column.columnDef.header`: porque ese valor puede ser un `string` simple
  (`"Nombre"`) **o** una función que retorna JSX (como el `cell` de precio, o el botón de
  expandir). `flexRender` sabe manejar ambos casos de forma uniforme, así el código de
  pintado no necesita un `if` para distinguir cuál es cuál.

### Filas expandibles (solo en `FacturasTable.tsx`)

```tsx
const [expanded, setExpanded] = useState<ExpandedState>({});

const table = useReactTable({
  // ...
  state: { expanded },
  onExpandedChange: setExpanded,
  getRowCanExpand: () => true,
  getExpandedRowModel: getExpandedRowModel(),
});
```

- `getExpandedRowModel()` → habilita la funcionalidad de expandir/colapsar filas (sin esto,
  `row.getIsExpanded()` siempre daría `false`).
- `getRowCanExpand: () => true` → le dice a la tabla que cualquier fila puede expandirse
  (podría ser condicional, ej. solo si tiene detalles, pero aquí todas las facturas los
  tienen).
- `state: { expanded }` + `onExpandedChange: setExpanded` → esto es lo que conecta el
  `useState` de React (ver `react.md`) con TanStack Table: la tabla no guarda su propio
  estado de expansión, **nosotros** lo guardamos (con `useState`) y se lo "prestamos" a la
  tabla — así nuestro componente sabe en todo momento qué filas están abiertas.
- `row.getToggleExpandedHandler()` → función lista para poner directo en el `onClick` del
  botón `+`/`−`, alterna el estado de esa fila.
- `row.getIsExpanded()` → `true`/`false`, se usa tanto para decidir qué símbolo mostrar en
  el botón (`+` o `−`) como para decidir si se pinta la fila extra con el detalle de la
  factura debajo.

---

## Parte 3 — TanStack Router (`@tanstack/react-router`, v1)

### Qué problema resuelve

En una app tradicional (sin JavaScript de por medio), cada link que clicas le pide una
página HTML completa nueva al servidor — el navegador tira todo lo que tenía cargado y
reconstruye desde cero (por eso el "parpadeo" en blanco entre página y página). Con React,
todo el JavaScript ya está cargado en el navegador desde el principio — "navegar" a otra
página no debería significar pedirle nada nuevo al servidor, solo cambiar qué se muestra.
TanStack Router es quien se encarga de: mantener la URL de la barra de direcciones
sincronizada con qué componente se ve, sin recargar nada.

### El árbol de rutas — `router.tsx`

```tsx
const rootRoute = createRootRoute({ component: RootLayout });

const productosRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/productos",
  component: ProductosPage,
});

const routeTree = rootRoute.addChildren([indexRoute, productosRoute, facturasRoute]);

export const router = createRouter({ routeTree, basepath: "/HelloJakarta-variante" });
```

- **`createRootRoute`** → la ruta que se renderiza siempre, sin importar la URL. Su
  `component` (`RootLayout`) trae el `<Outlet/>` — el "hueco" donde se inserta la página
  activa.
- **`createRoute`** → una página concreta. `getParentRoute` dice de quién "cuelga" en el
  árbol; `path` es la URL; `component` es qué se muestra ahí.
- **`routeTree`** → junta todas las rutas sueltas en una sola estructura, la que
  `createRouter` necesita para saber qué existe.
- **`basepath`** → igual que el `base` de Vite (ver `frontend.md`), le dice al router bajo
  qué ruta real vive el WAR (`/HelloJakarta-variante/`), para que los links que genera
  apunten al lugar correcto. Mismo problema que los assets, misma solución.

### `<Outlet/>` (`routes/RootLayout.tsx`)

```tsx
<main>
  <Outlet />
</main>
```

Literalmente "aquí va la página que corresponda". `RootLayout` se dibuja una sola vez (el
header, el `<nav>`) y `Outlet` es lo único que cambia cuando cambias de página.

### `<Link>` (no confundir con un `<a>` normal)

```tsx
<Link to="/productos" activeProps={{ className: "nav-activo" }}>
  Productos
</Link>
```

- `to="/productos"` — **sin** el `basepath`, el router lo agrega solo al armar el `href`
  real.
- Intercepta el clic: en vez de dejar que el navegador recargue, actualiza la URL (con el
  History API del navegador) y le avisa al router que muestre otro `component` en el
  `Outlet`.
- `activeProps` → props extra (aquí, una clase CSS) que se aplican automáticamente **solo**
  cuando la URL actual coincide con ese link — así se ve resaltado el ítem del menú en el
  que estás parado.
- `activeOptions={{ exact: true }}` (usado solo en el link de "/") — sin esto, como toda URL
  empieza con `/`, ese link se marcaría "activo" incluso estando en `/productos`.

### `RouterProvider` (`main.tsx`)

```tsx
<RouterProvider router={router} />
```

Reemplaza lo que antes era `<App />` renderizado directo — ahora quien decide qué mostrar es
el router, leyendo la URL actual contra el árbol de `router.tsx`.

### El problema que esto introduce, y cómo se resolvió (`SpaFallbackFilter`)

Como `/productos` y `/facturas` **nunca existen como archivos reales** (son puro
JavaScript decidiendo qué mostrar), si alguien pide esa URL **directo** al servidor
(escribiéndola a mano, o dando F5 estando ahí), GlassFish no tiene nada que entregarle —
404. Se resuelve con un filtro Java (`rest/SpaFallbackFilter.java`, `@WebFilter("/*")`)
que reenvía a `index.html` **solo** cuando la ruta no es `/api/*` y no corresponde a un
archivo real — así React y el router arrancan de nuevo y leen la URL actual para mostrar la
página correcta, sin interferir con los `404` reales de la API (el intento inicial usaba un
`error-page` en `web.xml`, pero eso interceptaba TAMBIÉN los `404` legítimos de la API,
rompiendo `PUT`/`DELETE`). Detalle técnico completo, con los dos bugs y sus causas reales,
en `bitacora-fixes.md` (incidentes 9 y 10).
