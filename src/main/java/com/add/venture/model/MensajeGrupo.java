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
@Table(name = "MensajeGrupo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long idMensaje;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio = LocalDateTime.now();

    @Column(length = 20)
    private String estado = "activo";
    
    @Column(name = "tipo_mensaje", length = 20)
    private String tipoMensaje = "texto"; // texto, imagen, archivo
    
    @Column(name = "archivo_url")
    private String archivoUrl;
    
    @Column(name = "archivo_nombre")
    private String archivoNombre;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "id_grupo")
    @JsonIgnoreProperties({"creador", "viaje", "etiquetas", "itinerarios", "participantes", "mensajes", "resenas"})
    private GrupoViaje grupo;

    @ManyToOne
    @JoinColumn(name = "id_remitente")
    @JsonIgnoreProperties({"gruposCreados", "etiquetas", "logros", "contrasenaHash"})
    private Usuario remitente;
}