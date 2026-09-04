package org.example.ejb;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.example.dto.UsuarioDto;
import org.example.lib.UsuarioReadService;
import org.example.lib.UsuarioService;
import org.example.lib.UsuarioWriteService;

import java.util.List;

// Fachada: sin logica propia, solo delega al Read o al Write segun corresponda.
@Stateless
public class UsuarioServiceImpl implements UsuarioService {

    @EJB
    private UsuarioReadService usuarioReadService;

    @EJB
    private UsuarioWriteService usuarioWriteService;

    @Override
    public List<UsuarioDto> listar() {
        return usuarioReadService.listar();
    }

    @Override
    public UsuarioDto buscarPorId(Long id) {
        return usuarioReadService.buscarPorId(id);
    }

    @Override
    public UsuarioDto crear(UsuarioDto dto) {
        return usuarioWriteService.crear(dto);
    }

    @Override
    public UsuarioDto actualizar(Long id, UsuarioDto dto) {
        return usuarioWriteService.actualizar(id, dto);
    }

    @Override
    public boolean eliminar(Long id) {
        return usuarioWriteService.eliminar(id);
    }
}
