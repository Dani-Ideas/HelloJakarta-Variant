# React — solo lo que se usa en este proyecto

Esta guía asume que **no sabes nada de React**. Cubre únicamente los conceptos que
aparecen en el código de `frontend/src/`, en el orden en que los vas a encontrar leyendo
los archivos.

---

## 1. Qué es React, en una frase

Una librería de JavaScript para construir interfaces como un árbol de **componentes** —
piezas reutilizables de UI, cada una responsable de una parte de la pantalla, que se
combinan entre sí. En este proyecto: `App` (raíz) contiene `ProductosTable` y
`FacturasTable`.

## 2. JSX — por qué hay HTML dentro del código Java... digo, JavaScript

Abre `App.tsx`:

```tsx
return (
  <div className="pagina">
    <h1>HelloJakarta</h1>
  </div>
);
```

Eso **no es un string ni HTML real** — es **JSX**, una extensión de sintaxis que permite
escribir algo que se ve como HTML directamente dentro del código TypeScript/JavaScript. Por
debajo, cada una de esas etiquetas se transforma (en tiempo de compilación, por Vite) a una
llamada de función de React. Nunca vas a escribir esa función a mano, JSX es el atajo.

Diferencias con HTML normal que vas a notar en el código:
- `className` en vez de `class` (porque `class` es palabra reservada en JavaScript)
- Los atributos con valores dinámicos van entre `{ }`: `className={header.column...}`
- Un componente siempre debe retornar **un solo elemento raíz** (por eso a veces se envuelve
  todo en un `<div>`, o en un `<>...</>` si no quieres agregar un elemento extra al DOM)

## 3. Componentes funcionales

```tsx
function App() {
  return ( /* JSX */ );
}

export default App;
```

Un componente en este proyecto es **una función de TypeScript que retorna JSX**. Así de
simple — no hay clases, no hay herencia. `ProductosTable` y `FacturasTable` (en
`components/`) siguen el mismo patrón: `export function ProductosTable() { ... }`.

## 4. Cómo se arma el árbol y cómo arranca

`main.tsx` es el punto de entrada real:

```tsx
createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
);
```

- `document.getElementById("root")` → el `<div id="root">` que está en `index.html`. Ahí es
  donde React "inyecta" toda la aplicación dentro del HTML real del navegador.
- `createRoot(...).render(<App />)` → le dice a React "dibuja el componente `App` (y todo lo
  que contenga) adentro de ese div".
- `<StrictMode>` → no renderiza nada visible, es un modo de desarrollo que ayuda a detectar
  errores comunes (a veces hace que un componente se ejecute dos veces a propósito, solo en
  desarrollo, para exponer efectos secundarios mal escritos).
- `<QueryClientProvider>` → viene de TanStack Query, ver `tanstack.md`. Envuelve toda la app
  para que cualquier componente adentro pueda usar `useQuery`.

El árbol completo de este proyecto:

```
main.tsx
 └─ QueryClientProvider
     └─ App
         ├─ ProductosTable
         └─ FacturasTable
```

## 5. Hooks — específicamente `useState` (usado en `FacturasTable.tsx`)

```tsx
const [expanded, setExpanded] = useState<ExpandedState>({});
```

Un **hook** es una función especial de React (siempre empieza con `use...`) que te deja
"engancharte" a capacidades de React dentro de un componente función — en este caso,
**tener memoria entre renders** (una función normal de JavaScript olvida todo cada vez que
se vuelve a ejecutar; un componente se re-ejecuta constantemente).

`useState(valorInicial)` retorna siempre un array de 2 posiciones:
1. El valor actual (`expanded`)
2. Una función para actualizarlo (`setExpanded`)

Cuando llamas `setExpanded(nuevoValor)`, React vuelve a ejecutar el componente con el nuevo
valor — así es como la tabla de facturas "recuerda" qué filas dejaste expandidas.

Regla que no se rompe en ningún lado del código: los hooks **siempre van al nivel superior**
del componente, nunca dentro de un `if` o un `.map()`.

## 6. Renderizar listas: `.map()` y la `key`

```tsx
{table.getRowModel().rows.map((row) => (
  <Fragment key={row.id}>
    <tr>...</tr>
  </Fragment>
))}
```

React no tiene una sintaxis especial para "repetir": usas el `.map()` normal de JavaScript
sobre un array, y cada elemento retorna JSX. Lo obligatorio es la prop **`key`**: un
identificador único por elemento de la lista, para que React sepa qué fila es cuál entre un
render y el siguiente (sin esto, si el orden cambia, React puede confundir una fila con
otra y mezclar el estado interno). Por eso usamos `key={row.id}` — el id que ya trae cada
fila de la tabla, nunca el índice de la posición.

## 7. Renderizado condicional

```tsx
if (isLoading) return <p className="estado">Cargando productos...</p>;
if (isError) return <p className="estado estado-error">Error: {(error as Error).message}</p>;

return <table>...</table>;
```

No hay un `<if>` en JSX — simplemente usas JavaScript normal *antes* del `return` final
(como aquí), o expresiones cortas dentro del JSX como `{row.getIsExpanded() && (<tr>...</tr>)}`
que se usa en `FacturasTable.tsx`: si `row.getIsExpanded()` es `false`, esa expresión entera
es `false`, y React simplemente no renderiza nada ahí; si es `true`, renderiza lo que sigue
del `&&`.

## 8. `Fragment` (`<>...</>` o `<Fragment key={...}>`)

Un componente/expresión JSX debe retornar **un solo elemento raíz**. Pero en
`FacturasTable.tsx` necesitábamos retornar dos `<tr>` hermanos (la fila principal + la fila
de detalle expandida) sin envolverlos en un `<div>` extra (que rompería la tabla HTML,
porque un `<div>` no es válido directo dentro de un `<tbody>`). `Fragment` es un envoltorio
"invisible" — agrupa elementos sin agregar ningún nodo real al HTML final. Se usó la forma
`<Fragment key={row.id}>` en vez del atajo `<>` porque, al estar dentro de un `.map()`,
necesita poder llevar la prop `key`.

## 9. TypeScript en React

Cada archivo de componente termina en `.tsx` (no `.jsx`) porque usa TypeScript. Lo que se
usa en este proyecto:
- **Interfaces** para los datos que vienen del backend (`api/types.ts`) — `ProductoDTO`,
  `FacturaDTO`, `FacturaDetalleDTO`, calcadas de los DTO de Java para que el editor te
  avise si usas un campo que no existe.
- **Genéricos** al usar TanStack (`createColumnHelper<ProductoDTO>()`) — le dicen a la
  librería "esta tabla trabaja con filas de este tipo específico", y a partir de ahí todo
  el autocompletado sabe qué campos existen.

No se usan `props` propias en los componentes de este proyecto (`ProductosTable` y
`FacturasTable` no reciben ningún parámetro por ahora, se autoabastecen con `useQuery`) —
si más adelante necesitas pasarle datos de un componente padre a un hijo, ese es el
mecanismo de "props" que verás en cualquier proyecto React real, pero no aparece aquí
todavía.
