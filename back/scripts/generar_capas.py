#!/usr/bin/env python3
"""Genera Repository/DTO/Mapper/Service/Resource a partir de @Entity ya existentes.

Uso:
    python3 generar_capas.py NombreEntidad [--con-eliminar] [--forzar]
    python3 generar_capas.py --todas [--con-eliminar] [--forzar]

Resuelve la parte "pura talacha" de agregar entidades nuevas: dado que las @Entity se
generan solas en IntelliJ (Persistence tool -> "Generate Persistence Mapping" a partir
del esquema real de la base) pero el salto Entity -> Repository/Mapper/Service/Resource
NO tiene ningun generador nativo en el IDE (ni en ninguna herramienta estandar -- Jakarta
Data es de Jakarta EE 11, 2024, el tooling todavia no lo cubre; JHipster/JPA Buddy estan
pensados para Spring Data, no para esto), este script escribe esos archivos siguiendo
exactamente el mismo patron que ya usan Producto/Factura/SesionCaja en este proyecto.

`--todas` recorre TODO `model/` y genera lo que falte para cada @Entity que encuentre --
pensado justo para el caso de "conecte 20 tablas de golpe".

Que SI genera (mecanico, siempre igual):
  - lib/<Entidad>Repository.java   (interfaz vacia, extends CrudRepository)
  - dto/<Entidad>DTO.java          (record, un componente por cada campo simple)
  - mapper/<Entidad>Mapper.java    (interfaz MapStruct, toDTO/toEntity)
  - lib/<Entidad>Service.java      (interfaz, extends Service<DTO, IdType>)
  - ejb/<Entidad>ServiceImpl.java  (@Stateless, crear/listar/buscarPorId/actualizar[/eliminar])
  - rest/<Entidad>Resource.java    (JAX-RS: GET lista, GET por id, POST, PUT[, DELETE])

Que NO genera (son decisiones de negocio, no boilerplate):
  - Validaciones en el DTO (@NotBlank, @Positive, etc.) -- se dejan como TODO.
  - patch()/PatchDTO -- solo algunas entidades lo necesitan (ver FacturaPatchDTO).
  - El @Path del Resource es una adivinanza simple (nombre en minusculas + "s") -- para
    nombres compuestos (ej. SesionCaja -> "sesiones-caja", no "sesioncajas") corrigelo a
    mano, pluralizar en español no se puede automatizar bien de forma generica.
  - Relaciones (@OneToMany/@ManyToOne/@JsonbTransient) -- se detectan y se EXCLUYEN
    del DTO/mapper automatico a proposito (ver SesionCajaMapper: las relaciones casi
    siempre necesitan una decision manual sobre que tanto serializar).

Si un archivo ya existe, no se toca (a menos que pases --forzar).
"""

import argparse
import re
import sys
from pathlib import Path

BACK = Path(__file__).resolve().parents[1]
JAVA_ROOT = BACK / "src/main/java/org/example"
MODEL_DIR = JAVA_ROOT / "model"
DTO_DIR = JAVA_ROOT / "dto"
LIB_DIR = JAVA_ROOT / "lib"
MAPPER_DIR = JAVA_ROOT / "mapper"
EJB_DIR = JAVA_ROOT / "ejb"
REST_DIR = JAVA_ROOT / "rest"

RELATION_MARKERS = ("@OneToMany", "@ManyToOne", "@ManyToMany", "@OneToOne", "@JsonbTransient")
COLECCION_TIPOS = ("List<", "Set<", "Collection<")

# Tipos que NO hace falta importar (java.lang o primitivos) al armar el DTO.
TIPOS_SIN_IMPORT = {
    "String", "Long", "Integer", "int", "long", "Double", "double",
    "Boolean", "boolean", "Short", "short", "Float", "float", "Character", "char",
    "Byte", "byte",
}
# A que paquete pertenece cada tipo que SI hace falta importar en el DTO.
IMPORTS_CONOCIDOS = {
    "BigDecimal": "java.math.BigDecimal",
    "BigInteger": "java.math.BigInteger",
    "LocalDate": "java.time.LocalDate",
    "LocalDateTime": "java.time.LocalDateTime",
    "LocalTime": "java.time.LocalTime",
    "Instant": "java.time.Instant",
}


def capitalizar(nombre: str) -> str:
    return nombre[0].upper() + nombre[1:]


