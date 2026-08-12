# TanStack — solo lo que se usa en este proyecto

Dos librerías distintas de la familia TanStack, cada una resolviendo un problema distinto.
Ninguna trae estilos visuales — ambas son "headless" (sin cabeza, sin apariencia propia):
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
  <App />
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
