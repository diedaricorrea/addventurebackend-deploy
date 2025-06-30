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
@Table(name = "Resena")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resena")
    private Long idResena;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(columnDefinition = "INT CHECK (calificacion BETWEEN 1 AND 5)")
    private Integer calificacion;

    private LocalDateTime fecha;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "id_autor")
    private Usuario autor;

    @ManyToOne
    @JoinColumn(name = "id_destinatario")
    private Usuario destinatario;

    @ManyToOne
    @JoinColumn(name = "id_grupo")
    private GrupoViaje grupo;
    
    @PrePersist
    protected void onCreate() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
