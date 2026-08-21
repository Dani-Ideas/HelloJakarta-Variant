# React — solo lo que se usa en este proyecto

Esta guía asume que **no sabes nada de React**. Cubre únicamente los conceptos que
aparecen en el código de `frontend/src/`, en el orden en que los vas a encontrar leyendo
los archivos.

---

## 1. Cómo bajar y correr este frontend en tu máquina

### 1.1 Requisitos

- **Node.js** — el build de producción (vía Maven, ver `frontend.md` sección 2, Pieza 3)
  fija **v20.20.2** y se instala solo, así que no depende de lo que tengas en el sistema.
  Para desarrollo local (`npm run dev`) basta con tener instalada esa misma versión o
  cualquier v20 reciente.
- **npm** (viene con Node).
- **GlassFish corriendo en el 8080** — solo si vas a probar Productos/Facturas (que sí
  hablan con la API real). Las páginas del "Menú de pago" (`/salir-sitio`,
  `/formulario-pago`, `/formulario-largo`) son 100% visuales, no llaman a ningún backend,
  así que funcionan sin GlassFish.

### 1.2 Si sí puedes hacer `git clone` normal

```bash
git clone git@github.com:Dani-Ideas/HelloJakarta-Variant.git
cd HelloJakarta-Variant/frontend
npm install
```

Sigue con la sección 1.3. Si en tu red esto **no funciona** (bloqueado el protocolo git/SSH,
o `github.com` entero), ve a la sección 1.2-bis.

### 1.2-bis Si NO puedes clonar el repo (red restringida en el trabajo)

La idea es siempre la misma: **el código ya existe completo en una máquina que sí tiene
acceso** (esta) — solo hace falta llevarlo a la otra, no "descargarlo de internet" desde
ahí. Tres formas, de más simple a más completa:

**Opción A — copiar solo la carpeta `frontend/` (alcanza para trabajar en el front).**
Desde esta máquina, comprime la carpeta *sin* lo generado (no hace falta llevarlo, y pesa
mucho):

```bash
cd /home/robute/IdeaProjects/HelloJakarta-variante
tar --exclude=node_modules --exclude=dist -czf frontend.tar.gz frontend
```

Lleva ese `.tar.gz` (o un `.zip` normal, si prefieres el explorador de archivos) a la
máquina del trabajo por el medio que sí tengas permitido ahí — USB, un share de red
interno, correo, un repositorio interno tipo Artifactory/Nexus, etc. Descomprímelo y sigue
con la sección 1.3 (`npm install`).

**Opción B — descargar el ZIP del repo completo desde el navegador.** Muchas redes
corporativas bloquean el protocolo `git`/SSH pero sí dejan navegar a `github.com` por HTTPS
normal (puerto 443, como cualquier página web). Si ese es tu caso: entra al repo en el
navegador → botón verde **"Code"** → **"Download ZIP"**. Baja todo el repo (`back/` +
`frontend/`) sin usar el comando `git` para nada.

**Opción C — `git bundle` (si además quieres el historial de commits, no solo los
archivos).** Desde esta máquina:

```bash
git bundle create HelloJakarta-Variant.bundle --all
```

Ese `.bundle` es un solo archivo (cópialo por USB/red interna/lo que uses) que se comporta
como un repositorio remoto completo, con todo el historial de commits. En la máquina
restringida, esto **sí es un `git clone` normal** — pero contra el archivo local, sin tocar
la red:

```bash
git clone HelloJakarta-Variant.bundle HelloJakarta-Variant
```

Útil si en el trabajo quieres seguir usando git como siempre (branches, `git log`, etc.)
contra una copia real del historial, no solo una carpeta suelta.

Con cualquiera de las tres, una vez que la carpeta `frontend/` está en la máquina destino,
sigue exactamente igual desde acá abajo — `npm install` solo necesita llegar al registry de
npm (que ya confirmaste que sí funciona en tu trabajo), no a GitHub ni a `ui.shadcn.com`.

```bash
cd frontend
npm install
```

