import { Link } from "@tanstack/react-router"
import { ClipboardListIcon, LogOutIcon, WalletIcon, type LucideIcon } from "lucide-react"

type Ruta = "/salir-sitio" | "/formulario-pago" | "/formulario-largo"

type Gajo = {
  to: Ruta
  icono: LucideIcon
  titulo: string
  color: string
}

const GAJOS: Gajo[] = [
  { to: "/salir-sitio", icono: LogOutIcon, titulo: "Opción 1", color: "#3d5a80" },
  { to: "/formulario-pago", icono: WalletIcon, titulo: "Opción 2", color: "#b98b3e" },
  { to: "/formulario-largo", icono: ClipboardListIcon, titulo: "Opción 3", color: "#4f7d5c" },
]

// Geometria del "pay" (donut chart) de 3 gajos iguales -- viewBox 0..200, centrado en (100,100).
const CX = 100
const CY = 100
const RADIO_EXTERNO = 92
const RADIO_INTERNO = 40
const RADIO_ETIQUETA = (RADIO_EXTERNO + RADIO_INTERNO) / 2

function puntoPolar(radio: number, anguloDeg: number) {
  const rad = (anguloDeg * Math.PI) / 180
  // angulo 0 = arriba (12 en punto), crece en sentido horario
  return { x: CX + radio * Math.sin(rad), y: CY - radio * Math.cos(rad) }
}

function pathGajo(anguloInicio: number, anguloFin: number) {
  const arcoGrande = anguloFin - anguloInicio > 180 ? 1 : 0
  const p1o = puntoPolar(RADIO_EXTERNO, anguloInicio)
  const p2o = puntoPolar(RADIO_EXTERNO, anguloFin)
  const p2i = puntoPolar(RADIO_INTERNO, anguloFin)
  const p1i = puntoPolar(RADIO_INTERNO, anguloInicio)
  return [
    `M ${p1o.x} ${p1o.y}`,
    `A ${RADIO_EXTERNO} ${RADIO_EXTERNO} 0 ${arcoGrande} 1 ${p2o.x} ${p2o.y}`,
    `L ${p2i.x} ${p2i.y}`,
    `A ${RADIO_INTERNO} ${RADIO_INTERNO} 0 ${arcoGrande} 0 ${p1i.x} ${p1i.y}`,
    "Z",
  ].join(" ")
}

// Pagina de la ruta "/" (ver indexRoute en router.tsx). Es la que se ve dentro del
// <Outlet/> de RootLayout cuando entras a la app por primera vez.
export function HomePage() {
  return (
    <>
      <section>
        <h2>Bienvenido</h2>
        <p>Elige qué quieres ver:</p>
        <div className="botones-inicio">
          {/* Mismo <Link> que en el nav -- aqui se ve mas como "boton" gracias a la clase
              CSS boton-nuevo, pero por dentro sigue siendo navegacion de router, no un
              <button> ni un <a> con recarga de pagina. */}
          <Link to="/productos" className="boton-nuevo">
            Ver productos
          </Link>
          <Link to="/facturas" className="boton-nuevo">
            Ver facturas
          </Link>
        </div>
      </section>

      <section>
        <h2>Menú de pago</h2>
        <p className="mb-4 text-sm text-muted-foreground">
          Un "pay" (pie chart) de 3 gajos: cada uno entra a una demo distinta (nada real,
          solo front-end).
        </p>

        <svg
          viewBox="0 0 200 200"
          role="img"
          aria-label="Menú de pago con 3 opciones"
          className="mx-auto h-auto w-full max-w-64"
        >
          {GAJOS.map((gajo, indice) => {
            const inicio = indice * 120
            const fin = inicio + 120
            const medio = inicio + 60
            const etiqueta = puntoPolar(RADIO_ETIQUETA, medio)
            const Icono = gajo.icono
            return (
              <Link key={gajo.to} to={gajo.to} className="group/gajo outline-none">
                <path
                  d={pathGajo(inicio, fin)}
                  fill={gajo.color}
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
                    <span className="text-xs leading-tight font-medium">{gajo.titulo}</span>
                  </div>
                </foreignObject>
                <title>{gajo.titulo}</title>
              </Link>
            )
          })}

          <circle
            cx={CX}
            cy={CY}
            r={RADIO_INTERNO - 2}
            className="fill-card stroke-border"
            strokeWidth={1}
          />
          <foreignObject
            x={CX - 38}
            y={CY - 24}
            width={76}
            height={48}
            className="pointer-events-none"
          >
            <div className="flex h-full w-full items-center justify-center text-center text-[11px] font-medium text-muted-foreground">
              Elige una opción
            </div>
          </foreignObject>
        </svg>
      </section>
    </>
  )
}
