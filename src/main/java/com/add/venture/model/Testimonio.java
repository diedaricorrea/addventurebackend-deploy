package com.add.venture.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Testimonio")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Testimonio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_testimonio")
    private Long idTestimonio;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String comentario;

    @Column(columnDefinition = "INT CHECK (calificacion BETWEEN 1 AND 5)", nullable = false)
    private Integer calificacion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private Boolean aprobado = false;

    @Column(nullable = false)
    private Boolean destacado = false;

    @Column(nullable = false)
    private Boolean anonimo = false;

    // Relación con el usuario autor
    @ManyToOne
    @JoinColumn(name = "id_autor", nullable = false)
    private Usuario autor;

    // Relación opcional con el grupo de viaje que motivó el testimonio
    @ManyToOne
    @JoinColumn(name = "id_grupo")
    private GrupoViaje grupo;

    @PrePersist
    protected void onCreate() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
        if (aprobado == null) {
            aprobado = false;
        }
        if (destacado == null) {
            destacado = false;
        }
        if (anonimo == null) {
            anonimo = false;
        }
    }
}
