package com.add.venture.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@Table(name = "Notificacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long idNotificacion;

    @Column(length = 30)
    private String tipo;

    @Column(columnDefinition = "TEXT")
    private String contenido;

    private Boolean leido = false;

    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;

    @Column(length = 20)
    private String estado = "activo";

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "id_usuario")
    @JsonIgnoreProperties({"gruposCreados", "etiquetas", "logros", "contrasenaHash"})
    private Usuario usuario;
    
    // Campos adicionales para solicitudes de unión
    @ManyToOne
    @JoinColumn(name = "id_grupo")
    @JsonIgnoreProperties({"creador", "viaje", "etiquetas", "itinerarios", "participantes", "mensajes", "resenas"})
    private GrupoViaje grupo;
    
    @ManyToOne
    @JoinColumn(name = "id_solicitante")
    @JsonIgnoreProperties({"gruposCreados", "etiquetas", "logros", "contrasenaHash"})
    private Usuario solicitante;
}
