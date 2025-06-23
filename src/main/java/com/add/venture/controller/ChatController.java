package com.add.venture.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.add.venture.model.GrupoViaje;
import com.add.venture.model.MensajeGrupo;
import com.add.venture.model.ParticipanteGrupo;
import com.add.venture.model.ParticipanteGrupo.EstadoSolicitud;
import com.add.venture.model.Usuario;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.MensajeGrupoRepository;
import com.add.venture.repository.ParticipanteGrupoRepository;
import com.add.venture.repository.UsuarioRepository;

@Controller
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoViajeRepository grupoViajeRepository;

    @Autowired
    private ParticipanteGrupoRepository participanteGrupoRepository;

    @Autowired
    private MensajeGrupoRepository mensajeGrupoRepository;

    private final String UPLOAD_DIR = "uploads/chat/";

    @GetMapping("/grupo/{idGrupo}/mensajes")
    @ResponseBody
    public ResponseEntity<?> obtenerMensajes(@PathVariable("idGrupo") Long idGrupo) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.badRequest().body("Usuario no autenticado");
            }

            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar que el usuario es participante aceptado del grupo
            Optional<ParticipanteGrupo> participante = participanteGrupoRepository.findByUsuarioAndGrupo(usuario, grupo);
            if (participante.isEmpty() || participante.get().getEstadoSolicitud() != EstadoSolicitud.ACEPTADO) {
                return ResponseEntity.badRequest().body("No tienes acceso al chat de este grupo");
            }

            List<MensajeGrupo> mensajes = mensajeGrupoRepository.findByGrupoOrderByFechaEnvioAsc(grupo);
            return ResponseEntity.ok(mensajes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al obtener mensajes: " + e.getMessage());
        }
    }

    @PostMapping("/grupo/{idGrupo}/enviar")
    @ResponseBody
    public ResponseEntity<?> enviarMensaje(
            @PathVariable("idGrupo") Long idGrupo,
            @RequestParam("mensaje") String mensaje) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.badRequest().body("Usuario no autenticado");
            }

            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar que el usuario es participante aceptado del grupo
            Optional<ParticipanteGrupo> participante = participanteGrupoRepository.findByUsuarioAndGrupo(usuario, grupo);
            if (participante.isEmpty() || participante.get().getEstadoSolicitud() != EstadoSolicitud.ACEPTADO) {
                return ResponseEntity.badRequest().body("No tienes acceso al chat de este grupo");
            }

            if (mensaje == null || mensaje.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El mensaje no puede estar vacío");
            }

            MensajeGrupo nuevoMensaje = MensajeGrupo.builder()
                    .mensaje(mensaje.trim())
                    .grupo(grupo)
                    .remitente(usuario)
                    .fechaEnvio(LocalDateTime.now())
                    .tipoMensaje("texto")
                    .estado("activo")
                    .build();

            mensajeGrupoRepository.save(nuevoMensaje);
            return ResponseEntity.ok(nuevoMensaje);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al enviar mensaje: " + e.getMessage());
        }
    }

    @PostMapping("/grupo/{idGrupo}/enviar-imagen")
    @ResponseBody
    public ResponseEntity<?> enviarImagen(
            @PathVariable("idGrupo") Long idGrupo,
            @RequestParam("imagen") MultipartFile imagen,
            @RequestParam(value = "descripcion", required = false) String descripcion) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.badRequest().body("Usuario no autenticado");
            }

            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar que el usuario es participante aceptado del grupo
            Optional<ParticipanteGrupo> participante = participanteGrupoRepository.findByUsuarioAndGrupo(usuario, grupo);
            if (participante.isEmpty() || participante.get().getEstadoSolicitud() != EstadoSolicitud.ACEPTADO) {
                return ResponseEntity.badRequest().body("No tienes acceso al chat de este grupo");
            }

            if (imagen.isEmpty()) {
                return ResponseEntity.badRequest().body("No se ha seleccionado ninguna imagen");
            }

            // Validar tipo de archivo
            String contentType = imagen.getContentType();
            if (!contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Solo se permiten archivos de imagen");
            }

            // Generar nombre único para el archivo
            String originalFileName = imagen.getOriginalFilename();
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + fileExtension;

            // Crear directorio si no existe
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Guardar archivo
            Path filePath = uploadPath.resolve(fileName);
            imagen.transferTo(filePath.toFile());

            // Crear mensaje con imagen
            MensajeGrupo nuevoMensaje = MensajeGrupo.builder()
                    .mensaje(descripcion != null && !descripcion.trim().isEmpty() ? descripcion.trim() : "Imagen compartida")
                    .grupo(grupo)
                    .remitente(usuario)
                    .fechaEnvio(LocalDateTime.now())
                    .tipoMensaje("imagen")
                    .archivoUrl("/" + UPLOAD_DIR + fileName)
                    .archivoNombre(originalFileName)
                    .estado("activo")
                    .build();

            mensajeGrupoRepository.save(nuevoMensaje);
            return ResponseEntity.ok(nuevoMensaje);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error al guardar la imagen: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al enviar imagen: " + e.getMessage());
        }
    }

    @PostMapping("/grupo/{idGrupo}/cerrar")
    @ResponseBody
    public ResponseEntity<?> cerrarChat(@PathVariable("idGrupo") Long idGrupo) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.badRequest().body("Usuario no autenticado");
            }

            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar que el usuario es el creador del grupo
            if (!grupo.getCreador().equals(usuario)) {
                return ResponseEntity.badRequest().body("Solo el creador del grupo puede cerrar el chat");
            }

            // Cambiar estado del grupo
            grupo.setEstado("cerrado");
            grupoViajeRepository.save(grupo);

            // Los mensajes se mantienen automáticamente para historial
            return ResponseEntity.ok("Chat cerrado exitosamente. Los mensajes se han guardado en el historial.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al cerrar chat: " + e.getMessage());
        }
    }
} 