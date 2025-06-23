package com.add.venture.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.add.venture.helper.UsuarioAutenticadoHelper;
import com.add.venture.model.GrupoViaje;
import com.add.venture.model.ParticipanteGrupo;
import com.add.venture.model.ParticipanteGrupo.EstadoSolicitud;
import com.add.venture.model.Usuario;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.ParticipanteGrupoRepository;
import com.add.venture.repository.UsuarioRepository;

@Controller
@RequestMapping("/calificaciones")
public class CalificacionController {

    @Autowired
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoViajeRepository grupoViajeRepository;

    @Autowired
    private ParticipanteGrupoRepository participanteGrupoRepository;

    @GetMapping("/grupo/{idGrupo}")
    public String mostrarCalificaciones(@PathVariable("idGrupo") Long idGrupo, Model model) {
        usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/auth/login";
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

        // Verificar que el viaje esté cerrado
        if (!"cerrado".equals(grupo.getEstado())) {
            return "redirect:/grupos/" + idGrupo;
        }

        // Obtener todos los participantes aceptados del grupo (excepto el usuario actual)
        List<ParticipanteGrupo> participantesParaCalificar = participanteGrupoRepository
                .findByGrupoAndEstadoSolicitudOrderByFechaUnionAsc(grupo, EstadoSolicitud.ACEPTADO)
                .stream()
                .filter(p -> !p.getUsuario().equals(usuario))
                .toList();

        model.addAttribute("grupo", grupo);
        model.addAttribute("participantesParaCalificar", participantesParaCalificar);

        return "calificaciones/calificar-viajeros";
    }

    @PostMapping("/calificar")
    public String calificarViajeros(
            @RequestParam("idGrupo") Long idGrupo,
            @RequestParam("calificaciones") List<Integer> calificaciones,
            @RequestParam("comentarios") List<String> comentarios,
            @RequestParam("idsParticipantes") List<Long> idsParticipantes,
            RedirectAttributes redirectAttributes) {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                redirectAttributes.addFlashAttribute("error", "Usuario no autenticado");
                return "redirect:/grupos/" + idGrupo;
            }

            String email = auth.getName();
            Usuario calificador = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Aquí normalmente guardarías las calificaciones en una tabla específica
            // Por ahora solo simularemos el proceso

            redirectAttributes.addFlashAttribute("mensaje", "Calificaciones enviadas exitosamente");
            return "redirect:/grupos/" + idGrupo;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al enviar calificaciones: " + e.getMessage());
            return "redirect:/grupos/" + idGrupo;
        }
    }
} 