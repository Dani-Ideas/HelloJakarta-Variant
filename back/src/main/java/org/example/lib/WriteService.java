package org.example.lib;

// Solo la operacion de escritura que SIEMPRE tiene sentido para cualquier entidad que
// escriba: crear. "actualizar"/"patch"/"eliminar" quedan fuera a proposito -- no todas las
// entidades los necesitan (SesionCaja no expone ninguno de los tres, Factura no expone
// eliminar) -- cada XService los agrega directo si le hacen falta, igual que ya se hacia
// antes de este split.
public interface WriteService<D, ID> extends Service<D, ID> {

    D crear(D dto);
}
