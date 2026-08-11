package org.example.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FacturaDTO {

    private Long id;
    private String numero;
    private LocalDate fecha;
    private String cliente;
    private BigDecimal total;
    private List<FacturaDetalleDTO> detalles;

    public FacturaDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public List<FacturaDetalleDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<FacturaDetalleDTO> detalles) {
        this.detalles = detalles;
    }
}
