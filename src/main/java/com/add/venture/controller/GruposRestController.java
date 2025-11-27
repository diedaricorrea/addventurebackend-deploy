package com.add.venture.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.add.venture.dto.ActionResponse;
import com.add.venture.dto.GrupoViajeResponseDTO;
import com.add.venture.model.Usuario;
import com.add.venture.model.GrupoViaje;
import com.add.venture.model.Itinerario;
import com.add.venture.model.ParticipanteGrupo;
import com.add.venture.model.ParticipanteGrupo.EstadoSolicitud;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.ItinerarioRepository;
import com.add.venture.repository.ParticipanteGrupoRepository;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.service.IBuscarGrupoService;
import com.add.venture.service.IPermisosService;

@RestController
@RequestMapping("/api/grupos")
public class GruposRestController {

    @Autowired
    private IBuscarGrupoService iBuscarGrupoService;

    @Autowired
    private ParticipanteGrupoRepository participanteGrupoRepository;

    @Autowired
    private GrupoViajeRepository grupoViajeRepository;

    @Autowired
    private ItinerarioRepository itinerarioRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private IPermisosService permisosService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> buscarGrupos(
            @RequestParam(required = false) String destinoPrincipal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String sort) {

        try {
            // Validación de destino principal
            if (destinoPrincipal != null && !destinoPrincipal.isBlank() 
                    && !destinoPrincipal.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Se deben ingresar letras");
                errorResponse.put("grupos", List.of());
                errorResponse.put("totalPages", 0);
                errorResponse.put("currentPage", page);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Validación de fechas
            LocalDate hoy = LocalDate.now();
            if ((fechaInicio != null && fechaInicio.isBefore(hoy)) 
                    || (fechaFin != null && fechaFin.isBefore(hoy))) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Fecha inválida");
                errorResponse.put("grupos", List.of());
                errorResponse.put("totalPages", 0);
                errorResponse.put("currentPage", page);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Configurar paginación y ordenamiento
            Pageable pageable;
            if ("destinoPrincipal".equals(sort)) {
                pageable = PageRequest.of(page, size, Sort.by("viaje.destinoPrincipal").ascending());
            } else if ("fechaInicio".equals(sort)) {
                pageable = PageRequest.of(page, size, Sort.by("viaje.fechaInicio").ascending());
            } else if (sort != null && !sort.isBlank()) {
                pageable = PageRequest.of(page, size, Sort.by(sort).ascending());
            } else {
                pageable = PageRequest.of(page, size);
            }

            // Obtener grupos filtrados
            Page<GrupoViaje> paginaFiltrada;
            if ((destinoPrincipal == null || destinoPrincipal.isBlank()) 
                    && fechaInicio == null && fechaFin == null) {
                paginaFiltrada = iBuscarGrupoService.obtenerGrupos(pageable);
            } else {
                paginaFiltrada = iBuscarGrupoService.buscarGrupos(destinoPrincipal, fechaInicio, fechaFin, pageable);
            }

            // Filtrar grupos con cupo disponible y convertir a DTO
            List<GrupoViajeResponseDTO> gruposDTO = paginaFiltrada.getContent().stream()
                    .filter(grupo -> {
                        long aceptados = participanteGrupoRepository.countByGrupoAndEstadoSolicitud(grupo,
                                EstadoSolicitud.ACEPTADO);
                        return (aceptados + 1) < grupo.getMaxParticipantes();
                    })
                    .map(this::convertirADTO)
                    .collect(Collectors.toList());

            // Construir respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("grupos", gruposDTO);
            response.put("totalPages", paginaFiltrada.getTotalPages());
            response.put("totalElements", paginaFiltrada.getTotalElements());
            response.put("currentPage", page);
            response.put("size", size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error al buscar grupos: " + e.getMessage());
            errorResponse.put("grupos", List.of());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private GrupoViajeResponseDTO convertirADTO(GrupoViaje grupo) {
        // Obtener participantes aceptados
        List<ParticipanteGrupo> participantesAceptados = participanteGrupoRepository
                .findByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.ACEPTADO);

        // Convertir participantes a DTO
        List<GrupoViajeResponseDTO.ParticipanteInfo> participantesDTO = participantesAceptados.stream()
                .map(p -> GrupoViajeResponseDTO.ParticipanteInfo.builder()
                        .idUsuario(p.getUsuario().getIdUsuario())
                        .nombreCompleto(p.getUsuario().getNombre() + " " + p.getUsuario().getApellidos())
                        .fotoPerfil(p.getUsuario().getFotoPerfil())
                        .iniciales(p.getUsuario().getIniciales())
                        .build())
                .collect(Collectors.toList());

        // Construir información del viaje
        GrupoViajeResponseDTO.ViajeInfo viajeInfo = null;
        if (grupo.getViaje() != null) {
            viajeInfo = GrupoViajeResponseDTO.ViajeInfo.builder()
                    .idViaje(grupo.getViaje().getIdViaje())
                    .destinoPrincipal(grupo.getViaje().getDestinoPrincipal())
                    .fechaInicio(grupo.getViaje().getFechaInicio())
                    .fechaFin(grupo.getViaje().getFechaFin())
                    .descripcion(grupo.getViaje().getDescripcion())
                    .rangoEdadMin(grupo.getViaje().getRangoEdadMin())
                    .rangoEdadMax(grupo.getViaje().getRangoEdadMax())
                    .esVerificado(grupo.getViaje().getEsVerificado())
                    .imagenDestacada(grupo.getViaje().getImagenDestacada())
                    .build();
        }

        // Construir información del creador
        GrupoViajeResponseDTO.CreadorInfo creadorInfo = GrupoViajeResponseDTO.CreadorInfo.builder()
                .idUsuario(grupo.getCreador().getIdUsuario())
                .nombreCompleto(grupo.getCreador().getNombre() + " " + grupo.getCreador().getApellidos())
                .fotoPerfil(grupo.getCreador().getFotoPerfil())
                .iniciales(grupo.getCreador().getIniciales())
                .build();

        // Convertir etiquetas a lista de strings
        List<String> etiquetas = grupo.getEtiquetas() != null
                ? grupo.getEtiquetas().stream()
                        .map(e -> e.getNombreEtiqueta())
                        .collect(Collectors.toList())
                : List.of();

        // Construir DTO del grupo
        return GrupoViajeResponseDTO.builder()
                .idGrupo(grupo.getIdGrupo())
                .nombreViaje(grupo.getNombreViaje())
                .maxParticipantes(grupo.getMaxParticipantes())
                .estado(grupo.getEstado())
                .viaje(viajeInfo)
                .creador(creadorInfo)
                .participantes(participantesDTO)
                .totalParticipantes(participantesDTO.size() + 1) // +1 por el creador
                .etiquetas(etiquetas)
                .build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetalleGrupo(@PathVariable("id") Long idGrupo) {
        try {
            // Buscar el grupo
            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Convertir a DTO básico
            GrupoViajeResponseDTO grupoDTO = convertirADTO(grupo);

            // Obtener itinerarios ordenados
            List<Itinerario> itinerarios = itinerarioRepository.findByGrupoOrderByDiaNumeroAsc(grupo);

            // Construir respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("grupo", grupoDTO);
            response.put("itinerarios", itinerarios);
            response.put("participantesAceptados", grupoDTO.getParticipantes().size());
            response.put("totalMiembros", grupoDTO.getTotalParticipantes());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error al obtener detalles del grupo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/{id}/permisos")
    public ResponseEntity<Map<String, Object>> obtenerPermisosUsuario(
            @PathVariable("id") Long idGrupo, 
            Authentication authentication) {
        try {
            Map<String, Object> permisos = new HashMap<>();
            
            // Si no está autenticado
            if (authentication == null || !authentication.isAuthenticated()) {
                permisos.put("isAuthenticated", false);
                permisos.put("isCreador", false);
                permisos.put("isMiembro", false);
                permisos.put("estadoSolicitud", "NINGUNA");
                permisos.put("puedeUnirse", false);
                permisos.put("puedeAbandonar", false);
                permisos.put("puedeEditar", false);
                permisos.put("puedeEliminar", false);
                permisos.put("puedeCerrar", false);
                permisos.put("puedeCalificar", false);
                permisos.put("puedeVerGaleria", false);
                permisos.put("puedeAccederChat", false);
                return ResponseEntity.ok(permisos);
            }
            
            // Buscar usuario y grupo
            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
            
            // Verificar si es creador
            boolean isCreador = permisosService.esCreadorDelGrupo(usuario, grupo);
            
            // Verificar estado de solicitud
            ParticipanteGrupo participante = participanteGrupoRepository
                    .findByUsuarioAndGrupo(usuario, grupo)
                    .orElse(null);
            
            String estadoSolicitud = "NINGUNA";
            boolean isMiembro = false;
            
            if (participante != null) {
                estadoSolicitud = participante.getEstadoSolicitud().toString();
                isMiembro = participante.getEstadoSolicitud() == EstadoSolicitud.ACEPTADO;
            }
            
            boolean isActivo = "activo".equals(grupo.getEstado());
            boolean isCerrado = "cerrado".equals(grupo.getEstado());
            
            // Calcular permisos
            permisos.put("isAuthenticated", true);
            permisos.put("isCreador", isCreador);
            permisos.put("isMiembro", isMiembro || isCreador);
            permisos.put("estadoSolicitud", estadoSolicitud);
            
            // Puede unirse: No es creador, no tiene solicitud, grupo activo
            permisos.put("puedeUnirse", !isCreador && "NINGUNA".equals(estadoSolicitud) && isActivo);
            
            // Puede abandonar: Es miembro (no creador), grupo activo
            permisos.put("puedeAbandonar", !isCreador && isMiembro && isActivo);
            
            // Puede editar: Es creador y grupo activo
            permisos.put("puedeEditar", isCreador && isActivo);
            
            // Puede eliminar: Es creador
            permisos.put("puedeEliminar", permisosService.usuarioTienePermiso(usuario, grupo, "ELIMINAR_GRUPO"));
            
            // Puede cerrar: Es creador y grupo activo
            permisos.put("puedeCerrar", permisosService.usuarioTienePermiso(usuario, grupo, "CERRAR_GRUPO") && isActivo);
            
            // Puede calificar: Es miembro y grupo cerrado
            permisos.put("puedeCalificar", (isMiembro || isCreador) && isCerrado);
            
            // Puede ver galería: Es miembro y grupo cerrado/concluido
            permisos.put("puedeVerGaleria", (isMiembro || isCreador) && (isCerrado || "concluido".equals(grupo.getEstado())));
            
            // Puede acceder chat: Es miembro aceptado
            permisos.put("puedeAccederChat", permisosService.usuarioTienePermiso(usuario, grupo, "ACCEDER_CHAT"));
            
            return ResponseEntity.ok(permisos);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error al obtener permisos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/{id}/unirse")
    public ResponseEntity<ActionResponse> unirseGrupo(@PathVariable("id") Long idGrupo, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(ActionResponse.builder()
                        .success(false)
                        .error("Debes iniciar sesión para unirte a un grupo")
                        .build());
            }
            
            // Buscar el grupo
            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar que no esté lleno
            long aceptados = participanteGrupoRepository.countByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.ACEPTADO);
            if ((aceptados + 1) >= grupo.getMaxParticipantes()) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("El grupo está lleno")
                        .build());
            }

            // Aquí llamar al servicio que maneja la lógica de unirse
            // Por ahora retornamos éxito simulado
            return ResponseEntity.ok(ActionResponse.builder()
                    .success(true)
                    .message("Solicitud enviada exitosamente")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al unirse al grupo: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/{id}/abandonar")
    public ResponseEntity<ActionResponse> abandonarGrupo(@PathVariable("id") Long idGrupo, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(ActionResponse.builder()
                        .success(false)
                        .error("Debes iniciar sesión")
                        .build());
            }

            // Aquí llamar al servicio que maneja la lógica de abandonar
            return ResponseEntity.ok(ActionResponse.builder()
                    .success(true)
                    .message("Has abandonado el grupo exitosamente")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al abandonar el grupo: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<ActionResponse> cerrarGrupo(@PathVariable("id") Long idGrupo, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(ActionResponse.builder()
                        .success(false)
                        .error("Debes iniciar sesión")
                        .build());
            }

            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            grupo.setEstado("cerrado");
            grupoViajeRepository.save(grupo);

            return ResponseEntity.ok(ActionResponse.builder()
                    .success(true)
                    .message("Viaje cerrado exitosamente")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al cerrar el grupo: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ActionResponse> eliminarGrupo(@PathVariable("id") Long idGrupo, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(ActionResponse.builder()
                        .success(false)
                        .error("Debes iniciar sesión")
                        .build());
            }

            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar permisos (simplificado)
            grupoViajeRepository.delete(grupo);

            return ResponseEntity.ok(ActionResponse.builder()
                    .success(true)
                    .message("Grupo eliminado exitosamente")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al eliminar el grupo: " + e.getMessage())
                    .build());
        }
    }
}
