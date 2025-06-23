package com.add.venture.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.add.venture.helper.UsuarioAutenticadoHelper;
import com.add.venture.model.GrupoViaje;
import com.add.venture.model.Notificacion;
import com.add.venture.model.ParticipanteGrupo;
import com.add.venture.model.ParticipanteGrupo.EstadoSolicitud;
import com.add.venture.model.Usuario;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.NotificacionRepository;
import com.add.venture.repository.ParticipanteGrupoRepository;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.service.INotificacionService;

@Controller
@RequestMapping("/notificaciones")
public class NotificacionController {

    @Autowired
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @Autowired
    private INotificacionService notificacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private GrupoViajeRepository grupoViajeRepository;

    @Autowired
    private ParticipanteGrupoRepository participanteGrupoRepository;

    @GetMapping
    public String listarNotificaciones(Model model) {
        try {
            usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
            usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                String email = auth.getName();
                Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
                
                if (usuarioOpt.isPresent()) {
                    Usuario usuario = usuarioOpt.get();
                    List<Notificacion> notificaciones = notificacionService.obtenerNotificacionesUsuario(usuario);
                    model.addAttribute("notificaciones", notificaciones);
                } else {
                    // Usuario no encontrado, agregar lista vacía
                    model.addAttribute("notificaciones", new ArrayList<>());
                    model.addAttribute("error", "Usuario no encontrado");
                }
            } else {
                // Usuario no autenticado, agregar lista vacía
                model.addAttribute("notificaciones", new ArrayList<>());
            }
        } catch (Exception e) {
            // En caso de cualquier error, agregar lista vacía para evitar problemas en el template
            model.addAttribute("notificaciones", new ArrayList<>());
            model.addAttribute("error", "Error al cargar notificaciones: " + e.getMessage());
            e.printStackTrace(); // Para debug
        }

        return "user/notificaciones";
    }

    @PostMapping("/{id}/marcar-leida")
    @ResponseBody
    public ResponseEntity<String> marcarComoLeida(@PathVariable("id") Long idNotificacion) {
        try {
            notificacionService.marcarComoLeida(idNotificacion);
            return ResponseEntity.ok("Notificación marcada como leída");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al marcar notificación como leída");
        }
    }

    @PostMapping("/marcar-todas-leidas")
    @ResponseBody
    public ResponseEntity<String> marcarTodasComoLeidas() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                String email = auth.getName();
                Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
                if (usuarioOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("Usuario no encontrado");
                }
                Usuario usuario = usuarioOpt.get();

                notificacionService.marcarTodasComoLeidas(usuario);
                return ResponseEntity.ok("Todas las notificaciones marcadas como leídas");
            }
            return ResponseEntity.badRequest().body("Usuario no autenticado");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al marcar notificaciones como leídas");
        }
    }

    @PostMapping("/responder-solicitud")
    public String responderSolicitudUnion(
            @RequestParam("idNotificacion") Long idNotificacion,
            @RequestParam("idGrupo") Long idGrupo,
            @RequestParam("idSolicitante") Long idSolicitante,
            @RequestParam("accion") String accion,
            RedirectAttributes redirectAttributes) {

        try {
            // Validar parámetros de entrada
            if (idNotificacion == null || idGrupo == null || idSolicitante == null || 
                accion == null || accion.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Parámetros de solicitud inválidos");
                return "redirect:/notificaciones";
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                redirectAttributes.addFlashAttribute("error", "Usuario no autenticado");
                return "redirect:/notificaciones";
            }

            String email = auth.getName();
            Usuario lider = usuarioRepository.findByEmail(email)
                    .orElse(null);
            
            if (lider == null) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/notificaciones";
            }

            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElse(null);
            
            if (grupo == null) {
                redirectAttributes.addFlashAttribute("error", "Grupo no encontrado");
                return "redirect:/notificaciones";
            }

            Usuario solicitante = usuarioRepository.findById(idSolicitante)
                    .orElse(null);
            
            if (solicitante == null) {
                redirectAttributes.addFlashAttribute("error", "Solicitante no encontrado");
                return "redirect:/notificaciones";
            }

            // Verificar que el usuario actual es el líder del grupo
            if (!grupo.getCreador().getIdUsuario().equals(lider.getIdUsuario())) {
                redirectAttributes.addFlashAttribute("error", "No tienes permisos para gestionar este grupo");
                return "redirect:/notificaciones";
            }

            // Buscar la solicitud pendiente
            ParticipanteGrupo solicitud = participanteGrupoRepository.findByUsuarioAndGrupo(solicitante, grupo)
                    .orElse(null);

            if ("aceptar".equals(accion)) {
                if (solicitud == null) {
                    // Crear nueva participación
                    solicitud = ParticipanteGrupo.builder()
                            .usuario(solicitante)
                            .grupo(grupo)
                            .rolParticipante("MIEMBRO")
                            .estadoSolicitud(EstadoSolicitud.ACEPTADO)
                            .fechaUnion(LocalDateTime.now())
                            .build();
                } else {
                    solicitud.setEstadoSolicitud(EstadoSolicitud.ACEPTADO);
                    solicitud.setFechaUnion(LocalDateTime.now());
                }
                participanteGrupoRepository.save(solicitud);
                redirectAttributes.addFlashAttribute("mensaje", "Solicitud aceptada exitosamente");
                
            } else if ("rechazar".equals(accion)) {
                if (solicitud != null) {
                    solicitud.setEstadoSolicitud(EstadoSolicitud.RECHAZADO);
                    participanteGrupoRepository.save(solicitud);
                } else {
                    // Crear registro de rechazo
                    solicitud = ParticipanteGrupo.builder()
                            .usuario(solicitante)
                            .grupo(grupo)
                            .rolParticipante("MIEMBRO")
                            .estadoSolicitud(EstadoSolicitud.RECHAZADO)
                            .fechaUnion(LocalDateTime.now())
                            .build();
                    participanteGrupoRepository.save(solicitud);
                }
                redirectAttributes.addFlashAttribute("mensaje", "Solicitud rechazada");
            } else {
                redirectAttributes.addFlashAttribute("error", "Acción no válida");
                return "redirect:/notificaciones";
            }

            // Marcar la notificación como leída
            try {
                notificacionService.marcarComoLeida(idNotificacion);
            } catch (Exception e) {
                // Log del error pero continúa el proceso
                System.err.println("Error al marcar notificación como leída: " + e.getMessage());
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar la solicitud: " + e.getMessage());
            e.printStackTrace(); // Para debug
        }

        return "redirect:/notificaciones";
    }
} 