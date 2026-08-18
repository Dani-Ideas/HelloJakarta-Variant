# shadcn/ui — instalación manual, paso a paso

Todo el código y las versiones de este documento se verificaron directo contra el
repositorio oficial (`github.com/shadcn-ui/ui`) y la documentación oficial
(`ui.shadcn.com`) — no es de memoria.

## Lo primero que hay que entender: shadcn/ui **no es una librería normal**

Con una librería típica (`npm install algo`), instalas un paquete "caja negra" — nunca ves
ni tocas su código fuente. **shadcn/ui funciona distinto a propósito**: el CLI (o tú, a
mano) **copia el código fuente del componente directo a tu proyecto** (a
`src/components/ui/`). Una vez copiado, es tuyo — lo puedes editar como cualquier otro
archivo tuyo. Por eso "instalar manualmente" no es raro ni un caso especial mal soportado
— es, literalmente, lo que hace el CLI por debajo, solo que tú copias el texto a mano en
vez de que un comando lo haga.

Por eso este documento no es "cómo hackear una instalación rota" — es el proceso real y
documentado, solo que paso a paso.

---

## Parte 1 — Requisitos base (una sola vez por proyecto)

### 1.1 Tailwind CSS (v4)

shadcn/ui depende de Tailwind CSS para todos sus estilos. Con Vite:

```bash
npm install tailwindcss @tailwindcss/vite
```

En `src/index.css`, reemplaza todo el contenido por:

```css
@import "tailwindcss";
```

En `vite.config.ts`, agrega el plugin de Tailwind:

```ts
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  // ... tu configuracion existente
});
```

### 1.2 El alias `@/` (para poder escribir `import { Button } from "@/components/ui/button"`)

En `tsconfig.json` (y en `tsconfig.app.json` si tu proyecto lo separa así, como el
generado por Vite):

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

En `vite.config.ts`, hace falta que Vite entienda ese mismo alias en tiempo de build
(instala primero `npm install -D @types/node`):

```ts
import path from "path";

export default defineConfig({
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
```

### 1.3 Dependencias npm base (una sola vez, sirven para todos los componentes)

```bash
npm install class-variance-authority clsx tailwind-merge lucide-react radix-ui
```

- `clsx` + `tailwind-merge` → para el helper `cn()` (junta clases CSS condicionales sin
  que se pisen entre sí).
- `class-variance-authority` (CVA) → maneja "variantes" de un componente (ej. botón
  `default`/`outline`/`ghost`) sin `if`s manuales.
- `lucide-react` → los íconos que usan varios componentes de ejemplo.
- `radix-ui` → **este es el dato que más cambia entre tutoriales viejos y el actual**: antes
  se instalaba un paquete separado por cada primitivo (`@radix-ui/react-dialog`,
  `@radix-ui/react-slot`, etc.) — ahora todo viene unificado en un solo paquete `radix-ui`.
  Si un tutorial viejo te dice que instales `@radix-ui/react-algo`, ya no es necesario,
  basta con `radix-ui` a secas.

### 1.4 `src/lib/utils.ts` — créalo tú mismo con este contenido exacto

```ts
import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

Este archivo lo usa **todo** componente de shadcn — es el que junta tus clases propias con
las de la variante que elijas, sin conflictos.

### 1.5 Variables de tema (colores) — pégalas en tu CSS principal

Esto va **antes** o **después** de tus propios estilos en `src/index.css` (no reemplaza lo
que ya tengas, se suma):

```css
@theme inline {
  --color-background: var(--background);
  --color-foreground: var(--foreground);
  --color-card: var(--card);
  --color-card-foreground: var(--card-foreground);
  --color-primary: var(--primary);
  --color-primary-foreground: var(--primary-foreground);
  --color-secondary: var(--secondary);
  --color-secondary-foreground: var(--secondary-foreground);
  --color-muted: var(--muted);
  --color-muted-foreground: var(--muted-foreground);
  --color-accent: var(--accent);
  --color-accent-foreground: var(--accent-foreground);
  --color-destructive: var(--destructive);
  --color-destructive-foreground: var(--destructive-foreground);
  --color-border: var(--border);
  --color-input: var(--input);
  --color-ring: var(--ring);
  --radius-md: calc(var(--radius) * 0.8);
  --radius-lg: var(--radius);
}

:root {
  --radius: 0.625rem;
  --background: oklch(1 0 0);
  --foreground: oklch(0% 0 0);
  --card: oklch(1 0 0);
  --card-foreground: oklch(0% 0 0);
  --primary: oklch(0% 0 0);
  --primary-foreground: oklch(0.985 0 0);
  --secondary: oklch(0.97 0 0);
  --secondary-foreground: oklch(0.205 0 0);
  --muted: oklch(0.97 0 0);
  --muted-foreground: oklch(0.556 0 0);
  --accent: oklch(0.97 0 0);
  --accent-foreground: oklch(0.205 0 0);
  --destructive: oklch(0.577 0.245 27.325);
  --destructive-foreground: oklch(0.97 0.01 17);
  --border: oklch(0.922 0 0);
  --input: oklch(0.922 0 0);
  --ring: oklch(0.708 0 0);
}

