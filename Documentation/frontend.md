# Frontend — HelloJakarta-variante

Carpeta: `frontend/`. Stack: **Vite + React + TypeScript + TanStack Query + TanStack
Table**. Consume la API REST del backend Jakarta EE (`GET /api/productos`,
`GET /api/facturas`, `POST`/`PUT`/`DELETE` de productos).

**Esta variante es monolítica**: el frontend compilado vive *dentro* del mismo `.war` que
el backend. Un solo artefacto, un solo `asadmin deploy`, un solo puerto. Este documento
explica cómo quedó armado ese flujo.

---

## 1. Quién sirve qué archivo — GlassFish vs. Vite (léelo primero, es la pregunta clave)

Hay que distinguir **dos momentos distintos** en la vida de este proyecto: cuando estás
desarrollando (`npm run dev`) y cuando ya está desplegado (`mvn package` + `asadmin
deploy`). Se comportan distinto a propósito.

### Cómo decide GlassFish qué entregar (repaso)

Cuando despliegas el `.war`, GlassFish lo descomprime y para cada URL entrante decide una
de dos cosas:

1. **¿Es un archivo estático?** Si la URL coincide con algo que existe tal cual dentro de
   `src/main/webapp/` (un `.html`, un `.js`, un `.css`), GlassFish **lo entrega directo, sin
   tocarlo** — como cualquier servidor de archivos.
2. **¿Es una URL que debe ejecutar código?** Si coincide con un `@Path` de un recurso
   JAX-RS, GlassFish **ejecuta tu código Java** y responde con lo que ese código construya
   en memoria (JSON armado por Jersey + JSON-B). Esto es "interpretar": el contenido no
   existía como archivo antes de la petición.

La forma en que GlassFish sabe cuál ruta tomar es leyendo, al momento del deploy, las
anotaciones de tus clases (`@ApplicationPath`, `@Path`) y registrando esas rutas en su
tabla interna de despachador de URLs.

### En esta variante, el frontend SÍ es "archivo estático" para GlassFish

A diferencia de la versión original (donde el frontend vivía aparte, servido por Vite en el
puerto 5173), aquí el build de producción de React **cae directo dentro de
`src/main/webapp/`** — así que, una vez desplegado, GlassFish trata `index.html` y los
`.js`/`.css` generados exactamente igual que trataría cualquier imagen puesta ahí a mano:
los entrega tal cual, sin ejecutar nada. Solo las URLs bajo `/api/...` disparan código Java.

```
Navegador → http://localhost:8080/HelloJakarta-variante/
                │
                ├─ /              → GlassFish entrega index.html (archivo estatico, del WAR)
                ├─ /assets/*.js    → GlassFish entrega el JS (archivo estatico, del WAR)
                └─ /api/productos  → GlassFish ejecuta ProductoResource (Java, JAX-RS)
```

**Un solo proceso, un solo puerto, un solo origen.** Por eso ya no hace falta CORS en
producción (aunque se dejó el `CorsFilter` en el backend porque sigue siendo útil en modo
desarrollo, ver abajo).

### Y durante el desarrollo (`npm run dev`), ¿sigue siendo monolítico?

No, y **está bien que no lo sea** — durante desarrollo seguimos usando dos procesos
separados (Vite en 5173 + GlassFish en 8080), porque Vite te da Hot Module Replacement
(cambios instantáneos sin perder el estado de la app), algo que no tiene sentido replicar
dentro de GlassFish. Lo que cambia frente a la versión original es que ahora Vite
**reenvía** las llamadas a la API en vez de que el navegador le pegue directo a GlassFish
— ver la sección de proxy más abajo. El monolito es una propiedad del **artefacto
desplegado**, no del flujo de desarrollo.

---

## 2. Cómo quedó armado (las 3 piezas que lo conectan)

### Pieza 1 — `vite.config.ts`

```ts
export default defineConfig({
  plugins: [react()],
  base: "/HelloJakarta-variante/",
  build: {
    outDir: "../src/main/webapp",
    emptyOutDir: true,
  },
  server: {
    proxy: {
      "/HelloJakarta-variante/api": "http://localhost:8080",
    },
  },
});
```

- **`build.outDir: "../src/main/webapp"`** → `npm run build` ya no escribe en
  `frontend/dist/`, escribe **directo dentro del proyecto Java**, en la carpeta que
  `maven-war-plugin` empaqueta automáticamente en el WAR. `emptyOutDir: true` limpia esa
  carpeta antes de cada build (para no dejar archivos viejos con hashes distintos).
- **`base: "/HelloJakarta-variante/"`** → le dice a Vite bajo qué ruta va a vivir la app
  una vez desplegada, para que genere `<script src="/HelloJakarta-variante/assets/...">` en
  vez de `<script src="/assets/...">`. **Esto se descubrió como bug real**: sin el `base`
  correcto, el `index.html` cargaba bien (GlassFish sí lo encontraba), pero el JS/CSS/
  favicon apuntaban a la raíz del dominio en vez del context root del WAR, y el navegador
  los pedía en la URL equivocada (`404`). Tiene que coincidir siempre con el `finalName`
  del `pom.xml` (que define el context root del WAR).
- **`server.proxy`** → solo aplica en `npm run dev`. Cuando el código pide
  `/HelloJakarta-variante/api/productos`, Vite intercepta esa ruta y la reenvía él mismo
  (server-side, en Node) hacia `http://localhost:8080`, así el navegador nunca ve un origen
  distinto — de ahí que ya no dependamos de CORS ni siquiera en desarrollo.

