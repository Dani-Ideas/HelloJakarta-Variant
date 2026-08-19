# Frontend — HelloJakarta-variante

Carpeta: `frontend/`, hermana de `back/` (todo el backend Jakarta EE vive ahí, ver
`Documentation/glassfish.md`/`bitacora-fixes.md` para el porqué de esta separación). Stack:
**Vite + React + TypeScript + TanStack Query + TanStack Table**. Consume la API REST del
backend Jakarta EE (`GET /api/productos`, `GET /api/facturas`, `POST`/`PUT`/`DELETE` de
productos).

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
    outDir: "../back/src/main/webapp",
    emptyOutDir: true,
  },
  server: {
    proxy: {
      "/HelloJakarta-variante/api": "http://localhost:8080",
    },
  },
});
```

- **`build.outDir: "../back/src/main/webapp"`** → `npm run build` ya no escribe en
  `frontend/dist/`, escribe **directo dentro del proyecto Java**, en la carpeta que
  `maven-war-plugin` empaqueta automáticamente en el WAR (`../back/...` porque `frontend/`
  y `back/` son carpetas hermanas dentro del repo). `emptyOutDir: true` limpia esa carpeta
  antes de cada build (para no dejar archivos viejos con hashes distintos).
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
        <workingDirectory>../frontend</workingDirectory>
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
HelloJakarta-variante/
├── back/                 todo el backend Jakarta EE (pom.xml, src/main/java, etc.)
│   ├── src/main/java/org/example/
│   │   ├── lib/            interfaces (Repository, Service, y las especificas por entidad)
│   │   ├── ejb/            implementaciones @Stateless/@Singleton (ver DOCUMENTATION.md, sec. 1)
│   │   └── rest/           JAX-RS + SpaFallbackFilter (fallback de rutas de React)
│   └── src/main/webapp/   GENERADO por `npm run build` -- no se edita a mano, se pisa
│                          completo en cada build (emptyOutDir: true)
└── frontend/
    ├── index.html          punto de entrada real (fuente, no el generado)
    ├── package.json         dependencias + scripts (npm run dev/build/preview)
    ├── vite.config.ts       outDir, base, proxy -- ver seccion 2
    ├── tsconfig*.json       configuracion de TypeScript
    └── src/
        ├── main.tsx          arranca React + router, monta <RouterProvider/> en el DOM
        ├── router.tsx         el "mapa" de rutas -- ver tanstack.md, Parte 3
        ├── routes/
        │   ├── RootLayout.tsx    marco fijo (header + nav), envuelve TODAS las paginas
        │   ├── HomePage.tsx       pagina de "/"
        │   ├── ProductosPage.tsx  pagina de "/productos" (envuelve ProductosPanel)
        │   └── FacturasPage.tsx   pagina de "/facturas" (envuelve FacturasTable)
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
```

`back/` y `frontend/` son carpetas **hermanas** dentro del mismo repo — por eso las rutas
relativas en `vite.config.ts` y en el `pom.xml` usan `../` para cruzar de una a la otra (ver
sección 2).

**Nota**: `App.tsx` ya no existe — su función (armar el layout con header + secciones) la
reparten ahora `RootLayout.tsx` (el marco fijo) y las páginas individuales en `routes/`,
desde que se agregó navegación con TanStack Router.

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
cd /home/robute/IdeaProjects/HelloJakarta-variante/back
mvn package
```

Este único comando: instala Node/npm propios (solo la primera vez, después usa la copia ya
descargada), corre `npm install` + `npm run build` (que escribe en
`back/src/main/webapp/`), y empaqueta todo en `back/target/HelloJakarta-variante.war`.

Desplegar, igual que siempre:

```bash
cd /home/robute/Documentos/codes/SanboxTEST/glassfish7/glassfish/bin
./asadmin deploy --force=true /home/robute/IdeaProjects/HelloJakarta-variante/back/target/HelloJakarta-variante.war
```

Resultado: `http://localhost:8080/HelloJakarta-variante/` sirve la app completa — frontend
y API, mismo origen, un solo artefacto desplegado.

**Este flujo (`mvn package` + `asadmin deploy`) es para cuando cambió el backend** (o es la
primera vez que despliegas). Para cambios que son *solo* de frontend, existe un camino más
rápido — sección 7.

## 7. Hot-swap del frontend sin tocar el backend (`hot_swap_frontend.py`)

Script: `back/scripts/hot_swap_frontend.py`. Resuelve un problema real: si cada cambio de
texto en un botón te obligara a `mvn package` + `asadmin deploy` completo, en un ambiente de
alto tráfico eso significa procesar de nuevo todo el WAR (clases Java incluidas) solo para
cambiar unos KB de JS/CSS. Este script evita eso.

### Qué hace, en orden

1. **`npm audit --audit-level=high`** — chequeo de seguridad de las dependencias del
   frontend. Si encuentra vulnerabilidades de nivel alto o superior, pregunta antes de
   continuar (se puede saltar con `--skip-audit`).
2. **`npm run build`** — igual que siempre, genera los archivos con hash en
   `back/src/main/webapp/` (ver sección 2, Pieza 1).
3. **Diff contra lo que GlassFish ya tiene sirviendo** — compara archivo por archivo (por
   contenido, no por fecha) el build nuevo contra la carpeta **explotada** del WAR ya
   desplegado:
   ```
   glassfish7/glassfish/domains/domain1/applications/HelloJakarta-variante/
   ```
   Esa es la copia real que GlassFish lee del disco en cada petición — no es el `.war`
   comprimido, es la versión ya descomprimida con la que el servidor trabaja mientras corre.
4. **Copia solo lo que cambió** — archivos nuevos se agregan, archivos distintos se
   sobrescriben, archivos que ya no existen en el build nuevo se borran (limpia los `.js`
   con hash viejo que Vite ya no genera). **Nunca toca `WEB-INF/` ni `META-INF/`** — esas
   carpetas son las clases Java compiladas, se excluyen a propósito.

### Por qué esto no reinicia el backend

GlassFish no "carga" los archivos estáticos a memoria en el momento del deploy — los lee
del disco en cada petición, igual que cualquier servidor de archivos. Sobrescribir esos
archivos mientras GlassFish sigue corriendo es exactamente igual, desde la perspectiva del
servidor, a que tú edites una imagen en el docroot de cualquier hosting: el próximo request
simplemente encuentra contenido distinto. Las clases Java (`WEB-INF/classes`) sí están
cargadas en memoria por la JVM — por eso esas nunca se tocan aquí; si cambiaras una clase
Java así sin pasar por deploy, GlassFish seguiría ejecutando la versión vieja en memoria.

### Uso

```bash
cd /home/robute/IdeaProjects/HelloJakarta-variante
python3 back/scripts/hot_swap_frontend.py
```

Verificado en la práctica: se cambió un texto en `App.tsx`, se corrió el script, y
`curl http://localhost:8080/HelloJakarta-variante/` reflejó el cambio de inmediato — sin
ejecutar `asadmin deploy` ni una sola vez, y con la API (`/api/productos`) respondiendo sin
interrupción durante todo el proceso.

### Trade-off que sigue existiendo

Este script asume que **ya desplegaste el WAR al menos una vez** con el flujo normal
(sección 6) — necesita que exista la carpeta explotada para poder compararla. Y si el
cambio real es en el **backend** (una clase Java, el `pom.xml`, `persistence.xml`, etc.),
este script no sirve — ahí sí hace falta el ciclo completo de `mvn package` +
`asadmin deploy`, porque las clases Java sí requieren que el classloader las recargue.