`npm install` descarga a `node_modules/` (no se sube a git) **todo** lo que hace falta:
React, TanStack (Router/Query/Table), Tailwind CSS v4, y las dependencias de shadcn/ui
(`radix-ui`, `class-variance-authority`, `clsx`, `tailwind-merge`, `lucide-react`) —
paquetes normales del registry público de npm, nada especial.

**Nota importante sobre shadcn/ui**: los componentes que se ven en `src/components/ui/`
(`button.tsx`, `card.tsx`, `carousel.tsx`, `field.tsx`, `attachment.tsx`, etc.) **no se
descargan con `npm install`** — son código fuente propio, ya copiado y commiteado en el
repo (así es como funciona shadcn/ui: el CLI copia el componente a tu proyecto una sola
vez, y a partir de ahí es tuyo, ver `shadcn-instalacion-manual.md`). Por eso `npm install`
alcanza para que compile todo, aunque tu red no tenga acceso a `ui.shadcn.com` — ese acceso
solo hace falta si quieres **agregar un componente nuevo** que todavía no existe en el
repo (con `npx shadcn@latest add <componente>`, o a mano siguiendo
`shadcn-instalacion-manual.md` si esa red está bloqueada).

### 1.3 Levantar el servidor de desarrollo

```bash
npm run dev
```

Abre `http://localhost:5173/HelloJakarta-variante/` (el `/HelloJakarta-variante/` al final
no es opcional — ver `base` en `vite.config.ts`, explicado en `frontend.md`). Cambios en
cualquier archivo de `src/` se reflejan al instante (Hot Module Replacement), sin perder el
estado de la app.

### 1.4 Otros scripts (`package.json`)

```bash
npm run build     # compila para produccion -- ESCRIBE DIRECTO en ../back/src/main/webapp
                   # (lo pisa completo). Normalmente no se corre a mano, ver frontend.md sec. 6.
npm run preview    # sirve localmente el resultado de "build", para probarlo sin GlassFish
npm run lint       # oxlint
```

Para el flujo real de build + deploy contra GlassFish, ver `frontend.md` (secciones 6 y 7).

---

## 2. Qué es React, en una frase

Una librería de JavaScript para construir interfaces como un árbol de **componentes** —
piezas reutilizables de UI, cada una responsable de una parte de la pantalla, que se
combinan entre sí. En este proyecto, qué se ve en pantalla depende de la URL: `RootLayout`
(el marco fijo, header + nav) siempre está, y adentro se inserta una página distinta —
`HomePage`, `ProductosPage` (que a su vez usa `ProductosPanel`, `ProductosTable`,
`ProductoForm`), `FacturasPage`, o alguna de las páginas del menú de pago. Quién decide cuál
página según la URL es TanStack Router (ver `tanstack.md`, Parte 3) — pero cada una de esas
"páginas" sigue siendo, por dentro, un componente de React normal.

## 3. JSX — por qué hay HTML dentro del código Java... digo, JavaScript

Abre `routes/RootLayout.tsx`:

```tsx
return (
  <div className="pagina">
    <header className="encabezado">
      <h1>HelloJakarta</h1>
    </header>
    <main>
      <Outlet />
    </main>
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

## 4. Componentes funcionales

```tsx
export function SalirSitioPage() {
  return ( /* JSX */ );
}
```

Un componente en este proyecto es **una función de TypeScript que retorna JSX**. Así de
simple — no hay clases, no hay herencia. Todos los archivos de `routes/` (`HomePage`,
`ProductosPage`, `FacturasPage`, y las tres páginas del menú de pago:
`pago/SalirSitioPage.tsx`, `pago/FormularioPagoPage.tsx`, `pago/FormularioLargoPage.tsx`) y
de `components/` (`ProductosTable`, `ProductoForm`, `FacturasTable`) siguen el mismo patrón:
`export function NombreDelComponente() { ... }`.

## 5. Cómo se arma el árbol y cómo arranca

`main.tsx` es el punto de entrada real:

```tsx
createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
);
```

- `document.getElementById("root")` → el `<div id="root">` que está en `index.html`. Ahí es
  donde React "inyecta" toda la aplicación dentro del HTML real del navegador.
- `createRoot(...).render(...)` → le dice a React "dibuja esto (y todo lo que contenga)
  adentro de ese div".
- `<StrictMode>` → no renderiza nada visible, es un modo de desarrollo que ayuda a detectar
  errores comunes (a veces hace que un componente se ejecute dos veces a propósito, solo en
  desarrollo, para exponer efectos secundarios mal escritos).
- `<QueryClientProvider>` → viene de TanStack Query, ver `tanstack.md`. Envuelve toda la app
  para que cualquier componente adentro pueda usar `useQuery`.
- `<RouterProvider router={router}>` → viene de TanStack Router (ver `tanstack.md`, Parte
  3). Reemplaza lo que antes era un único `<App />` fijo — ahora quien decide qué
  componente mostrar es el router, leyendo la URL actual contra el árbol de rutas de
  `router.tsx`. `App.tsx` ya no existe: su función (armar el layout general) la reparten
  ahora `RootLayout.tsx` (el marco fijo) y cada página individual en `routes/`.

El árbol completo de este proyecto, hoy:

```
main.tsx
 └─ QueryClientProvider
     └─ RouterProvider                (decide, segun la URL, que pintar -- tanstack.md Parte 3)
         └─ RootLayout                  (header + nav, SIEMPRE visible)
             └─ <Outlet/>                (solo esto cambia al navegar)
                 ├─ HomePage                    "/"
                 ├─ ProductosPage               "/productos"
                 │    └─ ProductosPanel
                 │         ├─ ProductoForm         (si estas creando/editando)
                 │         └─ ProductosTable
                 ├─ FacturasPage                "/facturas"
                 │    └─ FacturasTable
                 ├─ SalirSitioPage              "/salir-sitio"
                 ├─ FormularioPagoPage          "/formulario-pago"
                 └─ FormularioLargoPage         "/formulario-largo"
```

## 6. Hooks — específicamente `useState` (usado en `FacturasTable.tsx`, y a fondo en
`FormularioPagoPage.tsx`)

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

El ejemplo con más `useState` juntos en este proyecto es `FormularioPagoPage.tsx` (el
carrusel de la Opción 2 del menú de pago): un estado por cada dato del formulario
(`preferencia`, `categoria`, `nombre`), uno para el estado del carrusel (`current`,
`maxStep`), y uno para la simulación de pago (`pagoEstado`). Cada `set...` dispara un
re-render que decide qué paso mostrar y si se puede avanzar o no.

Regla que no se rompe en ningún lado del código: los hooks **siempre van al nivel superior**
del componente, nunca dentro de un `if` o un `.map()`.

## 7. Renderizar listas: `.map()` y la `key`

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

El mismo patrón se repite en `HomePage.tsx` (`GAJOS.map((gajo) => <Link key={gajo.to}...)`,
armando los 3 gajos del "pay" del menú) y en `FormularioPagoPage.tsx`
(`PASOS.map((titulo, indice) => ...)`, armando los botones del stepper).

## 8. Renderizado condicional

```tsx
if (isLoading) return <p className="estado">Cargando productos...</p>;
if (isError) return <p className="estado estado-error">Error: {(error as Error).message}</p>;

