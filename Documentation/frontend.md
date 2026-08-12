# Frontend — HelloJakarta

Carpeta: `frontend/`. Stack: **Vite + React + TypeScript + TanStack Query + TanStack
Table**. Consume la API REST del backend Jakarta EE (`GET /api/productos`,
`GET /api/facturas`).

---

## 1. Quién sirve qué archivo — GlassFish vs. Vite (léelo primero, es la pregunta clave)

Esto es importante porque hasta ahora todo lo que hiciste corría **dentro** de GlassFish.
El frontend **no**. Son dos mundos separados y hay que tener clarísima la frontera.

### Cómo decide GlassFish qué entregar (repaso, esto ya lo tenías con el backend)

Cuando despliegas el `.war`, GlassFish descomprime el archivo y lee su contenido para
decidir, para cada URL entrante, una de dos cosas:

1. **¿Es un archivo estático?** Si la URL coincide con algo que existe tal cual dentro de
   `src/main/webapp/` (una imagen, un `.html` puesto ahí a mano), GlassFish **lo entrega
   directo, sin tocarlo** — igual que cualquier servidor de archivos.
2. **¿Es una URL que debe ejecutar código?** Si la URL coincide con un `@Path` de una clase
   anotada `@Provider`/recurso JAX-RS (o un Servlet, o un JSF), GlassFish **no entrega un
   archivo — ejecuta tu código Java**, y lo que responde es lo que ese código construya en
   memoria (en tu caso, JSON armado por Jersey + JSON-B). Esto es "interpretar": el
   contenido no existía como archivo antes de la petición, se genera al vuelo.

La forma en que GlassFish sabe cuál de las dos rutas tomar es leyendo, al momento del
deploy, las anotaciones de tus clases (`@ApplicationPath`, `@Path`) y registrando esas
rutas en su tabla interna de despachador de URLs (el mismo mecanismo que un `web.xml`
manual, solo que vía anotaciones en vez de XML).

### Y el frontend, ¿quién lo entrega ahora mismo?

**GlassFish no sabe que el frontend existe.** No está desplegado ahí, no está en ningún
`.war`, GlassFish jamás lee ni un solo archivo de la carpeta `frontend/`. Lo que está
sirviendo esos archivos en `http://localhost:5173` es **Vite**, un proceso de Node.js
completamente aparte, corriendo con `npm run dev`. Son dos servidores, dos puertos, dos
procesos — el navegador es el único que "junta" ambos mundos, haciendo peticiones a los dos
(a 5173 para la interfaz, a 8080 para los datos).

```
Navegador
   │
   ├──► http://localhost:5173  → proceso Vite (Node.js) → sirve React/TS/CSS
   │
   └──► http://localhost:8080/HelloJakarta/api/...  → proceso GlassFish (Java) → JSON
```

Por eso hizo falta el `CorsFilter` que agregamos al backend: el navegador ve dos "orígenes"
distintos (mismo host, distinto puerto = distinto origen para las reglas de seguridad del
navegador) y por default bloquearía que el JavaScript servido desde 5173 lea una respuesta
que vino de 8080, a menos que el servidor de 8080 diga explícitamente "está permitido".

### En producción, ¿tiene que seguir siendo así?

No, hay tres caminos válidos (no implementados aquí, solo para que sepas que existen):

1. **Seguir separados** (lo más común en proyectos reales): el frontend compilado
   (`npm run build`) se sube a un hosting de archivos estáticos cualquiera (Nginx, Vercel,
   un bucket S3, etc.) y el backend sigue siendo GlassFish, aparte. Requiere CORS siempre,
   como ahora.
2. **Meter el frontend compilado dentro del mismo WAR**: copiar el contenido de
   `frontend/dist/` a `src/main/webapp/` del proyecto Java, y desplegar todo junto. Ahí
   GlassFish sí serviría el frontend, como archivos estáticos (opción 1 de la sección
   anterior) — y ya no haría falta CORS, porque todo vendría del mismo origen.
3. Un servidor intermedio (reverse proxy) que junte ambos bajo un solo dominio.

---

## 2. Estructura del proyecto

```
frontend/
├── index.html          punto de entrada real (lo primero que carga el navegador)
├── package.json         dependencias + scripts (npm run dev/build/preview)
├── vite.config.ts       configuracion de Vite
├── tsconfig*.json       configuracion de TypeScript
└── src/
    ├── main.tsx          arranca React, monta <App /> en el DOM
    ├── App.tsx           componente raiz, arma la pagina
    ├── index.css         estilos (minimalistas, sin libreria de CSS)
    ├── react-table.d.ts  extension de tipos para TanStack Table
    ├── api/
    │   ├── types.ts       interfaces TS que reflejan los DTO de Java
    │   └── client.ts       funciones fetch() hacia el backend
    └── components/
        ├── ProductosTable.tsx
        └── FacturasTable.tsx
```

Ver `react.md` para entender `main.tsx`/`App.tsx`/los componentes, y `tanstack.md` para
`client.ts` + la lógica de las tablas.

---

## 3. Instalación

```bash
cd frontend
npm install
```

Qué hace: lee `package.json`, descarga todas las dependencias (React, Vite, TanStack, etc.)
a la carpeta `node_modules/` (no se sube a git, se regenera con este comando), y fija las
versiones exactas instaladas en `package-lock.json`.

## 4. Correr en desarrollo

```bash
npm run dev
```

Levanta Vite en `http://localhost:5173`. Tiene **Hot Module Replacement (HMR)**: cuando
guardas un archivo `.tsx`, el navegador actualiza solo esa parte de la página al instante,
sin recargar todo ni perder el estado de la app. Este modo **nunca se usa en producción** —
sirve los archivos sin minificar y con herramientas de debug activas.

## 5. Compilar para producción

```bash
npm run build
```

Corre dos cosas en secuencia (mira el script en `package.json`):
1. `tsc -b` → TypeScript revisa todo el código en busca de errores de tipos, sin generar
   nada todavía (es un chequeo). Si hay un error de tipos, el build se detiene aquí.
2. `vite build` → empaqueta todo (React, tus componentes, las librerías) en unos pocos
   archivos `.js`/`.css` minificados y optimizados, dentro de `dist/`.

`dist/` es el resultado final: **puro HTML/CSS/JS estático**, sin Node.js, sin build tools,
sin nada — cualquier servidor web del planeta lo puede servir tal cual.

## 6. Previsualizar el build de producción

```bash
npm run preview
```

Levanta un servidor local simple sirviendo lo que quedó en `dist/` — para confirmar que el
build de producción funciona antes de desplegarlo de verdad (distinto del `npm run dev`,
que sirve el código fuente sin compilar).

## 7. Desplegar (según el camino que elijas de la sección 1)

- **Estático aparte**: copiar el contenido de `dist/` a donde sea que sirva archivos
  estáticos (Nginx, Vercel, Netlify, un bucket S3 con hosting estático, etc.).
- **Dentro del WAR de GlassFish**: copiar `dist/*` a `src/main/webapp/` del proyecto Java,
  volver a `mvn package`, y desplegar el WAR normal — GlassFish serviría el frontend como
  archivos estáticos desde ahí.