def parsear_entidad(nombre_entidad: str):
    ruta = MODEL_DIR / f"{nombre_entidad}.java"
    if not ruta.exists():
        sys.exit(f"No existe {ruta} -- revisa el nombre exacto de la clase @Entity.")
    texto = ruta.read_text(encoding="utf-8")

    if "@Entity" not in texto:
        sys.exit(f"{ruta} no tiene @Entity -- no parece ser una entidad JPA.")

    # Cada campo: 0+ anotaciones (cada una puede traer parentesis con ,) seguidas de
    # "private Tipo nombre;" (con o sin valor default). No es un parser real de Java,
    # es un regex a proposito -- suficiente para el estilo de este proyecto, no para
    # cualquier .java del mundo.
    patron_campo = re.compile(
        r"((?:@[\w.]+(?:\([^()]*(?:\([^()]*\)[^()]*)*\))?\s*)*)"
        r"private\s+([\w.<>\[\],\s]+?)\s+(\w+)\s*(?:=[^;]+)?;",
        re.MULTILINE,
    )

    campos = []
    id_campo = None
    for anotaciones, tipo, nombre in patron_campo.findall(texto):
        tipo = tipo.strip()
        if any(marca in anotaciones for marca in RELATION_MARKERS):
            continue
        if any(tipo.startswith(c) for c in COLECCION_TIPOS):
            continue
        es_id = "@Id" in anotaciones
        campos.append((tipo, nombre))
        if es_id:
            id_campo = (tipo, nombre)

    if id_campo is None:
        sys.exit(f"No se encontro ningun campo @Id en {ruta}.")

    return id_campo, campos


def escribir_si_no_existe(ruta: Path, contenido: str, forzar: bool):
    if ruta.exists() and not forzar:
        print(f"  ya existe, no se toca: {ruta.relative_to(BACK)}")
        return
    ruta.parent.mkdir(parents=True, exist_ok=True)
    ruta.write_text(contenido, encoding="utf-8")
    accion = "sobrescrito" if ruta.exists() and forzar else "creado"
    print(f"  {accion}: {ruta.relative_to(BACK)}")


def imports_dto(campos):
    imports = set()
    for tipo, _ in campos:
        if tipo in TIPOS_SIN_IMPORT:
            continue
        if tipo in IMPORTS_CONOCIDOS:
            imports.add(IMPORTS_CONOCIDOS[tipo])
        else:
            # Heuristica para este proyecto: cualquier tipo que no sea un primitivo/java.lang
            # conocido es casi siempre un enum o clase hermana en org.example.model (ej. Rol).
            imports.add(f"org.example.model.{tipo}")
    return sorted(imports)


def generar_dto(entidad, campos):
    imports = imports_dto(campos)
    lineas_import = "".join(f"import {i};\n" for i in imports)
    componentes = ",\n".join(f"        {tipo} {nombre}" for tipo, nombre in campos)
    return f'''package org.example.dto;

{lineas_import}// Generado por scripts/generar_capas.py a partir de model/{entidad}.java -- revisa que
// los tipos importados sean correctos y AGREGA las validaciones (@NotBlank, @NotNull,
// @Positive, etc.) que correspondan a las reglas de negocio reales de {entidad}, no se
// adivinan solas (ver ProductoDTO para el patron de como se ven esas anotaciones).
public record {entidad}DTO(
{componentes}
) {{
}}
'''


def generar_repository(entidad, id_tipo):
    return f'''package org.example.lib;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import org.example.model.{entidad};

// Jakarta Data: sin implementacion escrita a mano -- el proveedor (EclipseLink, via
// GlassFish) genera la clase real en tiempo de despliegue a partir de este contrato.
@Repository
public interface {entidad}Repository extends CrudRepository<{entidad}, {id_tipo}> {{
}}
'''


def generar_mapper(entidad, id_nombre):
    return f'''package org.example.mapper;

import org.example.dto.{entidad}DTO;
import org.example.model.{entidad};
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

// MapStruct genera {entidad}MapperImpl en tiempo de compilacion (revisa
// target/generated-sources/annotations despues de compilar) -- los campos que coinciden
// de nombre y tipo entre {entidad} y {entidad}DTO se mapean solos, sin declarar nada.
@Mapper
public interface {entidad}Mapper {{

    {entidad}Mapper INSTANCE = Mappers.getMapper({entidad}Mapper.class);

    {entidad}DTO toDTO({entidad} entidad);

    // {id_nombre} se ignora a proposito: una entidad nueva nunca debe nacer con el id que
    // (si acaso) mando el cliente en el JSON -- lo genera la base de datos.
    @Mapping(target = "{id_nombre}", ignore = true)
    {entidad} toEntity({entidad}DTO dto);
}}
'''


