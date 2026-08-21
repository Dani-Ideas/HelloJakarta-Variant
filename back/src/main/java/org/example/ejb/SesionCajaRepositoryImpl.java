// ejb/SesionCajaRepositoryImpl.java
package org.example.ejb;

import jakarta.ejb.Stateless;
import org.example.lib.SesionCajaRepository;
import org.example.model.SesionCaja;

@Stateless
public class SesionCajaRepositoryImpl extends AbstractRepository<SesionCaja, Long>
        implements SesionCajaRepository {

    @Override
    protected Class<SesionCaja> getEntityClass() {
        return SesionCaja.class;
    }
}