.dark {
  --background: oklch(0.145 0 0);
  --foreground: oklch(0.985 0 0);
  --card: oklch(0.205 0 0);
  --card-foreground: oklch(0.985 0 0);
  --primary: oklch(0.922 0 0);
  --primary-foreground: oklch(0.205 0 0);
  --secondary: oklch(0.269 0 0);
  --secondary-foreground: oklch(0.985 0 0);
  --muted: oklch(0.269 0 0);
  --muted-foreground: oklch(0.708 0 0);
  --accent: oklch(0.371 0 0);
  --accent-foreground: oklch(0.985 0 0);
  --destructive: oklch(0.704 0.191 22.216);
  --destructive-foreground: oklch(0.58 0.22 27);
  --border: oklch(1 0 0 / 10%);
  --input: oklch(1 0 0 / 15%);
  --ring: oklch(0.556 0 0);
}
```

(Recorté las variables de `sidebar-*` y `chart-*` porque solo hacen falta si usas esos
componentes específicos — agrégalas después, si llegas a necesitarlas, copiándolas de
`ui.shadcn.com/docs/installation/manual`.)

`oklch(...)` es solo un formato de color (como `rgb()` o `hsl()`, pero con mejor percepción
de contraste) — no necesitas entenderlo a fondo para usarlo, el navegador ya lo soporta.

---

## Parte 2 — Cómo instalar UN componente a mano (el patrón que se repite siempre)

Para cualquier componente, el patrón es idéntico:

1. Ve a `https://ui.shadcn.com/docs/components/<nombre>` (ej. `.../components/button`).
2. Busca la pestaña **"Manual"** en el bloque de instalación (junto a la de "CLI") — ahí
   vienen listadas las dependencias específicas de *ese* componente (no todos necesitan lo
   mismo).
3. Instala esas dependencias puntuales con `npm install`.
4. Copia el código fuente completo del componente a
   `src/components/ui/<nombre>.tsx`.
5. Impórtalo donde lo necesites: `import { Button } from "@/components/ui/button"`.

Si la pestaña "Manual" no carga bien (nos pasó a nosotros investigando esto — la página es
una SPA y a veces el contenido de las pestañas no se renderiza bien fuera del navegador),
el código fuente real y siempre actualizado está directo en GitHub:

```
https://github.com/shadcn-ui/ui/tree/main/apps/v4/registry/new-york-v4/ui/
```

Cada archivo `.tsx` ahí es exactamente lo que copiarías a mano.

---

## Parte 3 — Dos ejemplos reales completos

### Ejemplo 1 — `Input` (el caso más simple: cero dependencias extra)

No necesita nada además de la Parte 1 — ni CVA, ni Radix. Crea
`src/components/ui/input.tsx`:

```tsx
import * as React from "react"

import { cn } from "@/lib/utils"

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-xs transition-[color,box-shadow] outline-none selection:bg-primary selection:text-primary-foreground file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-muted-foreground disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm dark:bg-input/30",
        "focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50",
        "aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40",
        className
      )}
      {...props}
    />
  )
}

export { Input }
```

Uso: `<Input placeholder="Nombre del producto" />`.

### Ejemplo 2 — `Button` (más completo: usa CVA + Radix)

Dependencias extra para este componente puntual (ya deberías tenerlas de la Parte 1, pero
por si instalaste solo lo mínimo):

```bash
npm install class-variance-authority radix-ui
```

Crea `src/components/ui/button.tsx`:

```tsx
import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { Slot } from "radix-ui"

import { cn } from "@/lib/utils"

const buttonVariants = cva(
  "inline-flex shrink-0 items-center justify-center gap-2 rounded-md text-sm font-medium whitespace-nowrap transition-all outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground hover:bg-primary/90",
        destructive:
          "bg-destructive text-white hover:bg-destructive/90 focus-visible:ring-destructive/20 dark:bg-destructive/60 dark:focus-visible:ring-destructive/40",
        outline:
          "border bg-background shadow-xs hover:bg-accent hover:text-accent-foreground dark:border-input dark:bg-input/30 dark:hover:bg-input/50",
        secondary:
          "bg-secondary text-secondary-foreground hover:bg-secondary/80",
        ghost:
          "hover:bg-accent hover:text-accent-foreground dark:hover:bg-accent/50",
        link: "text-primary underline-offset-4 hover:underline",
      },
      size: {
        default: "h-9 px-4 py-2 has-[>svg]:px-3",
        sm: "h-8 gap-1.5 rounded-md px-3 has-[>svg]:px-2.5",
        lg: "h-10 rounded-md px-6 has-[>svg]:px-4",
        icon: "size-9",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

function Button({
  className,
  variant = "default",
  size = "default",
  asChild = false,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean
  }) {
  const Comp = asChild ? Slot.Root : "button"

  return (
    <Comp
      data-slot="button"
      data-variant={variant}
      data-size={size}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { Button, buttonVariants }
```

Uso:
```tsx
<Button>Guardar</Button>
<Button variant="outline">Cancelar</Button>
<Button variant="destructive" size="sm">Eliminar</Button>
```

**Cómo leer este archivo para entenderlo, no solo copiarlo**: `cva(...)` define, en un solo
lugar, todas las combinaciones posibles de `variant` + `size` y qué clases CSS le
corresponden a cada una — en vez de escribir `if (variant === "outline") { ... }` a mano.
`asChild` es un patrón de Radix: si es `true`, el componente no renderiza su propio
`<button>`, sino que le "presta" sus estilos al elemento hijo que le pongas (útil para,
por ejemplo, que un `<Link>` de TanStack Router se vea como botón sin anidar
`<button><a>...</a></button>`, que sería HTML inválido).

---

## Enlaces directos

- Instalación manual completa (todas las variables de tema, sin recortar): `https://ui.shadcn.com/docs/installation/manual`
- Guía específica para Vite: `https://ui.shadcn.com/docs/installation/vite`
- Catálogo de todos los componentes: `https://ui.shadcn.com/docs/components`
- Código fuente real de cada componente (más confiable que la pestaña "Manual" si no carga): `https://github.com/shadcn-ui/ui/tree/main/apps/v4/registry/new-york-v4/ui`
