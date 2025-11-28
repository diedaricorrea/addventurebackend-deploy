package com.add.venture.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.add.venture.model.GrupoViaje;
import com.add.venture.model.MensajeGrupo;
import com.add.venture.model.ParticipanteGrupo;
import com.add.venture.model.ParticipanteGrupo.EstadoSolicitud;
import com.add.venture.model.Usuario;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.MensajeGrupoRepository;
import com.add.venture.repository.ParticipanteGrupoRepository;
import com.add.venture.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/galeria")
public class GaleriaRestController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoViajeRepository grupoViajeRepository;

    @Autowired
    private ParticipanteGrupoRepository participanteGrupoRepository;
    
    @Autowired
    private MensajeGrupoRepository mensajeGrupoRepository;

    @GetMapping("/grupo/{idGrupo}")
    public ResponseEntity<?> obtenerGaleriaGrupo(@PathVariable Long idGrupo, Authentication auth) {
        try {
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Usuario no autenticado"));
            }

            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar que el usuario fue participante del grupo (incluyendo al creador)
            boolean esParticipante = false;
            if (grupo.getCreador().equals(usuario)) {
                esParticipante = true;
            } else {
                Optional<ParticipanteGrupo> participante = participanteGrupoRepository.findByUsuarioAndGrupo(usuario, grupo);
                if (participante.isPresent() && participante.get().getEstadoSolicitud() == EstadoSolicitud.ACEPTADO) {
                    esParticipante = true;
                }
            }

            if (!esParticipante) {
                return ResponseEntity.status(403).body(Map.of("error", "No tienes permisos para ver esta galería"));
            }

            // Verificar que el grupo esté cerrado o concluido para acceder a la galería
            if (!"cerrado".equals(grupo.getEstado()) && !"concluido".equals(grupo.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of("error", "La galería solo está disponible cuando el viaje está cerrado o concluido"));
            }

            // Obtener solo las imágenes compartidas en el chat
            List<MensajeGrupo> imagenesCompartidas = mensajeGrupoRepository.findByGrupoAndTipoMensajeOrderByFechaEnvioDesc(grupo, "imagen");

            // Convertir a DTOs
            List<Map<String, Object>> imagenesDTO = imagenesCompartidas.stream()
                .map(mensaje -> {
                    Map<String, Object> imagenMap = new HashMap<>();
                    imagenMap.put("idMensaje", mensaje.getIdMensaje());
                    imagenMap.put("archivoUrl", mensaje.getArchivoUrl());
                    imagenMap.put("archivoNombre", mensaje.getArchivoNombre());
                    imagenMap.put("fechaEnvio", mensaje.getFechaEnvio());
                    
                    Map<String, Object> remitenteMap = new HashMap<>();
                    remitenteMap.put("idUsuario", mensaje.getRemitente().getIdUsuario());
                    remitenteMap.put("nombre", mensaje.getRemitente().getNombre());
                    remitenteMap.put("apellido", mensaje.getRemitente().getApellidos());
                    remitenteMap.put("fotoPerfil", mensaje.getRemitente().getFotoPerfil());
                    imagenMap.put("remitente", remitenteMap);
                    
                    return imagenMap;
                })
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("grupo", Map.of(
                "idGrupo", grupo.getIdGrupo(),
                "nombre", grupo.getViaje().getDestinoPrincipal(),
                "nombreViaje", grupo.getNombreViaje() != null ? grupo.getNombreViaje() : grupo.getViaje().getDestinoPrincipal(),
                "fechaInicio", grupo.getViaje().getFechaInicio(),
                "fechaFin", grupo.getViaje().getFechaFin()
            ));
            response.put("imagenesCompartidas", imagenesDTO);
            response.put("totalImagenes", imagenesDTO.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
