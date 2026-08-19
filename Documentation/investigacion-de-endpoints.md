# Cómo investigar cualquier endpoint — de básico a avanzado

Guía por niveles. Los niveles 1 y 2 son el método que ya practicamos juntos con
`GET /api/productos`. El nivel 3 es contenido nuevo — técnicas que no hemos usado todavía
en este proyecto, pero que sí vas a necesitar en un proyecto real de trabajo.

---

## Nivel 1 — Básico: verlo desde afuera, sin tocar código

### 1.1 El navegador, para `GET`

Pega la URL directo en la barra de direcciones. Sirve solo para `GET` (es lo único que un
navegador manda al pegar una URL).

```
http://localhost:8080/HelloJakarta-variante/api/productos
```

### 1.2 `curl` con `-v` (verbose)

```bash
curl -v http://localhost:8080/HelloJakarta-variante/api/productos
```

Te muestra los headers de ida y vuelta, y el código de estado. Fíjate en `Content-Type`
(¿JSON o HTML?) y en la forma del cuerpo (¿array `[...]`? ¿objeto `{...}`? ¿qué campos?).

### 1.3 DevTools del navegador → pestaña Network

`F12` → **Network** → recarga la página. Ves cada petición real que hace React, con
headers, tiempos y respuesta completa. Funciona en cualquier página web, no solo aquí.

### 1.4 Buscar en el código con la URL ya conocida

1. `Ctrl+Shift+F` en IntelliJ → buscar `@Path("/productos")` → te lleva a la clase.
2. Dentro de la clase, busca el método con el verbo correcto (`@GET`, `@POST`, etc.).
3. Sigue la cadena hacia abajo: `Resource` (inyecta la interfaz `lib.Service`) →
   `ejb.ServiceImpl` (negocio + `Mapper`) → interfaz `lib.Repository` → `ejb.*RepositoryImpl`
   (`EntityManager`) → `Entity`. Ver `DOCUMENTATION.md` sección 1 para el diagrama completo.

### 1.5 Las 4 preguntas de cualquier endpoint

1. ¿Qué verbo HTTP? 2. ¿Qué espera recibir? 3. ¿Qué regresa? 4. ¿Qué status da en éxito y
en error?

---

## Nivel 2 — Intermedio: interactuar, no solo mirar

### 2.1 Probar los otros verbos con `curl`

```bash
# POST con body JSON
curl -X POST http://localhost:8080/HelloJakarta-variante/api/productos \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Prueba","sku":"TEST-01","precio":9.99,"stock":5}'

# PUT (actualizar)
curl -X PUT http://localhost:8080/HelloJakarta-variante/api/productos/1 \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Nuevo nombre","sku":"PRD-002","precio":10.00,"stock":50}'

# DELETE
curl -X DELETE http://localhost:8080/HelloJakarta-variante/api/productos/999
```

### 2.2 Probar casos límite a propósito (esto enseña más que el caso feliz)

- Un `id` que no existe → ¿da `404`?
- Un campo obligatorio vacío → ¿da `400` con mensaje claro? (nuestro
  `ValidationExceptionMapper` existe justo para esto)
- Borrar algo que está en uso por otra tabla → ¿da `409`? (lo vivimos con
  `DELETE /productos/{id}` cuando el producto está en una factura)

Provocar errores a propósito es la forma más rápida de entender qué protecciones tiene un
endpoint — mucho más que solo ver el caso que sí funciona.

### 2.3 `jq` — para no leer JSON crudo a ojo

```bash
curl -s http://localhost:8080/HelloJakarta-variante/api/productos | jq .
curl -s http://localhost:8080/HelloJakarta-variante/api/productos | jq '.[0].nombre'
curl -s http://localhost:8080/HelloJakarta-variante/api/productos | jq 'length'
```

`jq` formatea el JSON con indentación y colores, y te deja **filtrar** campos específicos
sin tener que leer todo el bloque. Si no lo tienes instalado: `sudo pacman -S jq` (Manjaro).

### 2.4 Comparar Request vs Response completos en DevTools

