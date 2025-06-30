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
import com.add.venture.service.IPermisosService;

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

    @Autowired
    private IPermisosService permisosService;

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

            // Verificar que el usuario tiene permiso para acceder al chat
            if (!permisosService.usuarioTienePermiso(usuario, grupo, "ACCEDER_CHAT")) {
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

            // Verificar que el grupo no esté cerrado
            if ("cerrado".equals(grupo.getEstado()) || "concluido".equals(grupo.getEstado())) {
                return ResponseEntity.badRequest().body("No se pueden enviar mensajes en un grupo cerrado o concluido");
            }

            // Verificar que el usuario tiene permiso para enviar mensajes
            if (!permisosService.usuarioTienePermiso(usuario, grupo, "ENVIAR_MENSAJES")) {
                return ResponseEntity.badRequest().body("No tienes permiso para enviar mensajes en este grupo");
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

            // Verificar que el grupo no esté cerrado
            if ("cerrado".equals(grupo.getEstado()) || "concluido".equals(grupo.getEstado())) {
                System.out.println("ERROR: Grupo cerrado o concluido");
                return ResponseEntity.badRequest().body("No se pueden enviar archivos en un grupo cerrado o concluido");
            }

            // Verificar que el usuario tiene permiso para compartir archivos
            if (!permisosService.usuarioTienePermiso(usuario, grupo, "COMPARTIR_ARCHIVOS")) {
                System.out.println("ERROR: Usuario no tiene permiso para compartir archivos");
                return ResponseEntity.badRequest().body("No tienes permiso para compartir archivos en este grupo");
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

    @PostMapping("/grupo/{idGrupo}/eliminar-mensaje/{idMensaje}")
    @ResponseBody
    public ResponseEntity<?> eliminarMensaje(
            @PathVariable("idGrupo") Long idGrupo,
            @PathVariable("idMensaje") Long idMensaje) {
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

            // Buscar el mensaje
            Optional<MensajeGrupo> mensajeOpt = mensajeGrupoRepository.findById(idMensaje);
            if (mensajeOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Mensaje no encontrado");
            }

            MensajeGrupo mensaje = mensajeOpt.get();

            // Verificar que el mensaje pertenece al grupo correcto
            if (!mensaje.getGrupo().getIdGrupo().equals(idGrupo)) {
                return ResponseEntity.badRequest().body("El mensaje no pertenece a este grupo");
            }

            // Verificar que el usuario es el remitente del mensaje o el creador del grupo
            if (!mensaje.getRemitente().equals(usuario) && !grupo.getCreador().equals(usuario)) {
                return ResponseEntity.badRequest().body("Solo puedes eliminar tus propios mensajes o ser el creador del grupo");
            }

            // Eliminar archivo si es una imagen
            if ("imagen".equals(mensaje.getTipoMensaje()) && mensaje.getArchivoUrl() != null) {
                try {
                    Path projectRoot = Paths.get("").toAbsolutePath();
                    Path filePath = projectRoot.resolve(mensaje.getArchivoUrl().substring(1)); // Quitar el "/" inicial
                    Files.deleteIfExists(filePath);
                } catch (Exception e) {
                    System.out.println("Error al eliminar archivo: " + e.getMessage());
                }
            }

            // Eliminar mensaje de la base de datos
            mensajeGrupoRepository.delete(mensaje);
            
            // Notificar por WebSocket que el mensaje fue eliminado
            messagingTemplate.convertAndSend("/topic/grupo/" + idGrupo + "/delete", idMensaje);
            
            return ResponseEntity.ok("Mensaje eliminado exitosamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar mensaje: " + e.getMessage());
        }
    }

    @GetMapping("/debug/permisos/{idGrupo}")
    @ResponseBody
    public ResponseEntity<?> debugPermisos(@PathVariable("idGrupo") Long idGrupo) {
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

            // Crear respuesta de depuración
            StringBuilder debug = new StringBuilder();
            debug.append("=== DEBUG PERMISOS ===\n");
            debug.append("Usuario: ").append(usuario.getEmail()).append(" (ID: ").append(usuario.getIdUsuario()).append(")\n");
            debug.append("Grupo: ").append(grupo.getNombreViaje()).append(" (ID: ").append(grupo.getIdGrupo()).append(")\n");
            debug.append("Creador del grupo: ").append(grupo.getCreador().getEmail()).append(" (ID: ").append(grupo.getCreador().getIdUsuario()).append(")\n");
            debug.append("¿Es creador? ").append(permisosService.esCreadorDelGrupo(usuario, grupo)).append("\n");
            
            // Verificar rol en UsuarioRolGrupo
            var rol = permisosService.obtenerRolEnGrupo(usuario, grupo);
            debug.append("Rol en grupo: ").append(rol != null ? rol.getNombreRol() : "NINGUNO").append("\n");
            
            // Verificar permisos específicos
            String[] permisosAVerificar = {"ACCEDER_CHAT", "ENVIAR_MENSAJES", "COMPARTIR_ARCHIVOS", "EDITAR_GRUPO"};
            debug.append("\nPermisos:\n");
            for (String permiso : permisosAVerificar) {
                boolean tiene = permisosService.usuarioTienePermiso(usuario, grupo, permiso);
                debug.append("- ").append(permiso).append(": ").append(tiene ? "✅ SÍ" : "❌ NO").append("\n");
            }
            
            return ResponseEntity.ok(debug.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error en debug: " + e.getMessage());
        }
    }

    @PostMapping("/fix/roles-creadores")
    @ResponseBody
    public ResponseEntity<?> corregirRolesCreadores() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.badRequest().body("Usuario no autenticado");
            }

            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            StringBuilder resultado = new StringBuilder();
            resultado.append("=== CORRECCIÓN DE ROLES DE CREADORES ===\n");

            // Buscar todos los grupos donde este usuario es creador
            List<GrupoViaje> gruposCreados = grupoViajeRepository.findByCreador(usuario);
            resultado.append("Grupos encontrados donde eres creador: ").append(gruposCreados.size()).append("\n\n");

            int rolesAsignados = 0;

            for (GrupoViaje grupo : gruposCreados) {
                resultado.append("Grupo: ").append(grupo.getNombreViaje()).append("\n");
                
                // Verificar si ya tiene rol
                var rolActual = permisosService.obtenerRolEnGrupo(usuario, grupo);
                resultado.append("- Rol actual: ").append(rolActual != null ? rolActual.getNombreRol() : "NINGUNO").append("\n");
                
                if (rolActual == null || !rolActual.getNombreRol().equals("LÍDER_GRUPO")) {
                    try {
                        // Asignar rol de líder
                        permisosService.asignarRolLiderCreador(usuario, grupo);
                        rolesAsignados++;
                        resultado.append("- ✅ LÍDER_GRUPO asignado correctamente\n");
                    } catch (Exception e) {
                        resultado.append("- ❌ Error al asignar rol: ").append(e.getMessage()).append("\n");
                    }
                } else {
                    resultado.append("- ✅ Ya tiene rol de LÍDER_GRUPO\n");
                }
                resultado.append("\n");
            }

            resultado.append("=== RESUMEN ===\n");
            resultado.append("Roles asignados: ").append(rolesAsignados).append("\n");
            resultado.append("Corrección completada.\n");

            return ResponseEntity.ok(resultado.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error en corrección: " + e.getMessage());
        }
    }

    @GetMapping("/test/creador/{idGrupo}")
    @ResponseBody
    public ResponseEntity<?> testCreador(@PathVariable("idGrupo") Long idGrupo) {
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

            StringBuilder resultado = new StringBuilder();
            resultado.append("=== TEST CREADOR ===\n");
            
            // Test 1: ¿Es creador?
            resultado.append("\n--- TEST 1: ¿Es creador? ---\n");
            boolean esCreador = permisosService.esCreadorDelGrupo(usuario, grupo);
            resultado.append("Resultado: ").append(esCreador).append("\n");
            
            // Test 2: ¿Puede acceder al chat?
            resultado.append("\n--- TEST 2: ¿Puede acceder al chat? ---\n");
            boolean puedeAcceder = permisosService.usuarioTienePermiso(usuario, grupo, "ACCEDER_CHAT");
            resultado.append("Resultado: ").append(puedeAcceder).append("\n");
            
            // Test 3: ¿Puede enviar mensajes?
            resultado.append("\n--- TEST 3: ¿Puede enviar mensajes? ---\n");
            boolean puedeEnviar = permisosService.usuarioTienePermiso(usuario, grupo, "ENVIAR_MENSAJES");
            resultado.append("Resultado: ").append(puedeEnviar).append("\n");

            resultado.append("\n=== RESUMEN ===\n");
            resultado.append("Usuario: ").append(usuario.getEmail()).append("\n");
            resultado.append("Grupo: ").append(grupo.getNombreViaje()).append("\n");
            resultado.append("Es creador: ").append(esCreador).append("\n");
            resultado.append("Puede acceder: ").append(puedeAcceder).append("\n");
            resultado.append("Puede enviar: ").append(puedeEnviar).append("\n");
            
            return ResponseEntity.ok(resultado.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error en test: " + e.getMessage());
        }
    }

} 