### Pieza 2 — `api/client.ts`

```ts
const API_BASE = "/HelloJakarta-variante/api";
```

Ruta **relativa**, ya no una URL absoluta con `http://localhost:8080` hardcodeado. Funciona
en ambos escenarios sin cambiar una línea: en `npm run dev` la resuelve el proxy de arriba;
en producción, como el frontend vive en el mismo origen que la API, una ruta relativa
resuelve sola al lugar correcto.

### Pieza 3 — `pom.xml` (el `frontend-maven-plugin`)

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>2.0.1</version>
    <configuration>
        <workingDirectory>frontend</workingDirectory>
    </configuration>
    <executions>
        <execution>
            <id>install node and npm</id>
            <goals><goal>install-node-and-npm</goal></goals>
            <configuration><nodeVersion>v20.20.2</nodeVersion></configuration>
        </execution>
        <execution>
            <id>npm install</id>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>install</arguments></configuration>
        </execution>
        <execution>
            <id>npm run build</id>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>run build</arguments></configuration>
        </execution>
    </executions>
</plugin>
```

Este plugin corre **antes** de que `maven-war-plugin` empaquete el WAR (las ejecuciones de
`frontend-maven-plugin` caen por default en la fase `generate-resources`, que ocurre antes
de `package`). En orden: descarga su propia copia de Node/npm (no depende de lo que tengas
instalado en el sistema, así el build es reproducible en cualquier máquina), corre
`npm install`, corre `npm run build` (que por la Pieza 1 ya escribe directo en
`src/main/webapp/`). Para cuando `maven-war-plugin` entra a actuar, los archivos del
frontend ya están ahí, listos para empaquetarse junto con las clases Java compiladas.

**Resultado**: `mvn package` por sí solo hace *todo* — ya no hay un paso manual de
`npm run build` aparte.

---

## 3. Estructura del proyecto

```
frontend/
├── index.html          punto de entrada real (fuente, no el generado)
├── package.json         dependencias + scripts (npm run dev/build/preview)
├── vite.config.ts       outDir, base, proxy -- ver seccion 2
├── tsconfig*.json       configuracion de TypeScript
└── src/
    ├── main.tsx          arranca React, monta <App /> en el DOM
    ├── App.tsx           componente raiz, arma la pagina
    ├── index.css         estilos (minimalistas, sin libreria de CSS)
    ├── react-table.d.ts  extension de tipos para TanStack Table
    ├── api/
    │   ├── types.ts       interfaces TS que reflejan los DTO de Java
    │   └── client.ts       funciones fetch() hacia el backend (ruta relativa)
    └── components/
        ├── ProductosPanel.tsx   contenedor: fetch + mutaciones (crear/editar/eliminar)
        ├── ProductosTable.tsx   tabla (recibe productos + callbacks via props)
        ├── ProductoForm.tsx     formulario de creacion/edicion
        └── FacturasTable.tsx

src/main/webapp/         GENERADO por `npm run build` -- no se edita a mano, se pisa
                          completo en cada build (emptyOutDir: true)
```

Ver `react.md` para entender los componentes, y `tanstack.md` para `client.ts` + la lógica
de las tablas y las mutaciones.

---

## 4. Instalación

```bash
cd frontend
npm install
```

Igual que siempre: descarga dependencias a `node_modules/` (no se sube a git). Este paso es
para cuando quieres trabajar en modo desarrollo (`npm run dev`) — el build de producción vía
Maven **no necesita que corras esto a mano**, el plugin instala su propio Node/npm y corre
su propio `npm install`.

## 5. Correr en desarrollo

```bash
npm run dev
```

Levanta Vite en `http://localhost:5173`, con HMR. Las llamadas a la API se van por el proxy
configurado (Pieza 1) hacia GlassFish en el 8080 — para que esto funcione, **GlassFish
tiene que estar corriendo** (`start-domain` + `start-database`), aunque el frontend en sí lo
sirva Vite.

## 6. Build + deploy (el flujo real de esta variante)

```bash
cd /home/robute/IdeaProjects/HelloJakarta-variante
mvn package
```

Este único comando: instala Node/npm propios (solo la primera vez, después usa la copia ya
descargada), corre `npm install` + `npm run build` (que escribe en `src/main/webapp/`), y
empaqueta todo en `target/HelloJakarta-variante.war`.

Desplegar, igual que siempre:

```bash
cd /home/robute/Documentos/codes/SanboxTEST/glassfish7/glassfish/bin
./asadmin deploy --force=true /home/robute/IdeaProjects/HelloJakarta-variante/target/HelloJakarta-variante.war
```

Resultado: `http://localhost:8080/HelloJakarta-variante/` sirve la app completa — frontend
y API, mismo origen, un solo artefacto desplegado.

## 7. Costo de esta estrategia (para que quede claro el trade-off)

Cada cambio en el frontend, por chico que sea, requiere el ciclo completo:
`mvn package` → `asadmin deploy`. No hay hot-swap de frontend sin tocar el backend, a
diferencia de la estrategia alternativa de usar el `docroot` del dominio (que se descartó
para esta variante, ver la conversación que dio origen a esta decisión). Para el tamaño de
este proyecto, ese costo es insignificante (el ciclo completo toma segundos) — pero es la
razón por la que esta estrategia no es la ideal para un frontend que cambia constantemente
en un ambiente de alto tráfico.
