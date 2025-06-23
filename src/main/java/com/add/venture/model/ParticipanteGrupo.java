package com.add.venture.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ParticipanteGrupo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder 
@IdClass(ParticipanteGrupoId.class) // ParticipanteGrupoId.java (Clase para clave compuesta)
public class ParticipanteGrupo { 

    @Id
    @ManyToOne
    @JoinColumn(name = "id_usuario")
    @JsonIgnoreProperties({"gruposCreados", "etiquetas", "logros", "contrasenaHash"})
    private Usuario usuario;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_grupo")
    @JsonIgnoreProperties({"creador", "viaje", "etiquetas", "itinerarios", "participantes", "mensajes", "resenas"})
    private GrupoViaje grupo;

    @Column(name = "rol_participante", length = 30)
    private String rolParticipante;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_solicitud", length = 20)
    private EstadoSolicitud estadoSolicitud;

    @Column(name = "fecha_union")
    private LocalDateTime fechaUnion;

    public enum EstadoSolicitud {
        PENDIENTE, ACEPTADO, RECHAZADO
    }
}
