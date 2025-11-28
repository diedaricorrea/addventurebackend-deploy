package com.add.venture.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private final String UPLOAD_DIR = "uploads/chat";

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

            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar que el grupo no esté cerrado
            if ("cerrado".equals(grupo.getEstado()) || "concluido".equals(grupo.getEstado())) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("No se pueden enviar archivos en un grupo cerrado o concluido")
                        .build());
            }

            // Verificar permisos
            if (!permisosService.usuarioTienePermiso(usuario, grupo, "COMPARTIR_ARCHIVOS")) {
                return ResponseEntity.status(403).body(ActionResponse.builder()
                        .success(false)
                        .error("No tienes permiso para compartir archivos en este grupo")
                        .build());
            }

            if (imagen.isEmpty()) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("No se ha seleccionado ninguna imagen")
                        .build());
            }

            // Validar tipo de archivo
            String contentType = imagen.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("Solo se permiten archivos de imagen")
                        .build());
            }

            // Validar tamaño (máximo 5MB)
            if (imagen.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("La imagen es demasiado grande. Máximo 5MB")
                        .build());
            }

            // Generar nombre único para el archivo
            String originalFileName = imagen.getOriginalFilename();
            if (originalFileName == null || !originalFileName.contains(".")) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("Nombre de archivo inválido")
                        .build());
            }

            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + fileExtension;

            // Crear directorio si no existe
            Path projectRoot = Paths.get("").toAbsolutePath();
            Path uploadPath = projectRoot.resolve(UPLOAD_DIR);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Guardar archivo
            Path filePath = uploadPath.resolve(fileName);
            imagen.transferTo(filePath.toFile());

            // Crear mensaje con imagen
            MensajeGrupo nuevoMensaje = MensajeGrupo.builder()
                    .mensaje(descripcion != null && !descripcion.trim().isEmpty() 
                        ? descripcion.trim() 
                        : "Imagen compartida")
                    .grupo(grupo)
                    .remitente(usuario)
                    .fechaEnvio(LocalDateTime.now())
                    .tipoMensaje("imagen")
                    .archivoUrl("/" + UPLOAD_DIR + "/" + fileName)
                    .archivoNombre(originalFileName)
                    .estado("activo")
                    .build();

            mensajeGrupoRepository.save(nuevoMensaje);
            
            // Enviar por WebSocket
            messagingTemplate.convertAndSend("/topic/grupo/" + idGrupo, nuevoMensaje);

            return ResponseEntity.ok(nuevoMensaje);

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
