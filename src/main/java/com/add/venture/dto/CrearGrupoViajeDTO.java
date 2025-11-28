package com.add.venture.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.add.venture.validation.FechaViaje;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearGrupoViajeDTO {

    // Datos del grupo
    @NotBlank(message = "El nombre del viaje es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre del viaje debe tener entre 3 y 100 caracteres")
    private String nombreViaje;
    
    // Datos del viaje
    @NotBlank(message = "El destino principal es obligatorio")
    @Size(min = 2, max = 100, message = "El destino principal debe tener entre 2 y 100 caracteres")
    private String destinoPrincipal;
    
    @NotNull(message = "La fecha de inicio es obligatoria")
    @FechaViaje(message = "La fecha de inicio debe ser al menos 1 semana después de hoy")
    private LocalDate fechaInicio;
    
    @NotNull(message = "La fecha de fin es obligatoria")
    @FechaViaje(message = "La fecha de fin debe ser al menos 1 semana después de hoy")
    private LocalDate fechaFin;
    
    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = 10, max = 1000, message = "La descripción debe tener entre 10 y 1000 caracteres")
    private String descripcion;
    
    @NotBlank(message = "El punto de encuentro es obligatorio")
    @Size(min = 5, max = 500, message = "El punto de encuentro debe tener entre 5 y 500 caracteres")
    private String puntoEncuentro;
    
    @Pattern(regexp = "^(https?://).*", message = "La imagen debe ser una URL válida que comience con http:// o https://")
    private String imagenDestacada;
    
    @Min(value = 18, message = "La edad mínima debe ser al menos 18 años")
    @Max(value = 80, message = "La edad mínima no puede ser mayor a 80 años")
    private Integer rangoEdadMin = 18;
    
    @Min(value = 18, message = "La edad máxima debe ser al menos 18 años")
    @Max(value = 80, message = "La edad máxima no puede ser mayor a 80 años")
    private Integer rangoEdadMax = 60;
    
    @NotNull(message = "El número máximo de participantes es obligatorio")
    @Min(value = 2, message = "Debe haber al menos 2 participantes")
    @Max(value = 20, message = "No puede haber más de 20 participantes")
    private Integer maxParticipantes;
    
    @NotEmpty(message = "Debe agregar al menos una etiqueta")
    @Size(max = 10, message = "No puede agregar más de 10 etiquetas")
    private List<String> etiquetas;
    
    // Campo auxiliar para recibir etiquetas como string separado por comas
    private String etiquetasString;
    
    // Para almacenar el JSON del itinerario
    private String diasItinerarioJson;
    
    // Para itinerario (se usará en el servicio)
    private List<DiaItinerarioDTO> diasItinerario;
    
    // Setter personalizado para procesar etiquetas desde string
    public void setEtiquetasString(String etiquetasString) {
        this.etiquetasString = etiquetasString;
        if (etiquetasString != null && !etiquetasString.trim().isEmpty()) {
            // Convertir string separado por comas a lista
            this.etiquetas = Arrays.asList(etiquetasString.split(","));
            // Limpiar espacios en blanco
            this.etiquetas = this.etiquetas.stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
        } else {
            this.etiquetas = new ArrayList<>();
        }
    }
    
    // Validación personalizada para fechas
    public boolean isFechaFinDespuesDeFechaInicio() {
        if (fechaInicio != null && fechaFin != null) {
            return fechaFin.isAfter(fechaInicio) || fechaFin.isEqual(fechaInicio);
        }
        return true;
    }
    
    // Validación para rango de edad
    public boolean isRangoEdadValido() {
        if (rangoEdadMin != null && rangoEdadMax != null) {
            return rangoEdadMax >= rangoEdadMin;
        }
        return true;
    }
}
