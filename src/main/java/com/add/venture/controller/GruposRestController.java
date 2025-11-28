package com.add.venture.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.add.venture.dto.ActionResponse;
import com.add.venture.dto.CrearGrupoViajeDTO;
import com.add.venture.dto.GrupoViajeResponseDTO;
import com.add.venture.model.Rol;

import jakarta.validation.Valid;
import com.add.venture.model.Usuario;
import com.add.venture.model.UsuarioRolGrupo;
import com.add.venture.model.GrupoViaje;
import com.add.venture.model.Itinerario;
import com.add.venture.model.ParticipanteGrupo;
import com.add.venture.model.ParticipanteGrupo.EstadoSolicitud;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.ItinerarioRepository;
import com.add.venture.repository.ParticipanteGrupoRepository;
import com.add.venture.repository.RolRepository;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.repository.UsuarioRolGrupoRepository;
import com.add.venture.service.IBuscarGrupoService;
import com.add.venture.service.INotificacionService;
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
    private UsuarioRolGrupoRepository usuarioRolGrupoRepository;
    
    @Autowired
    private IPermisosService permisosService;
    
    @Autowired
    private INotificacionService notificacionService;
    
    @Autowired
    private RolRepository rolRepository;
    
    @Autowired
    private com.add.venture.service.IGrupoViajeService grupoViajeService;

    /**
     * Buscar grupos con filtros
     */
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

    @GetMapping("/{id}/solicitudes-pendientes")
    public ResponseEntity<?> obtenerSolicitudesPendientes(@PathVariable("id") Long idGrupo, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Debes iniciar sesión"));
            }
            
            // Obtener usuario autenticado
            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Buscar el grupo
            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
            
            // Validar que el usuario es el líder del grupo
            if (!grupo.getCreador().getIdUsuario().equals(usuario.getIdUsuario())) {
                return ResponseEntity.status(403).body(Map.of("error", "No tienes permisos para ver las solicitudes de este grupo"));
            }
            
            // Obtener solicitudes pendientes
            List<ParticipanteGrupo> solicitudesPendientes = participanteGrupoRepository
                    .findByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.PENDIENTE);
            
            // Convertir a DTO con información del solicitante
            List<Map<String, Object>> solicitudesDTO = solicitudesPendientes.stream()
                    .map(solicitud -> {
                        Usuario solicitante = solicitud.getUsuario();
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("idUsuario", solicitante.getIdUsuario());
                        dto.put("nombreCompleto", solicitante.getNombre() + " " + solicitante.getApellidos());
                        dto.put("email", solicitante.getEmail());
                        dto.put("fotoPerfil", solicitante.getFotoPerfil());
                        dto.put("iniciales", solicitante.getIniciales());
                        dto.put("fechaSolicitud", solicitud.getFechaUnion());
                        dto.put("intentos", solicitud.getIntentosSolicitud());
                        return dto;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "solicitudes", solicitudesDTO,
                "total", solicitudesDTO.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al obtener solicitudes: " + e.getMessage()));
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
            
            // Obtener usuario autenticado
            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Buscar el grupo
            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // 1. Validar que no es el creador
            if (grupo.getCreador().getIdUsuario().equals(usuario.getIdUsuario())) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("No puedes unirte a tu propio grupo")
                        .build());
            }

            // 2. Verificar capacidad del grupo
            long participantesAceptados = participanteGrupoRepository
                    .countByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.ACEPTADO);
            
            // Incluir al creador en el conteo (+1)
            if ((participantesAceptados + 1) >= grupo.getMaxParticipantes()) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("El grupo está lleno")
                        .build());
            }

            // 3. Verificar si ya existe una solicitud
            ParticipanteGrupo participanteExistente = participanteGrupoRepository
                    .findByUsuarioAndGrupo(usuario, grupo)
                    .orElse(null);

            if (participanteExistente != null) {
                // Ya existe una solicitud, verificar estado
                switch (participanteExistente.getEstadoSolicitud()) {
                    case ACEPTADO:
                        return ResponseEntity.badRequest().body(ActionResponse.builder()
                                .success(false)
                                .error("Ya eres miembro de este grupo")
                                .build());
                    
                    case PENDIENTE:
                        return ResponseEntity.badRequest().body(ActionResponse.builder()
                                .success(false)
                                .error("Ya tienes una solicitud pendiente para este grupo")
                                .build());
                    
                    case RECHAZADO:
                        // Permitir reintentar si no ha superado el límite
                        int intentosActuales = participanteExistente.getIntentosSolicitud() != null 
                                ? participanteExistente.getIntentosSolicitud() 
                                : 0;
                        
                        if (intentosActuales >= 3) {
                            return ResponseEntity.badRequest().body(ActionResponse.builder()
                                    .success(false)
                                    .error("Has alcanzado el límite máximo de intentos (3/3) para este grupo")
                                    .build());
                        }
                        
                        // Actualizar solicitud existente
                        participanteExistente.setEstadoSolicitud(EstadoSolicitud.PENDIENTE);
                        participanteExistente.setIntentosSolicitud(intentosActuales + 1);
                        participanteExistente.setFechaUnion(java.time.LocalDateTime.now());
                        participanteGrupoRepository.save(participanteExistente);
                        
                        // Enviar notificación al líder
                        notificacionService.crearNotificacionSolicitudUnion(
                                usuario, 
                                grupo.getCreador(), 
                                grupo.getIdGrupo(), 
                                grupo.getNombreViaje());
                        
                        String mensaje = (intentosActuales + 1) == 1 
                                ? "Solicitud enviada exitosamente al líder del grupo"
                                : "Solicitud reenviada exitosamente (intento " + (intentosActuales + 1) + " de 3)";
                        
                        return ResponseEntity.ok(ActionResponse.builder()
                                .success(true)
                                .message(mensaje)
                                .build());
                }
            }

            // 4. Crear nueva solicitud (primera vez)
            ParticipanteGrupo nuevaSolicitud = ParticipanteGrupo.builder()
                    .usuario(usuario)
                    .grupo(grupo)
                    .rolParticipante("MIEMBRO")
                    .estadoSolicitud(EstadoSolicitud.PENDIENTE)
                    .fechaUnion(java.time.LocalDateTime.now())
                    .intentosSolicitud(1)
                    .build();
            
            participanteGrupoRepository.save(nuevaSolicitud);

            // 5. Enviar notificación al líder del grupo
            notificacionService.crearNotificacionSolicitudUnion(
                    usuario, 
                    grupo.getCreador(), 
                    grupo.getIdGrupo(), 
                    grupo.getNombreViaje());

            return ResponseEntity.ok(ActionResponse.builder()
                    .success(true)
                    .message("Solicitud enviada exitosamente al líder del grupo")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al unirse al grupo: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/{id}/solicitudes/{idUsuario}/aceptar")
    public ResponseEntity<ActionResponse> aceptarSolicitud(
            @PathVariable("id") Long idGrupo,
            @PathVariable("idUsuario") Long idSolicitante,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(ActionResponse.builder()
                        .success(false)
                        .error("Debes iniciar sesión")
                        .build());
            }
            
            // Obtener líder autenticado
            String email = authentication.getName();
            Usuario lider = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Buscar grupo y solicitante
            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
            
            Usuario solicitante = usuarioRepository.findById(idSolicitante)
                    .orElseThrow(() -> new RuntimeException("Solicitante no encontrado"));
            
            // Validar que el usuario es el líder del grupo
            if (!grupo.getCreador().getIdUsuario().equals(lider.getIdUsuario())) {
                return ResponseEntity.status(403).body(ActionResponse.builder()
                        .success(false)
                        .error("No tienes permisos para aprobar solicitudes de este grupo")
                        .build());
            }
            
            // Buscar la solicitud
            ParticipanteGrupo solicitud = participanteGrupoRepository
                    .findByUsuarioAndGrupo(solicitante, grupo)
                    .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
            
            // Validar que está pendiente
            if (solicitud.getEstadoSolicitud() != EstadoSolicitud.PENDIENTE) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("La solicitud no está pendiente")
                        .build());
            }
            
            // Verificar capacidad del grupo antes de aceptar
            long participantesAceptados = participanteGrupoRepository
                    .countByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.ACEPTADO);
            
            if ((participantesAceptados + 1) >= grupo.getMaxParticipantes()) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("El grupo está lleno")
                        .build());
            }
            
            // 1. Actualizar estado de la solicitud
            solicitud.setEstadoSolicitud(EstadoSolicitud.ACEPTADO);
            participanteGrupoRepository.save(solicitud);
            
            // 2. Asignar rol de MIEMBRO al usuario
            // IMPORTANTE: Eliminar cualquier rol existente (activo o inactivo) antes de asignar
            Optional<UsuarioRolGrupo> rolExistente = usuarioRolGrupoRepository
                    .findByUsuarioAndGrupo(solicitante, grupo);
            
            if (rolExistente.isPresent()) {
                usuarioRolGrupoRepository.delete(rolExistente.get());
                usuarioRolGrupoRepository.flush(); // Asegurar que se elimina antes de insertar
            }
            
            Rol rolMiembro = rolRepository.findByNombreRol("MIEMBRO")
                    .orElseThrow(() -> new RuntimeException("Rol MIEMBRO no encontrado"));
            permisosService.asignarRolEnGrupo(solicitante, grupo, rolMiembro, lider);
            
            // 3. Enviar notificación de aceptación al solicitante
            notificacionService.crearNotificacionSolicitudAceptada(solicitante, grupo.getNombreViaje());
            
            return ResponseEntity.ok(ActionResponse.builder()
                    .success(true)
                    .message("Solicitud aceptada exitosamente")
                    .build());
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al aceptar solicitud: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/{id}/solicitudes/{idUsuario}/rechazar")
    public ResponseEntity<ActionResponse> rechazarSolicitud(
            @PathVariable("id") Long idGrupo,
            @PathVariable("idUsuario") Long idSolicitante,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(ActionResponse.builder()
                        .success(false)
                        .error("Debes iniciar sesión")
                        .build());
            }
            
            // Obtener líder autenticado
            String email = authentication.getName();
            Usuario lider = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Buscar grupo y solicitante
            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
            
            Usuario solicitante = usuarioRepository.findById(idSolicitante)
                    .orElseThrow(() -> new RuntimeException("Solicitante no encontrado"));
            
            // Validar que el usuario es el líder del grupo
            if (!grupo.getCreador().getIdUsuario().equals(lider.getIdUsuario())) {
                return ResponseEntity.status(403).body(ActionResponse.builder()
                        .success(false)
                        .error("No tienes permisos para rechazar solicitudes de este grupo")
                        .build());
            }
            
            // Buscar la solicitud
            ParticipanteGrupo solicitud = participanteGrupoRepository
                    .findByUsuarioAndGrupo(solicitante, grupo)
                    .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
            
            // Validar que está pendiente
            if (solicitud.getEstadoSolicitud() != EstadoSolicitud.PENDIENTE) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("La solicitud no está pendiente")
                        .build());
            }
            
            int intentosActuales = solicitud.getIntentosSolicitud() != null 
                    ? solicitud.getIntentosSolicitud() 
                    : 1;
            
            // 1. Actualizar estado de la solicitud
            solicitud.setEstadoSolicitud(EstadoSolicitud.RECHAZADO);
            participanteGrupoRepository.save(solicitud);
            
            // 2. Remover cualquier rol que pudiera tener (por si acaso)
            permisosService.removerRolEnGrupo(solicitante, grupo, lider);
            
            // 3. Enviar notificación de rechazo con contador de intentos
            notificacionService.crearNotificacionSolicitudRechazada(
                    solicitante, 
                    grupo.getNombreViaje(), 
                    intentosActuales, 
                    3);
            
            return ResponseEntity.ok(ActionResponse.builder()
                    .success(true)
                    .message("Solicitud rechazada")
                    .build());
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ActionResponse.builder()
                    .success(false)
                    .error("Error al rechazar solicitud: " + e.getMessage())
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

            // Obtener usuario autenticado
            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Buscar el grupo
            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
            
            // Validar que el usuario no es el creador
            if (grupo.getCreador().getIdUsuario().equals(usuario.getIdUsuario())) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("El creador del grupo no puede abandonarlo. Puedes eliminarlo si lo deseas.")
                        .build());
            }
            
            // Buscar la participación del usuario
            ParticipanteGrupo participacion = participanteGrupoRepository
                    .findByUsuarioAndGrupo(usuario, grupo)
                    .orElseThrow(() -> new RuntimeException("No eres miembro de este grupo"));
            
            // Validar que está aceptado
            if (participacion.getEstadoSolicitud() != EstadoSolicitud.ACEPTADO) {
                return ResponseEntity.badRequest().body(ActionResponse.builder()
                        .success(false)
                        .error("No eres miembro activo de este grupo")
                        .build());
            }
            
            // Eliminar la participación
            participanteGrupoRepository.delete(participacion);
            
            // Remover roles del usuario en este grupo
            permisosService.removerRolEnGrupo(usuario, grupo, grupo.getCreador());

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

    /**
     * Crear un nuevo grupo de viaje
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearGrupo(
            @Valid @RequestBody CrearGrupoViajeDTO dto,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("error", "Usuario no autenticado");
            return ResponseEntity.status(401).body(response);
        }

        // Validar que la fecha de fin sea posterior o igual a la fecha de inicio
        if (dto.getFechaFin() != null && dto.getFechaInicio() != null) {
            if (dto.getFechaFin().isBefore(dto.getFechaInicio())) {
                response.put("success", false);
                response.put("error", "La fecha de fin debe ser igual o posterior a la fecha de inicio");
                return ResponseEntity.status(400).body(response);
            }
        }

        // Validar rango de edad
        if (dto.getRangoEdadMax() != null && dto.getRangoEdadMin() != null) {
            if (dto.getRangoEdadMax() < dto.getRangoEdadMin()) {
                response.put("success", false);
                response.put("error", "La edad máxima debe ser mayor o igual a la edad mínima");
                return ResponseEntity.status(400).body(response);
            }
        }

        try {
            GrupoViaje grupoCreado = grupoViajeService.crearGrupoViaje(dto);
            
            response.put("success", true);
            response.put("mensaje", "Grupo de viaje creado exitosamente");
            response.put("idGrupo", grupoCreado.getIdGrupo());
            
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error al crear el grupo: " + e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    /**
     * Actualizar un grupo de viaje existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizarGrupo(
            @PathVariable Long id,
            @Valid @RequestBody CrearGrupoViajeDTO dto,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("error", "Usuario no autenticado");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            GrupoViaje grupo = grupoViajeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar que el usuario sea el creador del grupo
            if (!grupo.getCreador().equals(usuario)) {
                response.put("success", false);
                response.put("error", "Solo el creador puede editar el grupo");
                return ResponseEntity.status(403).body(response);
            }

            // Validar que la fecha de fin sea posterior o igual a la fecha de inicio
            if (dto.getFechaFin() != null && dto.getFechaInicio() != null) {
                if (dto.getFechaFin().isBefore(dto.getFechaInicio())) {
                    response.put("success", false);
                    response.put("error", "La fecha de fin debe ser igual o posterior a la fecha de inicio");
                    return ResponseEntity.status(400).body(response);
                }
            }

            // Validar rango de edad
            if (dto.getRangoEdadMax() != null && dto.getRangoEdadMin() != null) {
                if (dto.getRangoEdadMax() < dto.getRangoEdadMin()) {
                    response.put("success", false);
                    response.put("error", "La edad máxima debe ser mayor o igual a la edad mínima");
                    return ResponseEntity.status(400).body(response);
                }
            }

            GrupoViaje grupoActualizado = grupoViajeService.actualizarGrupoViaje(id, dto);
            
            response.put("success", true);
            response.put("mensaje", "Grupo actualizado exitosamente");
            response.put("idGrupo", grupoActualizado.getIdGrupo());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error al actualizar el grupo: " + e.getMessage());
            return ResponseEntity.status(400).body(response);
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
