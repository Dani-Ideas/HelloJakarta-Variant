package org.example.ejb;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.example.dto.SesionCajaDto;
import org.example.lib.SesionCajaReadService;
import org.example.lib.SesionCajaService;
import org.example.lib.SesionCajaWriteService;

import java.util.List;

// Fachada: sin logica propia, solo delega al Read o al Write segun corresponda.
@Stateless
public class SesionCajaServiceImpl implements SesionCajaService {

    @EJB
    private SesionCajaReadService sesionCajaReadService;

    @EJB
    private SesionCajaWriteService sesionCajaWriteService;

    @Override
    public List<SesionCajaDto> listar() {
        return sesionCajaReadService.listar();
    }

    @Override
    public SesionCajaDto buscarPorId(Long id) {
        return sesionCajaReadService.buscarPorId(id);
    }

    @Override
    public SesionCajaDto crear(SesionCajaDto dto) {
        return sesionCajaWriteService.crear(dto);
    }

    // Delegacion hueca, igual que en ProductoServiceImpl/UsuarioServiceImpl.
    @Override
    public void refrescarCache(SesionCajaDto dto) {
        sesionCajaReadService.refrescarCache(dto);
    }

    @Override
    public void quitarDeCache(Long id) {
        sesionCajaReadService.quitarDeCache(id);
    }
}
