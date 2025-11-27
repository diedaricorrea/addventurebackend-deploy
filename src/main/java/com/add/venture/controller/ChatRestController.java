package com.add.venture.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.add.venture.dto.ActionResponse;
import com.add.venture.model.GrupoViaje;
import com.add.venture.model.MensajeGrupo;
import com.add.venture.model.Usuario;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.MensajeGrupoRepository;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.service.IPermisosService;

@RestController
@RequestMapping("/api/chat/grupo")
public class ChatRestController {

    @Autowired
    private MensajeGrupoRepository mensajeGrupoRepository;

    @Autowired
    private GrupoViajeRepository grupoViajeRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private IPermisosService permisosService;

    @GetMapping("/{idGrupo}/mensajes")
    public ResponseEntity<?> obtenerMensajes(@PathVariable Long idGrupo, Authentication authentication) {
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

            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar que el usuario tenga permiso para acceder al chat
            if (!permisosService.usuarioTienePermiso(usuario, grupo, "ACCEDER_CHAT")) {
                return ResponseEntity.status(403).body(ActionResponse.builder()
                        .success(false)
                        .error("No tienes permiso para acceder al chat de este grupo")
                        .build());
            }

            List<MensajeGrupo> mensajes = mensajeGrupoRepository
                    .findByGrupoOrderByFechaEnvioAsc(grupo);

            return ResponseEntity.ok(mensajes);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al cargar mensajes: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/{idGrupo}/enviar")
    public ResponseEntity<?> enviarMensaje(
            @PathVariable Long idGrupo,
            @RequestParam String mensaje,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(ActionResponse.builder()
                        .success(false)
                        .error("Debes iniciar sesión")
                        .build());
            }

            // Buscar usuario y grupo
            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar que el usuario tenga permiso para enviar mensajes
            if (!permisosService.usuarioTienePermiso(usuario, grupo, "ENVIAR_MENSAJES")) {
                return ResponseEntity.status(403).body(ActionResponse.builder()
                        .success(false)
                        .error("No tienes permiso para enviar mensajes en este grupo")
                        .build());
            }

            // Crear el mensaje
            MensajeGrupo nuevoMensaje = new MensajeGrupo();
            nuevoMensaje.setGrupo(grupo);
            nuevoMensaje.setRemitente(usuario);
            nuevoMensaje.setMensaje(mensaje);
            nuevoMensaje.setTipoMensaje("texto");
            nuevoMensaje.setFechaEnvio(LocalDateTime.now());

            // Guardar en base de datos
            nuevoMensaje = mensajeGrupoRepository.save(nuevoMensaje);

            // Enviar por WebSocket a todos los suscritos al grupo
            messagingTemplate.convertAndSend("/topic/grupo/" + idGrupo, nuevoMensaje);

            return ResponseEntity.ok(nuevoMensaje);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al enviar mensaje: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/{idGrupo}/enviar-imagen")
    public ResponseEntity<?> enviarImagen(
            @PathVariable Long idGrupo,
            @RequestParam("imagen") MultipartFile imagen,
            @RequestParam(required = false) String descripcion,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(ActionResponse.builder()
                        .success(false)
                        .error("Debes iniciar sesión")
                        .build());
            }

            // Validar tamaño de imagen (máximo 5MB)
            if (imagen.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("La imagen es demasiado grande. Máximo 5MB")
                        .build());
            }

            // Aquí iría la lógica de subida y guardado de imagen
            Map<String, Object> response = new HashMap<>();
            response.put("idMensaje", 1L);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al enviar imagen: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{idGrupo}/mensaje/{idMensaje}")
    public ResponseEntity<?> eliminarMensaje(
            @PathVariable Long idGrupo,
            @PathVariable Long idMensaje,
            Authentication authentication) {
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

            MensajeGrupo mensaje = mensajeGrupoRepository.findById(idMensaje)
                    .orElseThrow(() -> new RuntimeException("Mensaje no encontrado"));

            GrupoViaje grupo = mensaje.getGrupo();

            // Verificar que el mensaje pertenece al grupo indicado
            if (!grupo.getIdGrupo().equals(idGrupo)) {
                return ResponseEntity.status(403).body(ActionResponse.builder()
                        .success(false)
                        .error("El mensaje no pertenece a este grupo")
                        .build());
            }

            // Verificar que el usuario es el creador del mensaje o tiene permiso para eliminar
            boolean esCreadorMensaje = mensaje.getRemitente().getIdUsuario().equals(usuario.getIdUsuario());
            boolean tienePermisoEliminar = permisosService.usuarioTienePermiso(usuario, grupo, "ELIMINAR_MENSAJES");

            if (!esCreadorMensaje && !tienePermisoEliminar) {
                return ResponseEntity.status(403).body(ActionResponse.builder()
                        .success(false)
                        .error("No tienes permiso para eliminar este mensaje")
                        .build());
            }

            // Eliminar el mensaje
            mensajeGrupoRepository.delete(mensaje);

            // Notificar por WebSocket la eliminación del mensaje
            Map<String, Object> deleteNotification = new HashMap<>();
            deleteNotification.put("action", "delete");
            deleteNotification.put("idMensaje", idMensaje);
            messagingTemplate.convertAndSend("/topic/grupo/" + idGrupo + "/delete", deleteNotification);

            return ResponseEntity.ok(ActionResponse.builder()
                    .success(true)
                    .message("Mensaje eliminado exitosamente")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al eliminar mensaje: " + e.getMessage())
                    .build());
        }
    }
}
