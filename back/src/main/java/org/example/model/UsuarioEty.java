package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "USUARIO")
public class UsuarioEty {

    // id/nombre/rol, no idUsuario/nombreUsuario/rolUsuario: la ingenieria inversa de JPA
    // Buddy prefijo los campos con el nombre de la entidad -- inconsistente con las otras
    // 4 entidades del proyecto, que usan nombres simples.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "NOMBRE", nullable = false)
    private String nombre;

    // Rol (enum), no String plano: para que la base no pueda guardar cualquier texto
    // suelto -- restringido a ADMIN/VENDEDOR/CAJERO. @Enumerated(STRING), no el default
    // (ORDINAL): guarda el nombre como texto, no la posicion numerica.
    @Enumerated(EnumType.STRING)
    @Column(name = "ROL")
    private Rol rol;

    public UsuarioEty() {
    }
}
