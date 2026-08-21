import { Link } from "@tanstack/react-router"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { ExternalLinkIcon, TriangleAlertIcon } from "lucide-react"

// URL destino de la Opción 1 del menú de pago: consola de administración de
// GlassFish, corriendo en local durante el desarrollo.
const URL_DESTINO = "http://localhost:4848/"

export function SalirSitioPage() {
  function continuar() {
    window.open(URL_DESTINO, "_blank", "noopener,noreferrer")
  }

  return (
    <section className="max-w-lg">
      <h2>Opción 1 · Salir del sitio</h2>

      <Alert variant="destructive" className="mb-4">
        <TriangleAlertIcon />
        <AlertTitle>Estás a punto de salir de HelloJakarta</AlertTitle>
        <AlertDescription>
          El botón "Continuar" abrirá <strong>{URL_DESTINO}</strong> en una pestaña nueva
          (la consola de administración de GlassFish). Este sitio no controla ese destino.
        </AlertDescription>
      </Alert>

      <div className="flex gap-2">
        <Button variant="outline" asChild>
          <Link to="/">Cancelar</Link>
        </Button>
        <Button onClick={continuar}>
          <ExternalLinkIcon />
          Continuar a {URL_DESTINO}
        </Button>
      </div>
    </section>
  )
}
