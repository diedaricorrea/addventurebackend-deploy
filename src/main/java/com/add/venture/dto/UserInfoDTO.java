package com.add.venture.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {
    private Long id;
    private String nombreUsuario;
    private String email;
    private String nombre;
    private String apellido;
    private String iniciales;
    private String imagenPerfil;
    private List<String> roles;
}
