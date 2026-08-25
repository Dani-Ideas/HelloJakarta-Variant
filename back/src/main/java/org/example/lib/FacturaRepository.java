package org.example.lib;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import org.example.model.Factura;

// Jakarta Data: sin implementacion escrita a mano (no hay FacturaRepositoryImpl en ejb/).
// El "actualizar" con copiado de campos de encabezado ahora vive en FacturaServiceImpl.
// FacturaResource deliberadamente no expone @DELETE para esta entidad -- borrar una
// factura ya emitida no tiene sentido de negocio real -- aunque deleteById() exista
// heredado de CrudRepository.
@Repository
public interface FacturaRepository extends CrudRepository<Factura, Long> {
}
