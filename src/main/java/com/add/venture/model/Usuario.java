package com.add.venture.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Exclude;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    @EqualsAndHashCode.Include
    private Long idUsuario;

    @Column(length = 50)
    private String nombre;

    @Column(length = 50)
    private String apellidos;

    @Column(name = "nombre_usuario", length = 30, unique = true)
    private String nombreUsuario;

    @Column(length = 100, unique = true)
    @EqualsAndHashCode.Include
    private String email;

    @Column(length = 20, unique = true)
    private String telefono;

    @Column(length = 50)
    private String pais;

    @Column(length = 50)
    private String ciudad;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "contraseña_hash")
    @JsonIgnore
    private String contrasenaHash;

    @Column(name = "foto_perfil")
    private String fotoPerfil;

    @Column(name = "foto_portada")
    private String fotoPortada;

    private String descripcion;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "es_verificado")
    private Boolean esVerificado = false;

    @Column(name = "estado_cuenta", length = 20)
    private String estadoCuenta = "activa";

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(length = 20)
    private String estado = "activo";

    // Excluir las colecciones que causan referencias circulares
    @EqualsAndHashCode.Exclude
    @ManyToMany
    @JoinTable(name = "UsuarioEtiqueta", joinColumns = @JoinColumn(name = "id_usuario"), inverseJoinColumns = @JoinColumn(name = "id_etiqueta"))
    @JsonIgnore
    private Set<Etiqueta> etiquetas;

    // Excluir otras colecciones que puedan causar referencias circulares
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "creador")
    @ToString.Exclude
    @JsonIgnore
    private Set<GrupoViaje> gruposCreados;

    // Otras relaciones que deban ser excluidas

    // Añadir la relación con Logro
    @ManyToMany
    @JoinTable(name = "UsuarioLogro", joinColumns = @JoinColumn(name = "id_usuario"), inverseJoinColumns = @JoinColumn(name = "id_logro"))
    @JsonIgnore
    private Set<Logro> logros;

    public String getIniciales() {
        StringBuilder iniciales = new StringBuilder();

        if (nombre != null && !nombre.isBlank()) {
            iniciales.append(Character.toUpperCase(nombre.trim().charAt(0)));
        }

        if (apellidos != null && !apellidos.isBlank()) {
            iniciales.append(Character.toUpperCase(apellidos.trim().charAt(0)));
        }

        return iniciales.toString();
    }
}
