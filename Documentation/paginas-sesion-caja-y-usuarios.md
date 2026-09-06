# Las pantallas nuevas: "Sesiones de caja" y "Usuarios"

Este documento explica **qué se agregó** y **para qué sirve**, en español sencillo — como
si se lo explicaras a alguien que nunca ha programado. Para el "cómo" técnico exacto (el
código, la sintaxis, los archivos), no lo repito aquí — dejo el link al documento que ya lo
explica en detalle, para no duplicar información desactualizable en dos lugares.

---

## 1. ¿Qué es esto, en términos simples?

Imagina una tienda con caja registradora. Dos cosas nuevas que ahora la app puede hacer:

- **"Sesiones de caja"**: registrar cuándo un cajero *abre* la caja (con cuánto dinero
  empezó) y, más adelante, cuándo la *cierra*. Es como el cuaderno donde el cajero anota
  "hoy empecé con $10, a las 8am".
- **"Usuarios"**: la lista de personas que trabajan en el negocio — su nombre y su
  **rol** (qué tipo de trabajador es: `ADMIN`, `VENDEDOR` o `CAJERO`). Es como la lista de
  empleados, con qué puesto tiene cada quien.

Ambas son pantallas nuevas dentro de la misma aplicación — se navega a ellas igual que ya
navegabas a "Productos" o "Facturas": hay un link arriba, en el menú.

---

## 2. ¿Qué puedes hacer en cada pantalla?

### "Sesiones de caja"

- **Ver la lista** de todas las sesiones que ha habido (quién, dónde, cuándo abrió/cerró,
  con cuánto dinero).
- **Abrir una caja nueva** — un botón que pide 3 datos (quién es el cajero, en qué
  ubicación/tienda, y con cuánto dinero de arranque) y la agrega a la lista.

Lo que **no** puedes hacer todavía: editar o borrar una sesión, ni "cerrarla" desde la
pantalla (el sistema por dentro sí sabe distinguir una sesión abierta de una cerrada, pero
el botón para cerrarla no se ha construido — es trabajo pendiente, no un error).

### "Usuarios"

- **Ver la lista** de empleados registrados, con su nombre y su rol.
- **Agregar uno nuevo**, **editar** uno existente (cambiarle el nombre o el rol), o
  **eliminarlo** — las 4 acciones completas, igual que en "Productos".

---

## 3. ¿Por qué "Sesiones de caja" puede hacer menos cosas que "Usuarios"?

Fue una decisión de diseño, no una limitación técnica: no tiene sentido de negocio poder
"borrar" una sesión de caja que ya registró dinero real entrando y saliendo — sería como
arrancar una página del cuaderno de contabilidad. `Usuario`, en cambio, sí se puede editar
o borrar sin ese problema (es solo información de quién trabaja ahí).

Esta misma distinción — qué acciones tiene sentido exponer para cada tipo de dato — ya se
explicó con más detalle técnico en
[`Documentation/mapeo-frontend-backend.md`](./mapeo-frontend-backend.md), sección
"Resumen — mapa completo en una tabla".

---

## 4. ¿Cómo está hecho por dentro? (el "cómo", con links)

Esto ya está explicado en otros documentos — no lo repito, solo te digo dónde mirar según
qué te interese:

| Si quieres entender... | Mira este documento |
|---|---|
| Cómo un botón termina mandando una petición al servidor, paso a paso, con la sintaxis exacta | [`mapeo-frontend-backend.md`](./mapeo-frontend-backend.md) |
| Qué es React, un "componente", por qué la pantalla se actualiza sola | [`react.md`](./react.md) |
| Qué es TanStack Query (por qué la tabla se refresca sola después de guardar) | [`tanstack.md`](./tanstack.md) |
| Cómo viaja una petición desde el navegador hasta la base de datos, fase por fase | [`como-funcionan-los-endpoints.md`](./como-funcionan-los-endpoints.md) |
| Cómo está organizado el proyecto de frontend (carpetas, build, etc.) | [`frontend.md`](./frontend.md) |
| Qué pasó del lado del servidor para que estos 2 endpoints (`/sesiones-caja`, `/usuarios`) existieran | [`bitacora-fixes.md`](./bitacora-fixes.md), incidentes 18 al 22 |

Lo único genuinamente **nuevo** que no está en ningún otro documento: los 8 archivos que se
crearon para estas 2 pantallas.

### Los archivos nuevos, en una frase cada uno

- `api/types.ts` — se le agregaron las "formas" (`SesionCajaDTO`, `UsuarioDTO`, `Rol`) que
  describen qué campos trae cada dato que llega del servidor.
- `api/client.ts` — se le agregaron las funciones que hacen las peticiones reales
  (`fetchSesionesCaja`, `crearSesionCaja`, `fetchUsuarios`, `createUsuario`,
  `updateUsuario`, `deleteUsuario`).
- `components/SesionCajaTable.tsx` / `UsuariosTable.tsx` — dibujan la tabla.
- `components/SesionCajaForm.tsx` / `UsuarioForm.tsx` — dibujan el formulario (los campos
  que llenas para crear/editar).
- `components/SesionCajaPanel.tsx` / `UsuariosPanel.tsx` — el "director de orquesta" de
  cada pantalla: decide cuándo mostrar la tabla, cuándo el formulario, y qué pasa cuando
  guardas o cancelas. Exactamente el mismo papel que ya cumplía `ProductosPanel.tsx`.
- `routes/SesionCajaPage.tsx` / `UsuariosPage.tsx` — le dicen al router "esta pantalla
  corresponde a esta URL" (`/sesiones-caja`, `/usuarios`).

Todos siguen **el mismo patrón** que ya existía para "Productos" — no se inventó nada
nuevo, se copió la misma receta 2 veces más.
