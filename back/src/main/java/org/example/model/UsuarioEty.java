package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "USUARIO")
public class UsuarioEty {
    @Id
    @Column(name = "ID", nullable = false, precision = 19)
    private BigDecimal idUsuario;

    @Size(max = 255)
    @NotNull
    @Column(name = "NOMBRE", nullable = false)
    private String nombreUsuario;

    @Size(max = 255)
    @Column(name = "ROL")
    private String rolUsuario;


}