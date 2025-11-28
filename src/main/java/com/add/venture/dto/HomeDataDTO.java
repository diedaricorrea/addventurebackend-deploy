package com.add.venture.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeDataDTO {
    private Long idUsuario;
    private String iniciales;
    private String username;
    private String nombre;
    private String apellido;
    private String email;
    private String imagenPerfil;
    private String imagenPortada;
    private Long notificacionesNoLeidas;
    private boolean authenticated;
}
