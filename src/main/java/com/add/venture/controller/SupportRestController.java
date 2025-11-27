package com.add.venture.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.add.venture.dto.ContactoDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/support")
@CrossOrigin(origins = "http://localhost:4200")
public class SupportRestController {

    @PostMapping("/contacto")
    public ResponseEntity<?> enviarMensajeContacto(@Valid @RequestBody ContactoDTO contacto) {
        try {
            // Aquí puedes guardar el mensaje, enviarlo por correo, etc.
            System.out.println("Mensaje recibido de: " + contacto.getNombre());
            System.out.println("Email: " + contacto.getEmail());
            System.out.println("Asunto: " + contacto.getAsunto());
            System.out.println("Categoría: " + contacto.getCategoria());
            System.out.println("Mensaje: " + contacto.getMensaje());

            return ResponseEntity.ok(Map.of(
                "mensaje", "Tu mensaje ha sido enviado correctamente. Te responderemos a la brevedad.",
                "success", true
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Error al enviar el mensaje: " + e.getMessage(),
                "success", false
            ));
        }
    }
}
