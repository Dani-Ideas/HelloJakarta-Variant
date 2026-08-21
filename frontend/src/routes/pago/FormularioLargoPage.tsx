import { useState } from "react"
import { Link } from "@tanstack/react-router"
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSeparator,
  FieldSet,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { NativeSelect } from "@/components/ui/native-select"
import { Checkbox } from "@/components/ui/checkbox"
import { Switch } from "@/components/ui/switch"
import { Button } from "@/components/ui/button"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { CheckCircle2Icon } from "lucide-react"

// Formulario largo "de toda la vida" (sin carrusel): la unica pieza interactiva
// especial es la barra superior con el Breadcrumb, que se queda pegada arriba
// (sticky) aunque hagas scroll hasta el final del formulario.
export function FormularioLargoPage() {
  const [terminos, setTerminos] = useState(false)
  const [novedades, setNovedades] = useState(true)
  const [enviado, setEnviado] = useState(false)

  function enviar(evento: React.FormEvent) {
    evento.preventDefault()
    setEnviado(true)
  }

  return (
    <section>
      <div className="sticky top-0 z-40 -mx-6 mb-6 border-y bg-background/95 px-6 py-3 backdrop-blur supports-backdrop-filter:bg-background/75">
        <Breadcrumb>
          <BreadcrumbList>
            <BreadcrumbItem>
              <BreadcrumbLink asChild>
                <Link to="/">Inicio</Link>
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbLink asChild>
                <Link to="/">Menú de pago</Link>
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbPage>Formulario completo</BreadcrumbPage>
            </BreadcrumbItem>
          </BreadcrumbList>
        </Breadcrumb>
      </div>

      <h2>Opción 3 · Formulario completo</h2>
      <p className="mb-6 text-sm text-muted-foreground">
        Formulario tradicional (sin carrusel). También es solo visual: el botón
        "Enviar" no llama a ningún endpoint.
      </p>

      {enviado && (
        <Alert className="mb-6">
          <CheckCircle2Icon />
          <AlertTitle>Formulario enviado</AlertTitle>
          <AlertDescription>
            Simulación visual: los datos no se enviaron a ningún servidor.
          </AlertDescription>
        </Alert>
      )}

      <form onSubmit={enviar} className="max-w-xl">
        <FieldGroup>
          <FieldSet>
            <FieldLegend>Datos personales</FieldLegend>
            <FieldDescription>Cómo te podemos identificar.</FieldDescription>
            <Field>
              <FieldLabel htmlFor="nombre-largo">Nombre</FieldLabel>
              <Input id="nombre-largo" placeholder="Ada" required />
            </Field>
            <Field>
              <FieldLabel htmlFor="apellido-largo">Apellido</FieldLabel>
              <Input id="apellido-largo" placeholder="Lovelace" required />
            </Field>
            <Field>
              <FieldLabel htmlFor="correo-largo">Correo</FieldLabel>
              <Input id="correo-largo" type="email" placeholder="ada@ejemplo.com" required />
            </Field>
            <Field>
              <FieldLabel htmlFor="telefono-largo">Teléfono</FieldLabel>
              <Input id="telefono-largo" type="tel" placeholder="+52 55 0000 0000" />
            </Field>
          </FieldSet>

          <FieldSeparator />

          <FieldSet>
            <FieldLegend>Dirección</FieldLegend>
            <Field>
              <FieldLabel htmlFor="calle-largo">Calle y número</FieldLabel>
              <Input id="calle-largo" placeholder="Av. Siempre Viva 742" />
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field>
                <FieldLabel htmlFor="ciudad-largo">Ciudad</FieldLabel>
                <Input id="ciudad-largo" placeholder="Ciudad de México" />
              </Field>
              <Field>
                <FieldLabel htmlFor="cp-largo">Código postal</FieldLabel>
                <Input id="cp-largo" placeholder="00000" />
              </Field>
            </div>
            <Field>
              <FieldLabel htmlFor="pais-largo">País</FieldLabel>
              <NativeSelect id="pais-largo" defaultValue="">
                <option value="" disabled>
                  Selecciona un país…
                </option>
                <option value="mx">México</option>
                <option value="es">España</option>
                <option value="ar">Argentina</option>
                <option value="co">Colombia</option>
              </NativeSelect>
            </Field>
          </FieldSet>

          <FieldSeparator />

          <FieldSet>
            <FieldLegend>Preferencias</FieldLegend>
            <Field orientation="horizontal">
              <FieldLabel htmlFor="novedades-largo" className="flex-1">
                Recibir novedades por correo
              </FieldLabel>
              <Switch
                id="novedades-largo"
                checked={novedades}
                onCheckedChange={setNovedades}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="comentarios-largo">Comentarios</FieldLabel>
              <Textarea id="comentarios-largo" placeholder="Algo más que quieras contarnos…" />
            </Field>
            <Field orientation="horizontal">
              <Checkbox
                id="terminos-largo"
                checked={terminos}
                onCheckedChange={(marcado) => setTerminos(marcado === true)}
              />
              <FieldLabel htmlFor="terminos-largo" className="font-normal">
                Acepto los términos y condiciones
              </FieldLabel>
            </Field>
          </FieldSet>

          <Button type="submit" disabled={!terminos}>
            Enviar
          </Button>
        </FieldGroup>
      </form>
    </section>
  )
}
