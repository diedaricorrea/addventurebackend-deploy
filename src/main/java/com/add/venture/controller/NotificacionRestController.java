package com.add.venture.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.add.venture.dto.ActionResponse;
import com.add.venture.model.Notificacion;
import com.add.venture.model.Usuario;
import com.add.venture.repository.NotificacionRepository;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.service.INotificacionService;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionRestController {

    @Autowired
    private INotificacionService notificacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @GetMapping
    public ResponseEntity<?> obtenerNotificaciones(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión"));
            }

            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            List<Notificacion> notificaciones = notificacionService.obtenerNotificacionesUsuario(usuario);
            long noLeidas = notificacionService.contarNotificacionesNoLeidas(usuario);

            Map<String, Object> response = new HashMap<>();
            response.put("notificaciones", notificaciones);
            response.put("total", notificaciones.size());
            response.put("noLeidas", noLeidas);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener notificaciones: " + e.getMessage()));
        }
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<?> obtenerNotificacionesNoLeidas(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión"));
            }

            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            List<Notificacion> notificaciones = notificacionService.obtenerNotificacionesNoLeidas(usuario);

            Map<String, Object> response = new HashMap<>();
            response.put("notificaciones", notificaciones);
            response.put("total", notificaciones.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener notificaciones: " + e.getMessage()));
        }
    }

    @GetMapping("/contador")
    public ResponseEntity<?> contarNoLeidas(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión"));
            }

            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            long noLeidas = notificacionService.contarNotificacionesNoLeidas(usuario);

            return ResponseEntity.ok(Map.of("contador", noLeidas));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al contar notificaciones: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<ActionResponse> marcarComoLeida(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(ActionResponse.builder()
                        .success(false)
                        .error("Debes iniciar sesión")
                        .build());
            }

            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Verificar que la notificación pertenece al usuario
            Notificacion notificacion = notificacionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

            if (!notificacion.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
                return ResponseEntity.status(403).body(ActionResponse.builder()
                        .success(false)
                        .error("No tienes permisos para marcar esta notificación")
                        .build());
            }

            notificacionService.marcarComoLeida(id);

            return ResponseEntity.ok(ActionResponse.builder()
                    .success(true)
                    .message("Notificación marcada como leída")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al marcar notificación: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/leer-todas")
    public ResponseEntity<ActionResponse> marcarTodasComoLeidas(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(ActionResponse.builder()
                        .success(false)
                        .error("Debes iniciar sesión")
                        .build());
            }

            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            notificacionService.marcarTodasComoLeidas(usuario);

            return ResponseEntity.ok(ActionResponse.builder()
                    .success(true)
                    .message("Todas las notificaciones marcadas como leídas")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al marcar notificaciones: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ActionResponse> eliminarNotificacion(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(ActionResponse.builder()
                        .success(false)
                        .error("Debes iniciar sesión")
                        .build());
            }

            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Verificar que la notificación pertenece al usuario
            Notificacion notificacion = notificacionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

            if (!notificacion.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
                return ResponseEntity.status(403).body(ActionResponse.builder()
                        .success(false)
                        .error("No tienes permisos para eliminar esta notificación")
                        .build());
            }

            notificacionRepository.delete(notificacion);

            return ResponseEntity.ok(ActionResponse.builder()
                    .success(true)
                    .message("Notificación eliminada")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al eliminar notificación: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping
    public ResponseEntity<ActionResponse> eliminarTodasLasNotificaciones(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(ActionResponse.builder()
                        .success(false)
                        .error("Debes iniciar sesión")
                        .build());
            }

            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            notificacionService.eliminarTodasLasNotificaciones(usuario);

            return ResponseEntity.ok(ActionResponse.builder()
                    .success(true)
                    .message("Todas las notificaciones eliminadas")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al eliminar notificaciones: " + e.getMessage())
                    .build());
        }
    }
}
