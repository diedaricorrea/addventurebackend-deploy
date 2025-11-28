package com.add.venture.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DestinoTendenciaDTO {
    private String nombre;
    private String pais;
    private String imagen;
    private Long cantidadGrupos;
    private String tag;
}
