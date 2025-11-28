package com.add.venture.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestimonioDTO {
    
    private Long idTestimonio;
    
    @NotBlank(message = "El comentario es obligatorio")
    @Size(min = 20, max = 500, message = "El comentario debe tener entre 20 y 500 caracteres")
    private String comentario;
    
    @NotNull(message = "La calificación es obligatoria")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private Integer calificacion;
    
    @NotNull(message = "Debes indicar si quieres que sea anónimo")
    private Boolean anonimo;
    
    // Datos del autor (solo si no es anónimo)
    private String nombreAutor;
    private String apellidoAutor;
    private String ciudadAutor;
    private String paisAutor;
    private String fotoPerfilAutor;
    
    private LocalDateTime fecha;
    private Boolean aprobado;
    private Boolean destacado;
    
    // ID del grupo relacionado (opcional)
    private Long idGrupo;
}
