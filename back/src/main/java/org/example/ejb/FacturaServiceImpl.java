package org.example.ejb;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.example.dto.FacturaDto;
import org.example.dto.FacturaPatchDto;
import org.example.lib.FacturaReadService;
import org.example.lib.FacturaService;
import org.example.lib.FacturaWriteService;

import java.util.List;

// Fachada: sin logica propia, solo delega al Read o al Write segun corresponda.
@Stateless
public class FacturaServiceImpl implements FacturaService {

    @EJB
    private FacturaReadService facturaReadService;

    @EJB
    private FacturaWriteService facturaWriteService;

    @Override
    public List<FacturaDto> listar() {
        return facturaReadService.listar();
    }

    @Override
    public FacturaDto buscarPorId(Long id) {
        return facturaReadService.buscarPorId(id);
    }

    @Override
    public FacturaDto crear(FacturaDto dto) {
        return facturaWriteService.crear(dto);
    }

    @Override
    public FacturaDto actualizar(Long id, FacturaDto dto) {
        return facturaWriteService.actualizar(id, dto);
    }

    @Override
    public FacturaDto patch(Long id, FacturaPatchDto cambios) {
        return facturaWriteService.patch(id, cambios);
    }
}