return <table>...</table>;
```

No hay un `<if>` en JSX — simplemente usas JavaScript normal *antes* del `return` final
(como aquí), o expresiones cortas dentro del JSX. Dos formas se repiten en el código:

- **`condicion && <algo/>`** (`FacturasTable.tsx`: `{row.getIsExpanded() && (<tr>...</tr>)}`,
  o `FormularioPagoPage.tsx`: `{pagoEstado === "procesando" && (<><Spinner/> Procesando...</>)}`)
  → "esto, o nada". Si la condición es `false`, esa expresión entera es `false` y React no
  renderiza nada ahí.
- **`condicion ? <A/> : <B/>`** (ternario — `FormularioPagoPage.tsx`:
  `{enviado ? (<Alert>...</Alert>) : (<Button onClick={...}>Enviar</Button>)}`) → "esto, o lo
  otro" — a diferencia de `&&`, siempre se muestra algo, solo cambia cuál de las dos ramas.

## 9. `Fragment` (`<>...</>` o `<Fragment key={...}>`)

Un componente/expresión JSX debe retornar **un solo elemento raíz**. Pero en
`FacturasTable.tsx` necesitábamos retornar dos `<tr>` hermanos (la fila principal + la fila
de detalle expandida) sin envolverlos en un `<div>` extra (que rompería la tabla HTML,
porque un `<div>` no es válido directo dentro de un `<tbody>`). `Fragment` es un envoltorio
"invisible" — agrupa elementos sin agregar ningún nodo real al HTML final. Se usó la forma
`<Fragment key={row.id}>` en vez del atajo `<>` porque, al estar dentro de un `.map()`,
necesita poder llevar la prop `key`.

## 10. TypeScript en React

Cada archivo de componente termina en `.tsx` (no `.jsx`) porque usa TypeScript. Lo que se
usa en este proyecto:
- **Interfaces** para los datos que vienen del backend (`api/types.ts`) — `ProductoDTO`,
  `FacturaDTO`, `FacturaDetalleDTO`, calcadas de los DTO de Java para que el editor te
  avise si usas un campo que no existe.
- **Genéricos** al usar TanStack (`createColumnHelper<ProductoDTO>()`) — le dicen a la
  librería "esta tabla trabaja con filas de este tipo específico", y a partir de ahí todo
  el autocompletado sabe qué campos existen.

### Props

Sí se usan **props** en este proyecto — el ejemplo más claro es `ProductoForm.tsx`:

```tsx
interface ProductoFormProps {
  productoInicial?: ProductoDTO;
  onGuardar: (datos: ProductoInput) => void;
  onCancelar: () => void;
  guardando: boolean;
}

export function ProductoForm({ productoInicial, onGuardar, onCancelar, guardando }: ProductoFormProps) {
```

- `productoInicial?` (con `?`) → prop **opcional**: si no se pasa, es `undefined`. Así el
  mismo componente sirve tanto para "crear" como para "editar" — si viene un producto
  precarga los campos con `useState(productoInicial?.nombre ?? "")`; si no, empieza vacío.
- `onGuardar` / `onCancelar` → **callbacks**: funciones que el padre (`ProductosPanel.tsx`)
  le pasa al hijo, para que el hijo le "avise" que algo pasó (guardar, cancelar) sin saber
  nada de mutaciones ni de la API — esa lógica se queda en el padre (ver `tanstack.md`).
  `ProductosTable.tsx` sigue el mismo patrón (`productos`, `onEditar`, `onEliminar`).

En cambio, ninguna de las **páginas** de `routes/` recibe props: cada una se autoabastece,
ya sea con `useQuery` (`ProductosPanel`, `FacturasTable`) o con su propio `useState` interno
(`HomePage`, `SalirSitioPage`, `FormularioPagoPage`, `FormularioLargoPage` — estas últimas no
hablan con ningún backend, son simulaciones puramente visuales).

---

## Los componentes de `@/components/ui/*` (shadcn/ui) no son magia

Todo lo que se importa desde `@/components/ui/...` (`Button`, `Card`, `Carousel`, `Field`,
`Tabs`, etc., usados sobre todo en `HomePage.tsx` y en las tres páginas de
`routes/pago/`) son **componentes de React normales** — funciones que retornan JSX y reciben
props, exactamente las reglas de este documento. Lo único "especial" es de dónde salió ese
código (copiado a mano/CLI desde shadcn/ui, en vez de escrito por nosotros desde cero) y que
usan Tailwind CSS para el estilo en vez del CSS a mano de `index.css`. El detalle de cómo se
instalaron y cómo agregar uno nuevo está en `shadcn-instalacion-manual.md`; el árbol de
rutas completo (dónde vive cada página) está en `tanstack.md`, Parte 3.
