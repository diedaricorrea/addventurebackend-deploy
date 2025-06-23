package com.add.venture.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.add.venture.dto.CrearGrupoViajeDTO;
import com.add.venture.helper.UsuarioAutenticadoHelper;
import com.add.venture.model.GrupoViaje;
import com.add.venture.model.MensajeGrupo;
import com.add.venture.model.ParticipanteGrupo;
import com.add.venture.model.ParticipanteGrupo.EstadoSolicitud;
import com.add.venture.model.Usuario;
import com.add.venture.model.Viaje;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.MensajeGrupoRepository;
import com.add.venture.repository.ParticipanteGrupoRepository;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.repository.ViajeRepository;
import com.add.venture.service.IGrupoViajeService;
import com.add.venture.service.INotificacionService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/grupos")
public class GrupoViajeController {

    @Autowired
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @Autowired
    private GrupoViajeRepository grupoViajeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ParticipanteGrupoRepository participanteGrupoRepository;

    @Autowired
    private MensajeGrupoRepository mensajeGrupoRepository;

    @Autowired
    private IGrupoViajeService grupoViajeService;

    @Autowired
    private ViajeRepository viajeRepository;

    @Autowired
    private INotificacionService notificacionService;

    @GetMapping
    public String listarGrupos(
            @RequestParam(required = false) String destino,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long tipoViaje,
            @RequestParam(required = false) String rangoEdad,
            @RequestParam(required = false) Boolean verificado,
            @RequestParam(required = false) String etiquetas,
            @RequestParam(required = false) String ordenar,
            Model model) {

        // Cargar datos del usuario para la navbar y perfil
        usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);

        // Cargar solo grupos activos
        List<GrupoViaje> todosLosGrupos = grupoViajeRepository.findByEstado("activo");
        
        // Filtrar grupos que aún tienen cupo disponible
        List<GrupoViaje> gruposConCupo = new ArrayList<>();
        
        for (GrupoViaje grupo : todosLosGrupos) {
            // Contar participantes aceptados (sin incluir al creador)
            long participantesAceptados = participanteGrupoRepository.countByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.ACEPTADO);
            
            // Solo incluir grupos que aún tienen cupo (participantes + creador < maxParticipantes)
            if ((participantesAceptados + 1) < grupo.getMaxParticipantes()) {
                // Filtrar solo participantes aceptados para la visualización
                List<ParticipanteGrupo> participantesAceptadosList = participanteGrupoRepository
                        .findByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.ACEPTADO);
                
                // Convertir lista a set y reemplazar la lista de participantes con solo los aceptados
                grupo.setParticipantes(new java.util.HashSet<>(participantesAceptadosList));
                
