package com.add.venture.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Viaje")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Viaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_viaje")
    private Long idViaje;

    @Column(name = "destino_principal", length = 100)
    private String destinoPrincipal;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "punto_encuentro", columnDefinition = "TEXT")
    private String puntoEncuentro;

    @Column(name = "imagen_destacada", columnDefinition = "TEXT")
    private String imagenDestacada;

    @Column(name = "rango_edad_min")
    private Integer rangoEdadMin;

    @Column(name = "rango_edad_max")
    private Integer rangoEdadMax;

    @Column(name = "es_verificado")
    private Boolean esVerificado = false;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(length = 20)
    private String estado = "activo";

    // Relaciones
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToOne(mappedBy = "viaje", cascade = CascadeType.ALL)
    @JsonIgnore
    private GrupoViaje grupo;
}
