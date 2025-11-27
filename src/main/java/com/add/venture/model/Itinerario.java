package com.add.venture.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Itinerario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Itinerario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_itinerario")
    private Integer idItinerario;

    @Column(name = "dia_numero")
    private Integer diaNumero;

    @Column(length = 100)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "punto_partida", columnDefinition = "TEXT")
    private String puntoPartida;

    @Column(name = "punto_llegada", columnDefinition = "TEXT")
    private String puntoLlegada;

    @Column(name = "duracion_estimada", length = 50)
    private String duracionEstimada;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "id_grupo")
    @JsonIgnore
    private GrupoViaje grupo;
}
