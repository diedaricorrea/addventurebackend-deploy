package com.add.venture.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/calificaciones")
public class CalificacionRestController {

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
    public ResponseEntity<?> obtenerParticipantesParaCalificar(@PathVariable Long idGrupo, Authentication auth) {
        try {
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Usuario no autenticado"));
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
                return ResponseEntity.status(403).body(Map.of("error", "No tienes permisos para calificar en este grupo"));
            }

            // Verificar que el viaje esté cerrado o concluido
            if (!"cerrado".equals(grupo.getEstado()) && !"concluido".equals(grupo.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Solo se pueden calificar viajes cerrados o concluidos"));
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
            List<Map<String, Object>> usuariosSinCalificar = usuariosParaCalificar.stream()
                    .filter(u -> !resenaRepository.existsByAutorAndDestinatarioAndGrupo(usuario, u, grupo))
                    .map(u -> {
                        Map<String, Object> usuarioMap = new HashMap<>();
                        usuarioMap.put("idUsuario", u.getIdUsuario());
                        usuarioMap.put("nombreCompleto", u.getNombre() + " " + u.getApellidos());
                        usuarioMap.put("fotoPerfil", u.getFotoPerfil());
                        usuarioMap.put("iniciales", 
                            u.getNombre().substring(0, 1).toUpperCase() + 
                            u.getApellidos().substring(0, 1).toUpperCase());
                        return usuarioMap;
                    })
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("grupo", Map.of(
                "idGrupo", grupo.getIdGrupo(),
                "nombre", grupo.getViaje().getDestinoPrincipal()
            ));
            response.put("participantesParaCalificar", usuariosSinCalificar);
            response.put("yaCalificados", usuariosParaCalificar.size() - usuariosSinCalificar.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/calificar")
    public ResponseEntity<?> calificarViajeros(@Valid @RequestBody CalificarRequest request, Authentication auth) {
        try {
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Usuario no autenticado"));
            }

            String email = auth.getName();
            Usuario calificador = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            GrupoViaje grupo = grupoViajeRepository.findById(request.getIdGrupo())
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar que el grupo esté cerrado
            if (!"cerrado".equals(grupo.getEstado()) && !"concluido".equals(grupo.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Solo se pueden calificar viajes cerrados o concluidos"));
            }

            // Guardar las calificaciones
            int calificacionesGuardadas = 0;
            for (CalificacionDTO calificacion : request.getCalificaciones()) {
                Usuario destinatario = usuarioRepository.findById(calificacion.getIdUsuario())
                        .orElse(null);
                
                if (destinatario == null) {
                    continue;
                }

                // Verificar que no se haya calificado ya a este usuario
                if (resenaRepository.existsByAutorAndDestinatarioAndGrupo(calificador, destinatario, grupo)) {
                    continue;
                }

                // Crear y guardar la reseña
                Resena resena = Resena.builder()
                        .autor(calificador)
                        .destinatario(destinatario)
                        .grupo(grupo)
                        .calificacion(calificacion.getCalificacion())
                        .comentario(calificacion.getComentario() != null && !calificacion.getComentario().trim().isEmpty() 
                            ? calificacion.getComentario().trim() 
                            : null)
                        .build();

                resenaRepository.save(resena);
                calificacionesGuardadas++;
                
                // Verificar si el destinatario califica para el logro "Verificado"
                logroService.verificarLogroVerificado(destinatario);
            }

            if (calificacionesGuardadas > 0) {
                return ResponseEntity.ok(Map.of(
                    "mensaje", "Se guardaron " + calificacionesGuardadas + " calificaciones exitosamente",
                    "calificacionesGuardadas", calificacionesGuardadas
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "No se pudo guardar ninguna calificación"));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al enviar calificaciones: " + e.getMessage()));
        }
    }

    // DTOs internos
    public static class CalificarRequest {
        @NotNull(message = "El ID del grupo es requerido")
        private Long idGrupo;
        
        @NotNull(message = "Las calificaciones son requeridas")
        private List<CalificacionDTO> calificaciones;

        public Long getIdGrupo() { return idGrupo; }
        public void setIdGrupo(Long idGrupo) { this.idGrupo = idGrupo; }
        public List<CalificacionDTO> getCalificaciones() { return calificaciones; }
        public void setCalificaciones(List<CalificacionDTO> calificaciones) { this.calificaciones = calificaciones; }
    }

    public static class CalificacionDTO {
        @NotNull(message = "El ID del usuario es requerido")
        private Long idUsuario;
        
        @NotNull(message = "La calificación es requerida")
        @Min(value = 1, message = "La calificación mínima es 1")
        @Max(value = 5, message = "La calificación máxima es 5")
        private Integer calificacion;
        
        private String comentario;

        public Long getIdUsuario() { return idUsuario; }
        public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
        public Integer getCalificacion() { return calificacion; }
        public void setCalificacion(Integer calificacion) { this.calificacion = calificacion; }
        public String getComentario() { return comentario; }
        public void setComentario(String comentario) { this.comentario = comentario; }
    }
}
