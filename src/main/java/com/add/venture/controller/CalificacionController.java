package com.add.venture.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.add.venture.model.Resena;
import com.add.venture.model.Usuario;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.ParticipanteGrupoRepository;
import com.add.venture.repository.ResenaRepository;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.service.ILogroService;

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
    
    @Autowired
    private ResenaRepository resenaRepository;
    
    @Autowired
    private ILogroService logroService;

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

        // Verificar que el usuario fue participante del grupo O es el creador
        boolean esCreador = grupo.getCreador().equals(usuario);
        boolean esParticipante = false;
        
        if (!esCreador) {
            Optional<ParticipanteGrupo> participante = participanteGrupoRepository.findByUsuarioAndGrupo(usuario, grupo);
            esParticipante = participante.isPresent() && participante.get().getEstadoSolicitud() == EstadoSolicitud.ACEPTADO;
        }
        
        if (!esCreador && !esParticipante) {
            return "redirect:/grupos";
        }

        // Verificar que el viaje esté cerrado o concluido
        System.out.println("DEBUG: Estado del grupo: " + grupo.getEstado());
        if (!"cerrado".equals(grupo.getEstado()) && !"concluido".equals(grupo.getEstado())) {
            System.out.println("DEBUG: Redirigiendo porque el grupo no está cerrado/concluido");
            return "redirect:/grupos/" + idGrupo;
        }

        // Obtener todos los usuarios que se pueden calificar
        List<Usuario> usuariosParaCalificar = new ArrayList<>();
        
        // Agregar el creador del grupo si el usuario actual no es el creador
        if (!esCreador) {
            usuariosParaCalificar.add(grupo.getCreador());
        }
        
        // Agregar todos los participantes aceptados (excepto el usuario actual)
        List<ParticipanteGrupo> participantesAceptados = participanteGrupoRepository
                .findByGrupoAndEstadoSolicitudOrderByFechaUnionAsc(grupo, EstadoSolicitud.ACEPTADO);
        
        for (ParticipanteGrupo p : participantesAceptados) {
            if (!p.getUsuario().equals(usuario)) {
                usuariosParaCalificar.add(p.getUsuario());
            }
        }
        
        // Filtrar usuarios que ya han sido calificados por este usuario
        List<Usuario> usuariosSinCalificar = usuariosParaCalificar.stream()
                .filter(u -> !resenaRepository.existsByAutorAndDestinatarioAndGrupo(usuario, u, grupo))
                .toList();

        System.out.println("DEBUG: Total usuarios para calificar: " + usuariosParaCalificar.size());
        System.out.println("DEBUG: Usuarios sin calificar: " + usuariosSinCalificar.size());
        System.out.println("DEBUG: Es creador: " + esCreador);
        System.out.println("DEBUG: Email usuario actual: " + usuario.getEmail());
        if (grupo.getCreador() != null) {
            System.out.println("DEBUG: Email creador: " + grupo.getCreador().getEmail());
        }

        model.addAttribute("grupo", grupo);
        model.addAttribute("participantesParaCalificar", usuariosSinCalificar);
        model.addAttribute("yaCalificados", usuariosParaCalificar.size() - usuariosSinCalificar.size());

        return "calificaciones/calificar-viajeros";
    }

    @PostMapping("/calificar")
    public String calificarViajeros(
            @RequestParam("idGrupo") Long idGrupo,
            @RequestParam Map<String, String> allParams,
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

            // Verificar que el grupo esté cerrado
            if (!"cerrado".equals(grupo.getEstado()) && !"concluido".equals(grupo.getEstado())) {
                redirectAttributes.addFlashAttribute("error", "Solo se pueden calificar viajes cerrados o concluidos");
                return "redirect:/grupos/" + idGrupo;
            }

            // Extraer parámetros del Map
            List<Long> idsParticipantes = new ArrayList<>();
            List<Integer> calificaciones = new ArrayList<>();
            List<String> comentarios = new ArrayList<>();
            
            // Contar cuántos participantes hay para calificar
            int maxIndex = -1;
            for (String key : allParams.keySet()) {
                if (key.startsWith("idsParticipantes[")) {
                    try {
                        int index = Integer.parseInt(key.substring(17, key.length() - 1)); // Extraer el número entre [ ]
                        if (index > maxIndex) maxIndex = index;
                    } catch (NumberFormatException e) {
                        // Ignorar si no se puede parsear
                    }
                }
            }
            
            // Construir las listas en orden
            for (int i = 0; i <= maxIndex; i++) {
                String idParam = allParams.get("idsParticipantes[" + i + "]");
                String calificacionParam = allParams.get("calificaciones[" + i + "]");
                String comentarioParam = allParams.get("comentarios[" + i + "]");
                
                if (idParam != null && calificacionParam != null) {
                    try {
                        idsParticipantes.add(Long.parseLong(idParam));
                        calificaciones.add(Integer.parseInt(calificacionParam));
                        comentarios.add(comentarioParam != null ? comentarioParam : "");
                    } catch (NumberFormatException e) {
                        // Ignorar parámetros inválidos
                        continue;
                    }
                }
            }

            // Guardar las calificaciones
            int calificacionesGuardadas = 0;
            for (int i = 0; i < idsParticipantes.size(); i++) {
                Long idDestinatario = idsParticipantes.get(i);
                Integer calificacion = calificaciones.get(i);
                String comentario = comentarios.get(i);

                // Validar calificación
                if (calificacion < 1 || calificacion > 5) {
                    continue; // Saltar calificaciones inválidas
                }

                Usuario destinatario = usuarioRepository.findById(idDestinatario)
                        .orElse(null);
                
                if (destinatario == null) {
                    continue; // Saltar si el usuario no existe
                }

                // Verificar que no se haya calificado ya a este usuario
                if (resenaRepository.existsByAutorAndDestinatarioAndGrupo(calificador, destinatario, grupo)) {
                    continue; // Saltar si ya existe una calificación
                }

                // Crear y guardar la reseña
                Resena resena = Resena.builder()
                        .autor(calificador)
                        .destinatario(destinatario)
                        .grupo(grupo)
                        .calificacion(calificacion)
                        .comentario(comentario != null && !comentario.trim().isEmpty() ? comentario.trim() : null)
                        .build();

                resenaRepository.save(resena);
                calificacionesGuardadas++;
                
                // Verificar si el destinatario califica para el logro "Verificado"
                logroService.verificarLogroVerificado(destinatario);
            }

            if (calificacionesGuardadas > 0) {
                redirectAttributes.addFlashAttribute("mensaje", 
                    "Se guardaron " + calificacionesGuardadas + " calificaciones exitosamente");
            } else {
                redirectAttributes.addFlashAttribute("error", "No se pudo guardar ninguna calificación");
            }
            
            return "redirect:/grupos/" + idGrupo;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al enviar calificaciones: " + e.getMessage());
            return "redirect:/grupos/" + idGrupo;
        }
    }
} 