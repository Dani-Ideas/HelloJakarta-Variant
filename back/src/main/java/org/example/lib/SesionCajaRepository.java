package org.example.lib;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import org.example.model.SesionCaja;

// Jakarta Data: sin implementacion escrita a mano (no hay ejb/SesionCajaRepositoryImpl).
@Repository
public interface SesionCajaRepository extends CrudRepository<SesionCaja, Long> {
}