def generar_service(entidad, dto, id_tipo, con_eliminar):
    eliminar = f"\n\n    boolean eliminar({id_tipo} id);" if con_eliminar else ""
    return f'''package org.example.lib;

import org.example.dto.{dto};

public interface {entidad}Service extends Service<{dto}, {id_tipo}> {{

    {dto} actualizar({id_tipo} id, {dto} dto);{eliminar}
}}
'''


def generar_service_impl(entidad, dto, id_tipo, campos, con_eliminar):
    var = entidad[0].lower() + entidad[1:]
    campos_sin_id = [(t, n) for t, n in campos]
    # el primer campo es el id (ver parsear_entidad) -- se excluye del copiado, igual
    # que ProductoServiceImpl.actualizar() nunca reasigna el id.
    id_tipo_campo, id_nombre = campos[0]
    resto = campos[1:]
    copiado = "\n".join(
        f"        entidad.set{capitalizar(nombre)}(dto.{nombre}());" for _, nombre in resto
    )

    metodo_eliminar = ""
    if con_eliminar:
        metodo_eliminar = f'''

    @Override
    public boolean eliminar({id_tipo} id) {{
        if ({var}Repository.findById(id).isEmpty()) {{
            return false;
        }}
        {var}Repository.deleteById(id);
        return true;
    }}'''

    return f'''package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.dto.{dto};
import org.example.lib.{entidad}Repository;
import org.example.lib.{entidad}Service;
import org.example.mapper.{entidad}Mapper;
import org.example.model.{entidad};

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Generado por scripts/generar_capas.py siguiendo el mismo patron que ProductoServiceImpl
// (Jakarta Data + MapStruct, sin EntityManager/flush() -- ver Documentation/bitacora-fixes.md
// incidente #15 si {entidad}.{id_nombre} usara GenerationType.IDENTITY en vez de SEQUENCE).
@Stateless
public class {entidad}ServiceImpl implements {entidad}Service {{

    @Inject
    private {entidad}Repository {var}Repository;

    private final {entidad}Mapper {var}Mapper = {entidad}Mapper.INSTANCE;

    @Override
    public {dto} crear({dto} dto) {{
        {entidad} creado = {var}Repository.insert({var}Mapper.toEntity(dto));
        return {var}Mapper.toDTO(creado);
    }}

    @Override
    public List<{dto}> listar() {{
        return {var}Repository.findAll()
                .map({var}Mapper::toDTO)
                .collect(Collectors.toList());
    }}

    @Override
    public {dto} buscarPorId({id_tipo} id) {{
        return {var}Mapper.toDTO({var}Repository.findById(id).orElse(null));
    }}

    @Override
    public {dto} actualizar({id_tipo} id, {dto} dto) {{
        Optional<{entidad}> existente = {var}Repository.findById(id);
        if (existente.isEmpty()) {{
            return null;
        }}
        {entidad} entidad = existente.get();
{copiado}
        {entidad} actualizado = {var}Repository.update(entidad);
        return {var}Mapper.toDTO(actualizado);
    }}{metodo_eliminar}
}}
'''


