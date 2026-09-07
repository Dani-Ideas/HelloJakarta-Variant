import { useState } from "react"
import { Link, type LinkProps } from "@tanstack/react-router"
import type { LucideIcon } from "lucide-react"

export type PieMenuItem = {
  to: LinkProps["to"]
  label: string
  // Texto de la vista previa que aparece al pasar el mouse/foco sobre la rebanada --
  // que "espera" al usuario si le da clic a esta opcion.
  descripcion: string
  icon: LucideIcon
}

type Props = {
  items: PieMenuItem[]
  size?: number
}

// Geometria de un menu tipo "pastel" (pie/radial menu): reparte 360 grados en partes
// iguales segun items.length -- funciona igual si son 2, 3 o 20, porque el angulo de cada
// rebanada siempre es 360/total, nunca un numero fijo. viewBox fijo en 0..200 centrado en
// (100,100); "size" solo escala el <svg> en pantalla, no cambia esta geometria interna.
const CX = 100
const CY = 100
const RADIO = 95
const RADIO_ETIQUETA = RADIO * 0.65

function puntoPolar(radio: number, anguloDeg: number) {
  const rad = (anguloDeg * Math.PI) / 180
  // angulo 0 = arriba (12 en punto), crece en sentido horario -- mismo criterio que ya
  // se usaba en el menu anterior.
  return { x: CX + radio * Math.sin(rad), y: CY - radio * Math.cos(rad) }
}

// A diferencia del donut anterior (con un hueco al centro para el circulo de texto), cada
// rebanada aqui es un pastel completo: arranca en el centro (CX, CY), sale hasta el borde,
// traza el arco, y regresa al centro. Sin hueco no hace falta ningun circulo/boton
// tapando el medio -- las rebanadas ya se juntan solas en un punto.
function pathGajo(anguloInicioDeg: number, anguloFinDeg: number) {
  const arcoGrande = anguloFinDeg - anguloInicioDeg > 180 ? 1 : 0
  const p1 = puntoPolar(RADIO, anguloInicioDeg)
  const p2 = puntoPolar(RADIO, anguloFinDeg)
  return [
    `M ${CX} ${CY}`,
    `L ${p1.x} ${p1.y}`,
    `A ${RADIO} ${RADIO} 0 ${arcoGrande} 1 ${p2.x} ${p2.y}`,
    "Z",
  ].join(" ")
}

// Mismo tono que ya se usaba por rebanada (HSL repartido segun la posicion), pero
// parametrizado para poder pedirlo tambien fuera del render de cada <path> -- lo usan
// tanto el relleno de la rebanada como el fondo/la vista previa cuando esta activa.
function tonoDeGajo(indice: number, total: number) {
  return Math.round((indice * 360) / total)
}

// Menu tipo "pastel": cada rebanada es del mismo tamano (360/items.length) y navega a su
// ruta directo al hacer click -- no hay boton para "abrir/cerrar", las rebanadas SON el
// menu, siempre visibles.
export function PieMenu({ items, size = 260 }: Props) {
  const total = items.length
  // Rebanada con hover/foco encima ahora mismo (null = ninguna) -- controla dos cosas a
  // la vez: la vista previa de texto de abajo, y el tinte de fondo detras del SVG.
  const [activo, setActivo] = useState<number | null>(null)

  if (total === 0) return null

  const itemActivo = activo !== null ? items[activo] : null
  const tonoActivo = activo !== null ? tonoDeGajo(activo, total) : null

  return (
    <div className="mx-auto" style={{ maxWidth: size }}>
      <div
        className="rounded-2xl p-4 transition-colors duration-300 ease-out"
        style={{
          // "ligeramente" -- por eso el alpha bajo (0.12): el fondo solo insinua el color
          // de la rebanada activa, nunca lo reemplaza por completo.
          backgroundColor:
            tonoActivo !== null ? `hsla(${tonoActivo}, 55%, 45%, 0.12)` : "transparent",
        }}
      >
        <svg
          viewBox="0 0 200 200"
          role="img"
          aria-label="Menú de opciones"
          className="mx-auto h-auto w-full"
        >
          {items.map((item, indice) => {
            const inicio = (indice * 360) / total
            const fin = ((indice + 1) * 360) / total
            const medio = (inicio + fin) / 2
            const etiqueta = puntoPolar(RADIO_ETIQUETA, medio)
            const Icono = item.icon
            // Color repartido en HSL segun la posicion -- si manana items.length cambia (3,
            // 7, 20...), los colores se siguen repartiendo parejos solos, sin tocar nada aqui.
            const color = `hsl(${tonoDeGajo(indice, total)}, 55%, 45%)`

            return (
              <Link
                key={String(item.to)}
                to={item.to}
                className="group/gajo outline-none"
                onMouseEnter={() => setActivo(indice)}
                onMouseLeave={() => setActivo((actual) => (actual === indice ? null : actual))}
                onFocus={() => setActivo(indice)}
                onBlur={() => setActivo((actual) => (actual === indice ? null : actual))}
              >
                <path
                  d={pathGajo(inicio, fin)}
                  fill={color}
                  stroke="var(--background)"
                  strokeWidth={3}
                  className="cursor-pointer transition-opacity group-hover/gajo:opacity-85 group-focus-visible/gajo:opacity-85"
                />
                <foreignObject
                  x={etiqueta.x - 40}
                  y={etiqueta.y - 34}
                  width={80}
                  height={68}
                  className="pointer-events-none"
                >
                  <div className="flex h-full w-full flex-col items-center justify-center gap-1 text-center text-white">
                    <Icono className="size-5" />
                    <span className="text-xs leading-tight font-medium">{item.label}</span>
                  </div>
                </foreignObject>
                <title>{item.label}</title>
              </Link>
            )
          })}
        </svg>
      </div>

      {/* Vista previa de texto: siempre ocupa el mismo alto (min-h) para que el layout de
          abajo no "salte" al aparecer/desaparecer, y solo cambia su opacidad -- así la
          transición se siente como un fade, no como un contenido que entra empujando. */}
      <div className="mt-3 min-h-16 text-center">
        {itemActivo && (
          <div className="animate-in fade-in duration-200">
            <p className="text-sm font-semibold" style={{ color: `hsl(${tonoActivo}, 55%, 45%)` }}>
              {itemActivo.label}
            </p>
            <p className="text-xs text-muted-foreground">{itemActivo.descripcion}</p>
          </div>
        )}
      </div>
    </div>
  )
}
