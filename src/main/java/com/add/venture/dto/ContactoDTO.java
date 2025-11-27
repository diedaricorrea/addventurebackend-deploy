package com.add.venture.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactoDTO {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    private String email;
    
    @NotBlank(message = "El asunto es obligatorio")
    @Size(min = 3, max = 200, message = "El asunto debe tener entre 3 y 200 caracteres")
    private String asunto;
    
    @NotBlank(message = "La categoría es obligatoria")
    private String categoria;
    
    @NotBlank(message = "El mensaje es obligatorio")
    @Size(min = 10, max = 2000, message = "El mensaje debe tener entre 10 y 2000 caracteres")
    private String mensaje;

    // Constructor por defecto
    public ContactoDTO() {
    }

    // Constructor con parámetros
    public ContactoDTO(String nombre, String email, String asunto, String categoria, String mensaje) {
        this.nombre = nombre;
        this.email = email;
        this.asunto = asunto;
        this.categoria = categoria;
        this.mensaje = mensaje;
    }

    // Getters y Setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
