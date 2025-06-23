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
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final String UPLOAD_DIR = "uploads/chat";

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
            
            // Enviar mensaje por WebSocket a todos los participantes del grupo
            messagingTemplate.convertAndSend("/topic/grupo/" + idGrupo, nuevoMensaje);
            
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
            System.out.println("=== INICIO ENVIAR IMAGEN ===");
            System.out.println("Recibida imagen: " + imagen.getOriginalFilename() + ", Tamaño: " + imagen.getSize() + ", Tipo: " + imagen.getContentType());
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                System.out.println("ERROR: Usuario no autenticado");
                return ResponseEntity.badRequest().body("Usuario no autenticado");
            }

            System.out.println("Usuario autenticado: " + auth.getName());

            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            System.out.println("Usuario encontrado: " + usuario.getNombre());

            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            System.out.println("Grupo encontrado: " + grupo.getNombreViaje());

            // Verificar que el usuario es participante aceptado del grupo
            Optional<ParticipanteGrupo> participante = participanteGrupoRepository.findByUsuarioAndGrupo(usuario, grupo);
            if (participante.isEmpty() || participante.get().getEstadoSolicitud() != EstadoSolicitud.ACEPTADO) {
                System.out.println("ERROR: Usuario no tiene acceso al chat");
                return ResponseEntity.badRequest().body("No tienes acceso al chat de este grupo");
            }

            System.out.println("Usuario tiene acceso al chat");

            if (imagen.isEmpty()) {
                System.out.println("ERROR: Imagen vacía");
                return ResponseEntity.badRequest().body("No se ha seleccionado ninguna imagen");
            }

            // Validar tipo de archivo
            String contentType = imagen.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                System.out.println("ERROR: Tipo de archivo inválido: " + contentType);
                return ResponseEntity.badRequest().body("Solo se permiten archivos de imagen. Tipo recibido: " + contentType);
            }

            System.out.println("Tipo de archivo válido: " + contentType);

            // Generar nombre único para el archivo
            String originalFileName = imagen.getOriginalFilename();
            if (originalFileName == null || originalFileName.isEmpty()) {
                System.out.println("ERROR: Nombre de archivo nulo o vacío");
                return ResponseEntity.badRequest().body("Nombre de archivo inválido");
            }
            
            if (!originalFileName.contains(".")) {
                System.out.println("ERROR: Archivo sin extensión");
                return ResponseEntity.badRequest().body("Archivo sin extensión");
            }
            
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + fileExtension;

            System.out.println("Nombre de archivo generado: " + fileName);

            // Crear directorio si no existe - usando ruta absoluta
            Path projectRoot = Paths.get("").toAbsolutePath();
            Path uploadPath = projectRoot.resolve(UPLOAD_DIR);
            System.out.println("Ruta de upload: " + uploadPath.toAbsolutePath());
            
            if (!Files.exists(uploadPath)) {
                System.out.println("Creando directorio: " + uploadPath);
                Files.createDirectories(uploadPath);
            }

            // Guardar archivo
            Path filePath = uploadPath.resolve(fileName);
            System.out.println("Guardando archivo en: " + filePath.toAbsolutePath());
            
            imagen.transferTo(filePath.toFile());
            
            System.out.println("Archivo guardado exitosamente");

            // Crear mensaje con imagen
            MensajeGrupo nuevoMensaje = MensajeGrupo.builder()
                    .mensaje(descripcion != null && !descripcion.trim().isEmpty() ? descripcion.trim() : "Imagen compartida")
                    .grupo(grupo)
                    .remitente(usuario)
                    .fechaEnvio(LocalDateTime.now())
                    .tipoMensaje("imagen")
                    .archivoUrl("/" + UPLOAD_DIR + "/" + fileName)
                    .archivoNombre(originalFileName)
                    .estado("activo")
                    .build();

            System.out.println("Guardando mensaje en base de datos...");
            mensajeGrupoRepository.save(nuevoMensaje);
            System.out.println("Mensaje guardado con ID: " + nuevoMensaje.getIdMensaje());
            
            // Enviar mensaje por WebSocket a todos los participantes del grupo
            System.out.println("Enviando por WebSocket...");
            messagingTemplate.convertAndSend("/topic/grupo/" + idGrupo, nuevoMensaje);
            System.out.println("Mensaje enviado por WebSocket exitosamente");
            
            System.out.println("=== FIN ENVIAR IMAGEN EXITOSO ===");
            return ResponseEntity.ok(nuevoMensaje);
        } catch (IOException e) {
            System.out.println("ERROR IOException: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error al guardar la imagen: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("ERROR Exception: " + e.getMessage());
            e.printStackTrace();
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