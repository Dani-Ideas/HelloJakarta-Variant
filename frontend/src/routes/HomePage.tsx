import { Link } from "@tanstack/react-router"
import { ClipboardListIcon, LogOutIcon, WalletIcon } from "lucide-react"
import { PieMenu } from "../components/PieMenu"

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
          <Link to="/sesiones-caja" className="boton-nuevo">
            Ver sesiones de caja
          </Link>
          <Link to="/usuarios" className="boton-nuevo">
            Ver usuarios
          </Link>
        </div>
      </section>

      <section>
        <h2>Menú de pago</h2>
        <p className="mb-4 text-sm text-muted-foreground">
          Menú tipo "pastel" (pie/radial menu): 3 rebanadas iguales, cada una entra a una
          demo distinta (nada real, solo front-end).
        </p>

        <PieMenu
          items={[
            { to: "/salir-sitio", label: "Opción 1", icon: LogOutIcon },
            { to: "/formulario-pago", label: "Opción 2", icon: WalletIcon },
            { to: "/formulario-largo", label: "Opción 3", icon: ClipboardListIcon },
          ]}
          size={280}
        />
      </section>
    </>
  )
}
