package com.add.venture.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrupoViajeResponseDTO {
    
    private Long idGrupo;
    private String nombreViaje;
    private Integer maxParticipantes;
    private String estado;
    
    // Información del viaje
    private ViajeInfo viaje;
    
    // Información del creador
    private CreadorInfo creador;
    
    // Participantes actuales
    private List<ParticipanteInfo> participantes;
    private Integer totalParticipantes;
    
    // Etiquetas
    private List<String> etiquetas;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViajeInfo {
        private Long idViaje;
        private String destinoPrincipal;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private String descripcion;
        private Integer rangoEdadMin;
        private Integer rangoEdadMax;
        private Boolean esVerificado;
        private String imagenDestacada;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreadorInfo {
        private Long idUsuario;
        private String nombreCompleto;
        private String fotoPerfil;
        private String iniciales;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipanteInfo {
        private Long idUsuario;
        private String nombreCompleto;
        private String fotoPerfil;
        private String iniciales;
    }
}