def generar_resource(entidad, dto, id_tipo, con_eliminar):
    var = entidad[0].lower() + entidad[1:]
    path = entidad.lower() + "s"  # adivinanza simple -- corrige a mano si es irregular

    metodo_eliminar = ""
    if con_eliminar:
        metodo_eliminar = f'''

    @DELETE
    @Path("/{{id}}")
    public Response eliminar(@PathParam("id") {id_tipo} id) {{
        boolean eliminado = {var}Service.eliminar(id);
        if (!eliminado) {{
            return Response.status(Response.Status.NOT_FOUND).build();
        }}
        return Response.noContent().build();
    }}'''

    import_delete = "\nimport jakarta.ws.rs.DELETE;" if con_eliminar else ""

    return f'''package org.example.rest;

import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;{import_delete}
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.example.dto.{dto};
import org.example.lib.{entidad}Service;

import java.net.URI;

// Generado por scripts/generar_capas.py siguiendo el mismo patron que ProductoResource --
// revisa el @Path (adivinanza simple, "{entidad.lower()}s") si el plural correcto es
// irregular (ej. SesionCaja -> "sesiones-caja").
@Path("/{path}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class {entidad}Resource {{

    @EJB
    private {entidad}Service {var}Service;

    @Context
    private UriInfo uriInfo;

    @GET
    public Response listar() {{
        return Response.ok({var}Service.listar()).build();
    }}

    @GET
    @Path("/{{id}}")
    public Response buscar(@PathParam("id") {id_tipo} id) {{
        {dto} dto = {var}Service.buscarPorId(id);
        if (dto == null) {{
            return Response.status(Response.Status.NOT_FOUND).build();
        }}
        return Response.ok(dto).build();
    }}

    @POST
    public Response crear(@Valid {dto} dto) {{
        {dto} creado = {var}Service.crear(dto);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(creado.id())).build();
        return Response.created(location).entity(creado).build();
    }}

    @PUT
    @Path("/{{id}}")
    public Response actualizar(@PathParam("id") {id_tipo} id, @Valid {dto} dto) {{
        {dto} actualizado = {var}Service.actualizar(id, dto);
        if (actualizado == null) {{
            return Response.status(Response.Status.NOT_FOUND).build();
        }}
        return Response.ok(actualizado).build();
    }}{metodo_eliminar}
}}
'''


def listar_entidades():
    """Todas las clases @Entity encontradas en model/, en orden alfabetico."""
    entidades = []
    for ruta in sorted(MODEL_DIR.glob("*.java")):
        texto = ruta.read_text(encoding="utf-8")
        if "@Entity" not in texto:
            continue
        match = re.search(r"class\s+(\w+)", texto)
        if match:
            entidades.append(match.group(1))
    return entidades


def generar_para_entidad(entidad, con_eliminar, forzar):
    id_campo, campos = parsear_entidad(entidad)
    id_tipo, id_nombre = id_campo
    dto_nombre = f"{entidad}DTO"

    print(f"== {entidad} == (id={id_tipo} {id_nombre}, {len(campos) - 1} campo(s) simple(s) mas)")

    escribir_si_no_existe(DTO_DIR / f"{dto_nombre}.java", generar_dto(entidad, campos), forzar)
    escribir_si_no_existe(LIB_DIR / f"{entidad}Repository.java", generar_repository(entidad, id_tipo), forzar)
    escribir_si_no_existe(MAPPER_DIR / f"{entidad}Mapper.java", generar_mapper(entidad, id_nombre), forzar)
    escribir_si_no_existe(LIB_DIR / f"{entidad}Service.java", generar_service(entidad, dto_nombre, id_tipo, con_eliminar), forzar)
    escribir_si_no_existe(EJB_DIR / f"{entidad}ServiceImpl.java", generar_service_impl(entidad, dto_nombre, id_tipo, campos, con_eliminar), forzar)
    escribir_si_no_existe(REST_DIR / f"{entidad}Resource.java", generar_resource(entidad, dto_nombre, id_tipo, con_eliminar), forzar)
    print()


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    grupo = parser.add_mutually_exclusive_group(required=True)
    grupo.add_argument("entidad", nargs="?", help="Nombre exacto de la clase @Entity (ej. Usuario)")
    grupo.add_argument("--todas", action="store_true", help="Genera para TODAS las @Entity que encuentre en model/")
    parser.add_argument("--con-eliminar", action="store_true", help="Tambien genera eliminar()/DELETE (como Producto, no como Factura)")
    parser.add_argument("--forzar", action="store_true", help="Sobrescribe archivos que ya existan")
    args = parser.parse_args()

    if args.todas:
        entidades = listar_entidades()
        print(f"Encontradas {len(entidades)} @Entity en model/: {', '.join(entidades)}")
        print()
        for entidad in entidades:
            generar_para_entidad(entidad, args.con_eliminar, args.forzar)
    else:
        generar_para_entidad(args.entidad, args.con_eliminar, args.forzar)

    print("Pendiente a mano (a proposito, no se genera solo):")
    print("  - Revisar tipos/imports de cada <Entidad>DTO.java y agregar validaciones reales.")
    print("  - Entidades con relaciones (@OneToMany/@ManyToOne): decidir como se ven en el DTO")
    print("    (ver SesionCajaMapper.java para un ejemplo de mapper escrito a mano con relaciones).")
    print("  - El @Path de cada Resource es una adivinanza simple (entidad + 's') -- corrige")
    print("    los plurales irregulares a mano (ej. SesionCaja -> \"sesiones-caja\").")


if __name__ == "__main__":
    main()
