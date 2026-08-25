package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.dto.FacturaDTO;
import org.example.lib.FacturaRepository;
import org.example.lib.FacturaService;
import org.example.lib.ProductoRepository;
import org.example.mapper.FacturaMapper;
import org.example.model.Factura;
import org.example.model.FacturaDetalle;
import org.example.model.Producto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Stateless
public class FacturaServiceImpl implements FacturaService {

    // @Inject, no @EJB: FacturaRepository ahora es un repositorio Jakarta Data (bean CDI),
    // generado por el proveedor -- ya no existe FacturaRepositoryImpl escrito a mano.
    @Inject
    private FacturaRepository facturaRepository;

    // Necesita el repositorio de OTRA entidad para calcular precios reales -- esto es
    // justo el tipo de regla que pertenece a la capa de Service, no a un Repository
    // (que debe quedarse "tonto", solo persistiendo lo que se le pase).
    @Inject
    private ProductoRepository productoRepository;

    // Ver ProductoServiceImpl: el repositorio Jakarta Data no sincroniza el id IDENTITY
    // en el objeto que devuelve insert() -- este EntityManager comparte el mismo contexto
    // de persistencia (misma transaccion JTA, misma unidad HelloJakartaPU) y el flush()
    // fuerza esa sincronizacion.
    @PersistenceContext(unitName = "HelloJakartaPU")
    private EntityManager em;

    @Override
    public FacturaDTO crear(FacturaDTO dto) {
        Factura factura = FacturaMapper.toEntity(dto);

        if (factura.getFecha() == null) {
            factura.setFecha(LocalDate.now());
        }

        BigDecimal total = BigDecimal.ZERO;
        for (FacturaDetalle detalle : factura.getDetalles()) {
            // El precio SIEMPRE se recalcula del lado del servidor, nunca se confia
            // en el precio que mande el cliente en el JSON.
            Producto producto = productoRepository.findById(detalle.getProducto().getId()).orElse(null);
            detalle.setProducto(producto);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            detalle.setFactura(factura);
            total = total.add(detalle.getSubtotal());
        }
        factura.setTotal(total);

        Factura creada = facturaRepository.insert(factura);
        em.flush();
        return FacturaMapper.toDTO(creada);
    }

    @Override
    public List<FacturaDTO> listar() {
        // findAll() de Jakarta Data devuelve Stream<T>, no List<T>.
        return facturaRepository.findAll()
                .map(FacturaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public FacturaDTO buscarPorId(Long id) {
        return FacturaMapper.toDTO(facturaRepository.findById(id).orElse(null));
    }

    @Override
    public FacturaDTO actualizar(Long id, FacturaDTO dto) {
        // El copiado de campos ya no vive en el Repository (Jakarta Data no permite
        // metodos con cuerpo propio en la interfaz) -- es logica de negocio, vive aqui.
        // Solo se tocan numero/fecha/cliente: editar detalles/total de una factura ya
        // emitida queda fuera del alcance de este PUT (ver FacturaRepository original).
        Optional<Factura> existente = facturaRepository.findById(id);
        if (existente.isEmpty()) {
            return null;
        }
        Factura factura = existente.get();
        factura.setNumero(dto.getNumero());
        factura.setFecha(dto.getFecha());
        factura.setCliente(dto.getCliente());
        Factura actualizada = facturaRepository.update(factura);
        return FacturaMapper.toDTO(actualizada);
    }
}
