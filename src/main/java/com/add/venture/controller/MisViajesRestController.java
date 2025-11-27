package com.add.venture.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.add.venture.dto.MisViajesResponseDTO;
import com.add.venture.dto.GrupoViajeDTO;
import com.add.venture.model.GrupoViaje;
import com.add.venture.model.ParticipanteGrupo;
import com.add.venture.model.Usuario;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.ParticipanteGrupoRepository;
import com.add.venture.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/mis-viajes")
@CrossOrigin(origins = "http://localhost:4200")
public class MisViajesRestController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoViajeRepository grupoViajeRepository;

    @Autowired
    private ParticipanteGrupoRepository participanteGrupoRepository;

    @GetMapping
    public ResponseEntity<MisViajesResponseDTO> obtenerMisViajes(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Grupos creados por el usuario
        List<GrupoViaje> gruposCreados = grupoViajeRepository.findByCreador(usuario);

        // 2. Grupos donde el usuario es participante (no creador)
        List<ParticipanteGrupo> participaciones = participanteGrupoRepository
                .findByUsuarioAndEstadoSolicitud(usuario, ParticipanteGrupo.EstadoSolicitud.ACEPTADO);

        List<GrupoViaje> gruposUnidos = participaciones.stream()
                .map(ParticipanteGrupo::getGrupo)
                .filter(grupo -> !grupo.getCreador().equals(usuario)) // Excluir grupos propios
                .collect(Collectors.toList());

        // 3. Separar grupos activos y cerrados
        List<GrupoViajeDTO> gruposActivosCreados = gruposCreados.stream()
                .filter(grupo -> "activo".equals(grupo.getEstado()))
                .map(this::convertirGrupoADTO)
                .collect(Collectors.toList());

        List<GrupoViajeDTO> gruposActivosUnidos = gruposUnidos.stream()
                .filter(grupo -> "activo".equals(grupo.getEstado()))
                .map(this::convertirGrupoADTO)
                .collect(Collectors.toList());

        List<GrupoViajeDTO> gruposCerrados = new ArrayList<>();
        
        // Agregar grupos creados que están cerrados
        gruposCerrados.addAll(gruposCreados.stream()
                .filter(grupo -> "cerrado".equals(grupo.getEstado()) || "concluido".equals(grupo.getEstado()))
                .map(this::convertirGrupoADTO)
                .collect(Collectors.toList()));

        // Agregar grupos unidos que están cerrados
        gruposCerrados.addAll(gruposUnidos.stream()
                .filter(grupo -> "cerrado".equals(grupo.getEstado()) || "concluido".equals(grupo.getEstado()))
                .map(this::convertirGrupoADTO)
                .collect(Collectors.toList()));

        // Construir respuesta
        MisViajesResponseDTO response = new MisViajesResponseDTO();
        response.setGruposCreados(gruposActivosCreados);
        response.setGruposUnidos(gruposActivosUnidos);
        response.setGruposCerrados(gruposCerrados);
        response.setTotalGrupos(gruposCreados.size() + gruposUnidos.size());

        return ResponseEntity.ok(response);
    }

    private GrupoViajeDTO convertirGrupoADTO(GrupoViaje grupo) {
        GrupoViajeDTO dto = new GrupoViajeDTO();
        dto.setIdGrupo(grupo.getIdGrupo());
        dto.setNombreViaje(grupo.getNombreViaje());
        dto.setEstado(grupo.getEstado());
        dto.setMaxParticipantes(grupo.getMaxParticipantes());
        dto.setFechaCreacion(grupo.getFechaCreacion());
        
        // Información del viaje
        if (grupo.getViaje() != null) {
            GrupoViajeDTO.ViajeDTO viajeDTO = new GrupoViajeDTO.ViajeDTO();
            viajeDTO.setIdViaje(grupo.getViaje().getIdViaje());
            viajeDTO.setDescripcion(grupo.getViaje().getDescripcion());
            viajeDTO.setDestinoPrincipal(grupo.getViaje().getDestinoPrincipal());
            viajeDTO.setFechaInicio(grupo.getViaje().getFechaInicio());
            viajeDTO.setFechaFin(grupo.getViaje().getFechaFin());
            viajeDTO.setImagenDestacada(grupo.getViaje().getImagenDestacada());
            viajeDTO.setRangoEdadMin(grupo.getViaje().getRangoEdadMin());
            viajeDTO.setRangoEdadMax(grupo.getViaje().getRangoEdadMax());
            viajeDTO.setEsVerificado(grupo.getViaje().getEsVerificado());
            dto.setViaje(viajeDTO);
        }
        
        // Información del creador
        if (grupo.getCreador() != null) {
            GrupoViajeDTO.CreadorDTO creadorDTO = new GrupoViajeDTO.CreadorDTO();
            creadorDTO.setIdUsuario(grupo.getCreador().getIdUsuario());
            creadorDTO.setNombre(grupo.getCreador().getNombre());
            creadorDTO.setApellidos(grupo.getCreador().getApellidos());
            creadorDTO.setFotoPerfil(grupo.getCreador().getFotoPerfil());
            dto.setCreador(creadorDTO);
        }
        
        // Participantes (solo aprobados)
        if (grupo.getParticipantes() != null) {
            List<GrupoViajeDTO.ParticipanteDTO> participantesDTO = grupo.getParticipantes().stream()
                    .filter(p -> p.getEstadoSolicitud() == ParticipanteGrupo.EstadoSolicitud.ACEPTADO)
                    .map(p -> {
                        GrupoViajeDTO.ParticipanteDTO pDTO = new GrupoViajeDTO.ParticipanteDTO();
                        pDTO.setIdUsuario(p.getUsuario().getIdUsuario());
                        pDTO.setNombre(p.getUsuario().getNombre());
                        pDTO.setApellidos(p.getUsuario().getApellidos());
                        pDTO.setFotoPerfil(p.getUsuario().getFotoPerfil());
                        return pDTO;
                    })
                    .collect(Collectors.toList());
            dto.setParticipantes(participantesDTO);
        }
        
        // Etiquetas
        if (grupo.getEtiquetas() != null) {
            List<GrupoViajeDTO.EtiquetaDTO> etiquetasDTO = grupo.getEtiquetas().stream()
                    .map(e -> {
                        GrupoViajeDTO.EtiquetaDTO eDTO = new GrupoViajeDTO.EtiquetaDTO();
                        eDTO.setIdEtiqueta(e.getIdEtiqueta());
                        eDTO.setNombreEtiqueta(e.getNombreEtiqueta());
                        return eDTO;
                    })
                    .collect(Collectors.toList());
            dto.setEtiquetas(etiquetasDTO);
        }
        
        return dto;
    }
}