                gruposConCupo.add(grupo);
            }
        }
        
        model.addAttribute("grupos", gruposConCupo);

        // Cargar tipos de viaje para los filtros
        model.addAttribute("tiposViaje", grupoViajeService.obtenerTiposViaje());

        return "grupos/buscar";
    }

    @GetMapping("/{id}")
    public String verDetallesGrupo(@PathVariable("id") Long idGrupo, Model model) {
        // Cargar datos del usuario para la navbar y perfil
        usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);

        // Cargar el grupo
        GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        model.addAttribute("grupo", grupo);

        // Verificar si el usuario autenticado es participante del grupo
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Agregar usuarioId para WebSocket global
            model.addAttribute("usuarioId", usuario.getIdUsuario());

            // Verificar si es participante ACEPTADO
            Optional<ParticipanteGrupo> participante = participanteGrupoRepository.findByUsuarioAndGrupo(usuario, grupo);
            boolean isParticipante = participante.isPresent() && 
                                   participante.get().getEstadoSolicitud() == EstadoSolicitud.ACEPTADO;
            model.addAttribute("isParticipante", isParticipante);
            
            // Verificar estado de solicitud si existe
            if (participante.isPresent()) {
                model.addAttribute("estadoSolicitud", participante.get().getEstadoSolicitud().name());
            } else {
                model.addAttribute("estadoSolicitud", "NINGUNA");
            }
        } else {
            model.addAttribute("isParticipante", false);
            model.addAttribute("estadoSolicitud", "NINGUNA");
        }
        
        // Contar solo participantes aceptados (sin incluir al creador ya que se cuenta aparte)
        long participantesAceptados = participanteGrupoRepository.countByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.ACEPTADO);
        model.addAttribute("participantesAceptados", participantesAceptados);
        
        // Total de miembros = participantes aceptados + creador
        model.addAttribute("totalMiembros", participantesAceptados + 1);
        
        // Cargar solo participantes con estado ACEPTADO
        List<ParticipanteGrupo> participantesAceptadosList = participanteGrupoRepository
                .findByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.ACEPTADO);
        model.addAttribute("participantesAceptadosList", participantesAceptadosList);

        return "grupos/detalles";
    }

    @PostMapping("/{id}/unirse")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unirseAlGrupo(@PathVariable("id") Long idGrupo) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Obtener el usuario autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                response.put("success", false);
                response.put("error", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Obtener el grupo
            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar que no sea el creador
            if (grupo.getCreador().equals(usuario)) {
                response.put("success", false);
                response.put("error", "El creador no puede unirse como participante");
                return ResponseEntity.badRequest().body(response);
            }

            // Verificar capacidad del grupo (sin contar al creador)
            List<ParticipanteGrupo> participantesAceptados = participanteGrupoRepository.findByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.ACEPTADO);
            if (participantesAceptados.size() >= grupo.getMaxParticipantes() - 1) {
                response.put("success", false);
                response.put("error", "El grupo ha alcanzado su capacidad máxima");
                return ResponseEntity.badRequest().body(response);
            }

            // Verificar si ya tiene solicitud
            Optional<ParticipanteGrupo> participanteExistente = participanteGrupoRepository.findByUsuarioAndGrupo(usuario, grupo);
            if (participanteExistente.isPresent()) {
                ParticipanteGrupo participante = participanteExistente.get();
                
                if (participante.getEstadoSolicitud() == EstadoSolicitud.ACEPTADO) {
                    response.put("success", false);
                    response.put("error", "Ya eres miembro de este grupo");
                    return ResponseEntity.badRequest().body(response);
                }
                
                if (participante.getEstadoSolicitud() == EstadoSolicitud.PENDIENTE) {
                    response.put("success", false);
                    response.put("error", "Ya tienes una solicitud pendiente para este grupo");
                    return ResponseEntity.badRequest().body(response);
                }
                
                if (participante.getEstadoSolicitud() == EstadoSolicitud.RECHAZADO) {
                    int intentos = participante.getIntentosSolicitud();
                    if (intentos >= 3) {
                        response.put("success", false);
                        response.put("error", "Has alcanzado el límite máximo de intentos para este grupo (3/3)");
                        return ResponseEntity.badRequest().body(response);
                    }
                    
                    // Permitir reenvío de solicitud
                    participante.setEstadoSolicitud(EstadoSolicitud.PENDIENTE);
                    participante.setFechaUnion(LocalDateTime.now());
                    participante.setIntentosSolicitud(intentos + 1);
                    participanteGrupoRepository.save(participante);
                    
                    // Crear notificación para el líder del grupo
                    notificacionService.crearNotificacionSolicitudUnion(usuario, grupo.getCreador(), 
                            grupo.getIdGrupo(), grupo.getNombreViaje());
                    
                    response.put("success", true);
                    response.put("message", "Nueva solicitud enviada al líder del grupo (Intento " + (intentos + 1) + " de 3)");
                    return ResponseEntity.ok(response);
                }
            }

            // Crear solicitud de participante
            ParticipanteGrupo solicitud = ParticipanteGrupo.builder()
                    .usuario(usuario)
                    .grupo(grupo)
                    .rolParticipante("MIEMBRO")
                    .estadoSolicitud(EstadoSolicitud.PENDIENTE)
                    .fechaUnion(LocalDateTime.now())
                    .build();

            participanteGrupoRepository.save(solicitud);

            // Crear notificación para el líder del grupo
            notificacionService.crearNotificacionSolicitudUnion(usuario, grupo.getCreador(), 
                    idGrupo, grupo.getNombreViaje());

            response.put("success", true);
            response.put("message", "Solicitud enviada al líder del grupo");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{id}/abandonar")
    public String abandonarGrupo(@PathVariable("id") Long idGrupo, RedirectAttributes redirectAttributes) {
        // Obtener el usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            redirectAttributes.addFlashAttribute("error", "Usuario no autenticado");
            return "redirect:/grupos/" + idGrupo;
        }

        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener el grupo
        GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        // Verificar si es participante
        Optional<ParticipanteGrupo> participanteOpt = participanteGrupoRepository.findByUsuarioAndGrupo(usuario, grupo);
        if (participanteOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No eres participante de este grupo");
            return "redirect:/grupos/" + idGrupo;
        }

        // Verificar que no sea el creador
        if (grupo.getCreador().equals(usuario)) {
            redirectAttributes.addFlashAttribute("error", "El creador no puede abandonar el grupo");
            return "redirect:/grupos/" + idGrupo;
        }

        // Eliminar participante
        participanteGrupoRepository.delete(participanteOpt.get());

        redirectAttributes.addFlashAttribute("mensaje", "Has abandonado el grupo exitosamente");
        return "redirect:/grupos";
    }

    @GetMapping("/{id}/historial-chat")
    public String verHistorialChat(@PathVariable("id") Long idGrupo, Model model) {
        usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }

        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        // Verificar que el usuario fue participante del grupo
        Optional<ParticipanteGrupo> participante = participanteGrupoRepository.findByUsuarioAndGrupo(usuario, grupo);
        if (participante.isEmpty() || participante.get().getEstadoSolicitud() != EstadoSolicitud.ACEPTADO) {
            return "redirect:/grupos";
        }

        // Obtener mensajes del chat
        List<MensajeGrupo> mensajes = mensajeGrupoRepository.findByGrupoOrderByFechaEnvioAsc(grupo);

        model.addAttribute("grupo", grupo);
        model.addAttribute("mensajes", mensajes);
        model.addAttribute("esHistorial", true);

        return "grupos/historial-chat";
    }

    @PostMapping("/{id}/expulsar")
    public String expulsarMiembro(
            @PathVariable("id") Long idGrupo,
            @RequestParam("userId") Long userId,
            @RequestParam("expulsionReason") String expulsionReason,
            @RequestParam(value = "expulsionComment", required = false) String expulsionComment,
            RedirectAttributes redirectAttributes) {

        // Obtener el usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            redirectAttributes.addFlashAttribute("error", "Usuario no autenticado");
            return "redirect:/grupos/" + idGrupo;
        }

        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener el grupo
        GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        // Verificar que el usuario autenticado sea el creador del grupo
        if (!grupo.getCreador().equals(usuario)) {
            redirectAttributes.addFlashAttribute("error", "Solo el creador puede expulsar miembros");
            return "redirect:/grupos/" + idGrupo;
        }

        // Obtener el usuario a expulsar
        Usuario usuarioExpulsado = usuarioRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario a expulsar no encontrado"));

        // Verificar que no sea el creador
        if (grupo.getCreador().equals(usuarioExpulsado)) {
            redirectAttributes.addFlashAttribute("error", "No puedes expulsar al creador del grupo");
            return "redirect:/grupos/" + idGrupo;
        }

        // Verificar si es participante
        Optional<ParticipanteGrupo> participanteOpt = participanteGrupoRepository
                .findByUsuarioAndGrupo(usuarioExpulsado, grupo);
        if (participanteOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El usuario no es participante de este grupo");
            return "redirect:/grupos/" + idGrupo;
        }

        // Eliminar participante
        participanteGrupoRepository.delete(participanteOpt.get());

        // Aquí se podría enviar una notificación al usuario expulsado

        redirectAttributes.addFlashAttribute("mensaje", "Usuario expulsado exitosamente");
        return "redirect:/grupos/" + idGrupo;
    }

    @PostMapping("/{id}/mensaje")
    @ResponseBody
    public String enviarMensaje(
            @PathVariable("id") Long idGrupo,
            @RequestBody MensajeRequest mensajeRequest) {

        // Obtener el usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "{\"error\": \"Usuario no autenticado\"}";
        }

        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener el grupo
        GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        // Verificar si es participante
        if (!participanteGrupoRepository.existsByUsuarioAndGrupo(usuario, grupo)) {
            return "{\"error\": \"No eres participante de este grupo\"}";
        }

        // Crear mensaje
        MensajeGrupo mensaje = new MensajeGrupo();
        mensaje.setMensaje(mensajeRequest.getMensaje());
        mensaje.setFechaEnvio(LocalDateTime.now());
        mensaje.setEstado("activo");
        mensaje.setGrupo(grupo);
        mensaje.setRemitente(usuario);

        mensajeGrupoRepository.save(mensaje);

        return "{\"success\": true}";
    }

    // Clase para recibir el mensaje
    public static class MensajeRequest {
        private String mensaje;

        public String getMensaje() {
            return mensaje;
        }

        public void setMensaje(String mensaje) {
            this.mensaje = mensaje;
        }
    }

    // Método para mostrar los viajes del usuario autenticado
    @GetMapping("/mis-viajes")
    public String mostrarMisViajes(Model model) {
        usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);

        // Obtener el usuario autenticado usando el SecurityContextHolder
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            model.addAttribute("error", "Usuario no autenticado");
            model.addAttribute("gruposCreados", List.of());
            model.addAttribute("gruposUnidos", List.of());
            model.addAttribute("gruposCerrados", List.of());
        } else {
            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Agregar usuarioId para WebSocket global
            model.addAttribute("usuarioId", usuario.getIdUsuario());

            // 1. Grupos creados por el usuario
            List<GrupoViaje> gruposCreados = grupoViajeRepository.findByCreador(usuario);
            
            // 2. Grupos donde el usuario es participante (no creador)
            List<ParticipanteGrupo> participaciones = participanteGrupoRepository
                    .findByUsuarioAndEstadoSolicitud(usuario, EstadoSolicitud.ACEPTADO);
            
            List<GrupoViaje> gruposUnidos = participaciones.stream()
                    .map(ParticipanteGrupo::getGrupo)
                    .filter(grupo -> !grupo.getCreador().equals(usuario)) // Excluir grupos propios
                    .collect(Collectors.toList());
            
            // 3. Separar grupos activos y cerrados
            List<GrupoViaje> gruposActivosCreados = gruposCreados.stream()
                    .filter(grupo -> "activo".equals(grupo.getEstado()))
                    .collect(Collectors.toList());
            
            List<GrupoViaje> gruposActivosUnidos = gruposUnidos.stream()
                    .filter(grupo -> "activo".equals(grupo.getEstado()))
                    .collect(Collectors.toList());
            
            List<GrupoViaje> gruposCerrados = gruposCreados.stream()
                    .filter(grupo -> "cerrado".equals(grupo.getEstado()) || "concluido".equals(grupo.getEstado()))
                    .collect(Collectors.toList());
            
            // Agregar también grupos unidos que estén cerrados
            List<GrupoViaje> gruposUnidosCerrados = gruposUnidos.stream()
                    .filter(grupo -> "cerrado".equals(grupo.getEstado()) || "concluido".equals(grupo.getEstado()))
                    .collect(Collectors.toList());
            
            gruposCerrados.addAll(gruposUnidosCerrados);

            model.addAttribute("gruposCreados", gruposActivosCreados);
            model.addAttribute("gruposUnidos", gruposActivosUnidos);
            model.addAttribute("gruposCerrados", gruposCerrados);
            model.addAttribute("totalGrupos", gruposCreados.size() + gruposUnidos.size());
        }

        model.addAttribute("tiposViaje", grupoViajeService.obtenerTiposViaje());

        return "grupos/mis-viajes";
    }

    @GetMapping("/editar/{id}")
    public String editarGrupo(@PathVariable("id") Long idGrupo, Model model) {
        usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);

        // Obtener el grupo
        GrupoViaje grupo = grupoViajeService.buscarGrupoPorId(idGrupo);

        // Verificar permisos
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (!grupo.getCreador().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new AccessDeniedException("No tienes permiso para editar este grupo");
        }

        // Convertir entidad a DTO
        CrearGrupoViajeDTO dto = new CrearGrupoViajeDTO();
        dto.setNombreViaje(grupo.getNombreViaje());
        
        if (grupo.getViaje() != null) {
            dto.setDestinoPrincipal(grupo.getViaje().getDestinoPrincipal());
            dto.setFechaInicio(grupo.getViaje().getFechaInicio());
            dto.setFechaFin(grupo.getViaje().getFechaFin());
            dto.setDescripcion(grupo.getViaje().getDescripcion());
            dto.setPuntoEncuentro(grupo.getViaje().getPuntoEncuentro());
            dto.setImagenDestacada(grupo.getViaje().getImagenDestacada());
            dto.setRangoEdadMin(grupo.getViaje().getRangoEdadMin());
            dto.setRangoEdadMax(grupo.getViaje().getRangoEdadMax());
            if (grupo.getViaje().getTipo() != null) {
                dto.setIdTipoViaje(grupo.getViaje().getTipo().getIdTipo());
            }
        }

        if (grupo.getEtiquetas() != null) {
            dto.setEtiquetas(grupo.getEtiquetas().stream()
                    .map(etiqueta -> etiqueta.getNombreEtiqueta())
                    .collect(Collectors.toList()));
        }

        model.addAttribute("datosViaje", dto);
        model.addAttribute("grupo", grupo); // Necesario para el ID en el formulario
        model.addAttribute("tiposViaje", grupoViajeService.obtenerTiposViaje());

        return "grupos/editar-grupo";
    }

    @PostMapping("/editar/{id}")
    public String actualizarGrupo(@PathVariable("id") Long idGrupo,
            @Valid @ModelAttribute("datosViaje") CrearGrupoViajeDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            // Recargar datos necesarios en caso de error
            usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
            usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);
            model.addAttribute("tiposViaje", grupoViajeService.obtenerTiposViaje());
            return "grupos/editar-grupo";
        }

        try {
            // Actualizar el grupo usando el servicio
            grupoViajeService.actualizarGrupoViaje(idGrupo, dto);
            redirectAttributes.addFlashAttribute("mensaje", "Grupo actualizado exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
            return "redirect:/grupos/mis-viajes";
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("mensaje", "No tienes permiso para editar este grupo");
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/grupos/mis-viajes";
        } catch (Exception e) {
            model.addAttribute("error", "Error al actualizar el grupo: " + e.getMessage());
            usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
            usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);
            model.addAttribute("tiposViaje", grupoViajeService.obtenerTiposViaje());
            return "grupos/editar-grupo";
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarGrupo(@PathVariable("id") Long idGrupo) {
        // Obtener el usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener el grupo
        GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        // Verificar que el usuario autenticado sea el creador del grupo
        if (!grupo.getCreador().equals(usuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo el creador puede eliminar el grupo");
        }

        try {
            // Eliminar el grupo y su viaje asociado
            Viaje viaje = grupo.getViaje();
            if (viaje != null) {
                viajeRepository.delete(viaje);
            }
            grupoViajeRepository.delete(grupo);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el grupo: " + e.getMessage());
        }
    }

}