En la pestaña Network, al hacer clic en una petición, tiene sub-pestañas: **Headers**
(todo lo que se mandó/recibió), **Payload** (el body que mandaste, si hubo), **Response**
(el body que llegó), **Timing** (cuánto tardó cada etapa: DNS, conexión, espera, descarga).

---

## Nivel 3 — Avanzado: lo que todavía no habíamos visto

### 3.1 Debugging en vivo — "congelar" la petición a medias

En vez de solo leer código, puedes **pausarlo mientras corre de verdad**:

1. Pon un breakpoint (clic en el margen izquierdo) en `ProductoResource.listar()`.
2. Corre GlassFish en modo **Debug** desde IntelliJ (no Run).
3. Manda la petición (`curl` o navegador).
4. IntelliJ pausa la ejecución justo ahí — puedes ver el valor real de cada variable en ese
   instante, avanzar línea por línea (`F8`), y hasta "meterte" dentro de `productoService.listar()`
   (`F7`) para ver qué pasa ahí adentro también, sin adivinar nada.

Esto es lo más cercano a "ver el endpoint pensando" que existe.

### 3.2 Ver el SQL real que se ejecutó

Ya tienes esto configurado (`persistence.xml`, `eclipselink.logging.level=FINE`) — mientras
mandas una petición, mira el log en vivo:

```bash
tail -f /home/robute/Documentos/codes/SanboxTEST/glassfish7/glassfish/domains/domain1/logs/server.log
```

Vas a ver el `SELECT`/`INSERT` real que EclipseLink le mandó a Derby — la traducción final
de tu JPQL a SQL de verdad, con los valores reales.

### 3.3 Ver un rollback en acción

Fuerza un error a mitad de una operación con varios pasos — por ejemplo, mandar una factura
con un `productoId` que no existe. En el log vas a ver que la transacción completa se
deshace (nada queda guardado a medias). Esto demuestra en la práctica lo que ya sabes en
teoría: que `@Stateless` con CMT protege la operación completa o nada.

### 3.4 Leer un stack trace cuando algo da `500`

Cuando un endpoint truena, el log imprime un stack trace larguísimo (líneas de
`com.sun.ejb...`, `org.eclipse.persistence...`, etc.) — la mayoría es infraestructura, no
tu código. **Técnica**: lee de arriba hacia abajo y busca la **primera línea que diga
`org.example`** — esa es la línea exacta de tu propio código donde reventó. Todo lo que
está arriba de esa línea en el trace es la causa (el error real); todo lo que está abajo es
solo "quién llamó a quién" para llegar hasta ahí.

### 3.5 Medir tiempos con `curl`

```bash
curl -s -o /dev/null -w "DNS: %{time_namelookup}s | Conexion: %{time_connect}s | Total: %{time_total}s\n" \
  http://localhost:8080/HelloJakarta-variante/api/productos
```

Útil para notar si un endpoint específico es lento, y en qué etapa (¿conexión? ¿espera del
servidor? ¿descarga del body?).

### 3.6 Idempotencia — un concepto que cambia cómo diseñas endpoints

- `GET`, `PUT`, `DELETE` **deberían** ser idempotentes: hacer la misma petición 5 veces
  seguidas da el mismo resultado que hacerla 1 vez (actualizar el mismo dato al mismo
  valor, o borrar algo que ya no existe, no debería "explotar" ni duplicar nada).
- `POST` **no** es idempotente por diseño: cada llamada crea algo nuevo (5 `POST` iguales
  = 5 facturas nuevas, no 1).

Esto no es una regla nuestra — es una convención HTTP que cualquier API bien diseñada
respeta, y por eso `crear()` usa `POST` y `actualizar()` usa `PUT` en este proyecto.

### 3.7 Documentación automática de APIs (concepto, no implementado aquí)

En proyectos reales de trabajo, es común encontrar una interfaz **Swagger/OpenAPI** —
una página web autogenerada que lista todos los endpoints de la API, con formularios para
probarlos directo desde el navegador, sin `curl` ni Postman. No la configuramos en este
proyecto, pero si en tu trabajo ves una URL como `/swagger-ui` o `/openapi.json`, es
exactamente esto — un "catálogo" de endpoints generado automáticamente desde las mismas
anotaciones (`@Path`, `@GET`, etc.) que ya sabes leer.
