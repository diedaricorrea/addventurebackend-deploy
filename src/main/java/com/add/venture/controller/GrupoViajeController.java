package com.add.venture.controller;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import com.add.venture.dto.DiaItinerarioDTO;
import com.add.venture.helper.UsuarioAutenticadoHelper;
import com.add.venture.model.GrupoViaje;
import com.add.venture.model.Itinerario;
import com.add.venture.model.MensajeGrupo;
import com.add.venture.model.Notificacion;
import com.add.venture.model.ParticipanteGrupo;
import com.add.venture.model.ParticipanteGrupo.EstadoSolicitud;
import com.add.venture.model.Usuario;
import com.add.venture.model.Viaje;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.MensajeGrupoRepository;
import com.add.venture.repository.NotificacionRepository;
import com.add.venture.repository.ParticipanteGrupoRepository;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.repository.UsuarioRolGrupoRepository;
import com.add.venture.repository.ViajeRepository;
import com.add.venture.repository.ItinerarioRepository;
import com.add.venture.service.IBuscarGrupoService;
import com.add.venture.service.IGrupoViajeService;
import com.add.venture.service.INotificacionService;
import com.add.venture.service.IPermisosService;

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

    @Autowired
    private IPermisosService permisosService;

    @Autowired
    private UsuarioRolGrupoRepository usuarioRolGrupoRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private ItinerarioRepository itinerarioRepository;

    @Autowired
    private IBuscarGrupoService iBuscarGrupoService;

    @GetMapping
    public String mostrarGrupos(
            @RequestParam(required = false) String destinoPrincipal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String sort,
            Model model) {

        // Cargar datos del usuario
        usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);

        // Validación de campos
        if (destinoPrincipal != null && !destinoPrincipal.isBlank() && !destinoPrincipal.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$")) {
            model.addAttribute("error", "Se deben ingresar letras");
            model.addAttribute("grupos", List.of());
            return "grupos/buscar";
        }

        LocalDate hoy = LocalDate.now();
        if ((fechaInicio != null && fechaInicio.isBefore(hoy)) || (fechaFin != null && fechaFin.isBefore(hoy))) {
            model.addAttribute("error", "Fecha inválida");
            model.addAttribute("grupos", List.of());
            return "grupos/buscar";
        }

        /*Pageable pageable = (sort != null && !sort.isBlank())
            ? PageRequest.of(page, size, Sort.by(sort).ascending())
            : PageRequest.of(page, size);*/
        
        Pageable pageable;
        if ("destinoPrincipal".equals(sort)) {
            // Ordenar por destinoPrincipal en la entidad Viaje
            pageable = PageRequest.of(page, size, Sort.by("viaje.destinoPrincipal").ascending());
        } else if ("fechaInicio".equals(sort)) {
            // Ordenar por fechaInicio en la entidad Viaje
            pageable = PageRequest.of(page, size, Sort.by("viaje.fechaInicio").ascending());
        } else if (sort != null && !sort.isBlank()) {
            // Si hay otro valor para sort, usarlo directamente
            pageable = PageRequest.of(page, size, Sort.by(sort).ascending());
        } else {
            // Si no hay orden, solo paginación
            pageable = PageRequest.of(page, size);
        }

        // Obtener grupos filtrados por destino/fechas (si los hay)
        Page<GrupoViaje> paginaFiltrada;
        if ((destinoPrincipal == null || destinoPrincipal.isBlank()) && fechaInicio == null && fechaFin == null) {
            paginaFiltrada = iBuscarGrupoService.obtenerGrupos(pageable);
        } else {
            paginaFiltrada = iBuscarGrupoService.buscarGrupos(destinoPrincipal, fechaInicio, fechaFin, pageable);
        }

        //Filtrar grupos con cupo
        List<GrupoViaje> gruposConCupo = paginaFiltrada.getContent().stream()
                .filter(grupo -> {
                    long aceptados = participanteGrupoRepository.countByGrupoAndEstadoSolicitud(grupo,
                            EstadoSolicitud.ACEPTADO);
                    return (aceptados + 1) < grupo.getMaxParticipantes();
                })
                .peek(grupo -> {
                    List<ParticipanteGrupo> aceptados = participanteGrupoRepository.findByGrupoAndEstadoSolicitud(grupo,
                            EstadoSolicitud.ACEPTADO);
                    grupo.setParticipantes(new HashSet<>(aceptados));
                })
                .toList();
        
        // Crear una Page "manual"
        Page<GrupoViaje> paginaConCupo = new PageImpl<>(gruposConCupo, pageable, paginaFiltrada.getTotalElements());

        // Agregar al modelo
        model.addAttribute("grupos", paginaConCupo.getContent());
        model.addAttribute("totalPages", paginaConCupo.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);

        return "grupos/buscar";
    }

    /*@GetMapping
    public String listarGrupos(
            @RequestParam(required = false) String destino,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
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

        return "grupos/buscar";
    } */

    @GetMapping("/{id}")
    public String verDetallesGrupo(@PathVariable("id") Long idGrupo, Model model) {
        // Cargar datos del usuario para la navbar y perfil
        usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);

        // Cargar el grupo
        GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        model.addAttribute("grupo", grupo);

        // Cargar itinerarios ordenados por día
        List<Itinerario> itinerariosOrdenados = itinerarioRepository.findByGrupoOrderByDiaNumeroAsc(grupo);
        model.addAttribute("itinerariosOrdenados", itinerariosOrdenados);

        // Verificar si el usuario autenticado es participante del grupo
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Agregar usuarioId para WebSocket global
            model.addAttribute("usuarioId", usuario.getIdUsuario());

            // Verificar si es participante ACEPTADO o es el CREADOR del grupo
            Optional<ParticipanteGrupo> participante = participanteGrupoRepository.findByUsuarioAndGrupo(usuario, grupo);
            boolean isParticipante = (participante.isPresent() && 
                                   participante.get().getEstadoSolicitud() == EstadoSolicitud.ACEPTADO) ||
                                   grupo.getCreador().equals(usuario); // El creador también puede usar el chat
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

        // Obtener rol descriptivo del creador si existe
        Optional<ParticipanteGrupo> creadorParticipante = participanteGrupoRepository
                .findByUsuarioAndGrupo(grupo.getCreador(), grupo);
        if (creadorParticipante.isPresent() && creadorParticipante.get().getRolParticipante() != null) {
            model.addAttribute("creadorRolDescriptivo", creadorParticipante.get().getRolParticipante());
        }

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
        
        // IMPORTANTE: Remover rol en el nuevo sistema de permisos
        try {
            permisosService.removerRolEnGrupo(usuario, grupo, null);
            System.out.println("Rol removido para " + usuario.getEmail() + " al abandonar grupo " + grupo.getNombreViaje());
        } catch (Exception e) {
            System.err.println("Error al remover rol al abandonar grupo: " + e.getMessage());
        }

        redirectAttributes.addFlashAttribute("mensaje", "Has abandonado el grupo exitosamente");
        return "redirect:/grupos";
    }

    @GetMapping("/{id}/galeria-fotos")
    public String verGaleriaFotos(@PathVariable("id") Long idGrupo, Model model) {
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
            return "redirect:/grupos";
        }

        // Verificar que el grupo esté cerrado o concluido para acceder a la galería
        if (!"cerrado".equals(grupo.getEstado()) && !"concluido".equals(grupo.getEstado())) {
            return "redirect:/grupos/" + idGrupo;
        }

        // Obtener solo las imágenes compartidas en el chat
        List<MensajeGrupo> imagenesCompartidas = mensajeGrupoRepository.findByGrupoAndTipoMensajeOrderByFechaEnvioDesc(grupo, "imagen");

        model.addAttribute("grupo", grupo);
        model.addAttribute("imagenesCompartidas", imagenesCompartidas);
        model.addAttribute("totalImagenes", imagenesCompartidas.size());

        return "grupos/galeria-fotos";
    }

    @GetMapping("/{id}/descargar-fotos")
    public ResponseEntity<byte[]> descargarTodasLasFotos(@PathVariable("id") Long idGrupo) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Verificar que el grupo esté cerrado o concluido
            if (!"cerrado".equals(grupo.getEstado()) && !"concluido".equals(grupo.getEstado())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Obtener todas las imágenes del grupo
            List<MensajeGrupo> imagenesCompartidas = mensajeGrupoRepository.findByGrupoAndTipoMensajeOrderByFechaEnvioDesc(grupo, "imagen");

            if (imagenesCompartidas.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            // Crear archivo ZIP con todas las imágenes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            int contador = 1;
            for (MensajeGrupo mensaje : imagenesCompartidas) {
                if (mensaje.getArchivoUrl() != null) {
                    try {
                        // Obtener la ruta completa del archivo
                        String rutaArchivo = mensaje.getArchivoUrl();
                        if (rutaArchivo.startsWith("/uploads/")) {
                            rutaArchivo = "uploads/" + rutaArchivo.substring("/uploads/".length());
                        }
                        
                        Path archivoPath = Paths.get(rutaArchivo);
                        if (Files.exists(archivoPath)) {
                            // Crear entrada en el ZIP
                            String nombreArchivo = String.format("%03d_%s_%s", 
                                contador, 
                                mensaje.getRemitente().getNombre().replaceAll("[^a-zA-Z0-9]", ""),
                                mensaje.getArchivoNombre() != null ? mensaje.getArchivoNombre() : "imagen.jpg");
                            
                            ZipEntry zipEntry = new ZipEntry(nombreArchivo);
                            zos.putNextEntry(zipEntry);
                            
                            // Copiar contenido del archivo al ZIP
                            Files.copy(archivoPath, zos);
                            zos.closeEntry();
                            contador++;
                        }
                    } catch (Exception e) {
                        System.err.println("Error al procesar imagen: " + mensaje.getArchivoUrl() + " - " + e.getMessage());
                        // Continuar con las demás imágenes
                    }
                }
            }

            zos.close();
            
            // Preparar respuesta
            String nombreZip = String.format("Fotos_%s_%s.zip", 
                grupo.getNombreViaje().replaceAll("[^a-zA-Z0-9]", "_"),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", nombreZip);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            System.err.println("Error al crear ZIP de fotos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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

    @PostMapping("/{id}/asignar-rol")
    public String asignarRolDescriptivo(
            @PathVariable("id") Long idGrupo,
            @RequestParam("userId") Long userId,
            @RequestParam("rolDescriptivo") String rolDescriptivo,
            @RequestParam(value = "notasRol", required = false) String notasRol,
            RedirectAttributes redirectAttributes) {

        try {
            // Obtener el usuario autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                redirectAttributes.addFlashAttribute("error", "Usuario no autenticado");
                return "redirect:/grupos/" + idGrupo;
            }

            String email = auth.getName();
            Usuario usuarioAutenticado = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Obtener el grupo
            GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

            // Verificar permisos para asignar roles
            if (!permisosService.puedeAsignarRoles(usuarioAutenticado, grupo)) {
                redirectAttributes.addFlashAttribute("error", "No tienes permisos para asignar roles en este grupo");
                return "redirect:/grupos/" + idGrupo;
            }

            // Obtener el usuario al que se le asignará el rol
            Usuario usuarioAAsignar = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario a asignar rol no encontrado"));

            // Verificar que el usuario sea participante del grupo
            Optional<ParticipanteGrupo> participanteOpt = participanteGrupoRepository
                    .findByUsuarioAndGrupo(usuarioAAsignar, grupo);
            
            // Si no es participante pero es el creador, crear la entrada de participante
            if (participanteOpt.isEmpty()) {
                if (grupo.getCreador().equals(usuarioAAsignar)) {
                    // Crear entrada para el creador
                    ParticipanteGrupo participanteCreador = ParticipanteGrupo.builder()
                            .usuario(usuarioAAsignar)
                            .grupo(grupo)
                            .rolParticipante(rolDescriptivo)
                            .estadoSolicitud(EstadoSolicitud.ACEPTADO)
                            .fechaUnion(LocalDateTime.now())
                            .build();
                    participanteGrupoRepository.save(participanteCreador);
                } else {
                    redirectAttributes.addFlashAttribute("error", "El usuario no es participante de este grupo");
                    return "redirect:/grupos/" + idGrupo;
                }
            } else {
                // Actualizar el rol del participante existente
                ParticipanteGrupo participante = participanteOpt.get();
                participante.setRolParticipante(rolDescriptivo);
                participanteGrupoRepository.save(participante);
            }

            // Mapear emojis y nombres bonitos para el mensaje
            String nombreRolBonito = obtenerNombreRolBonito(rolDescriptivo);
            
            redirectAttributes.addFlashAttribute("mensaje", 
                "Rol '" + nombreRolBonito + "' asignado exitosamente a " + 
                usuarioAAsignar.getNombre() + " " + usuarioAAsignar.getApellidos());
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al asignar rol: " + e.getMessage());
        }

        return "redirect:/grupos/" + idGrupo;
    }

    public String obtenerNombreRolBonito(String rolDescriptivo) {
        switch (rolDescriptivo) {
            case "GUÍA": return "🗺️ Guía del viaje";
            case "COORDINADOR": return "📋 Coordinador";
            case "TESORERO": return "💰 Tesorero/Finanzas";
            case "FOTOGRAFO": return "📸 Fotógrafo oficial";
            case "COCINERO": return "👨‍🍳 Cocinero/Chef";
            case "CONDUCTOR": return "🚗 Conductor";
            case "NAVEGADOR": return "🧭 Navegador";
            case "ANIMADOR": return "🎉 Animador";
            case "MEDICO": return "🏥 Encargado de primeros auxilios";
            case "TRADUCTOR": return "🗣️ Traductor";
            case "LUGAREÑO": return "🏠 Conoce el lugar";
            case "PLANIFICADOR": return "📅 Planificador de actividades";
            case "MIEMBRO": return "👤 Miembro";
            default: return rolDescriptivo;
        }
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

        return "grupos/mis-viajes";
    }

    @GetMapping("/editar/{id}")
    public String editarGrupo(@PathVariable("id") Long idGrupo, Model model) {
        usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);

        // Obtener el grupo
        GrupoViaje grupo = grupoViajeService.buscarGrupoPorId(idGrupo);

        // Verificar permisos usando el sistema de permisos
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (!permisosService.usuarioTienePermiso(usuario, grupo, "EDITAR_GRUPO")) {
            throw new AccessDeniedException("No tienes permiso para editar este grupo");
        }

        // Verificar si han pasado más de 24 horas desde la creación
        LocalDateTime ahora = LocalDateTime.now();
        Duration tiempoDesdeCreacion = Duration.between(grupo.getFechaCreacion(), ahora);
        if (tiempoDesdeCreacion.toHours() >= 24) {
            // Redireccionar a la página de edición bloqueada con información del error
            model.addAttribute("grupo", grupo);
            model.addAttribute("horasTranscurridas", tiempoDesdeCreacion.toHours());
            return "grupos/editar-grupo-bloqueado";
        }

        // Convertir entidad a DTO completo
        CrearGrupoViajeDTO dto = new CrearGrupoViajeDTO();
        dto.setNombreViaje(grupo.getNombreViaje());
        dto.setMaxParticipantes(grupo.getMaxParticipantes());
        
        if (grupo.getViaje() != null) {
            dto.setDestinoPrincipal(grupo.getViaje().getDestinoPrincipal());
            dto.setFechaInicio(grupo.getViaje().getFechaInicio());
            dto.setFechaFin(grupo.getViaje().getFechaFin());
            dto.setDescripcion(grupo.getViaje().getDescripcion());
            dto.setPuntoEncuentro(grupo.getViaje().getPuntoEncuentro());
            dto.setImagenDestacada(grupo.getViaje().getImagenDestacada());
            dto.setRangoEdadMin(grupo.getViaje().getRangoEdadMin());
            dto.setRangoEdadMax(grupo.getViaje().getRangoEdadMax());
        }

        // Cargar etiquetas
        if (grupo.getEtiquetas() != null && !grupo.getEtiquetas().isEmpty()) {
            dto.setEtiquetas(grupo.getEtiquetas().stream()
                    .map(etiqueta -> etiqueta.getNombreEtiqueta())
                    .collect(Collectors.toList()));
        }

        // Cargar itinerarios si existen
        if (grupo.getItinerarios() != null && !grupo.getItinerarios().isEmpty()) {
            List<DiaItinerarioDTO> diasItinerario = grupo.getItinerarios().stream()
                .sorted((i1, i2) -> Integer.compare(i1.getDiaNumero(), i2.getDiaNumero()))
                .map(itinerario -> {
                    DiaItinerarioDTO diaDTO = new DiaItinerarioDTO();
                    diaDTO.setDiaNumero(itinerario.getDiaNumero());
                    diaDTO.setTitulo(itinerario.getTitulo());
                    diaDTO.setDescripcion(itinerario.getDescripcion());
                    diaDTO.setPuntoPartida(itinerario.getPuntoPartida());
                    diaDTO.setPuntoLlegada(itinerario.getPuntoLlegada());
                    diaDTO.setDuracionEstimada(itinerario.getDuracionEstimada());
                    return diaDTO;
                })
                .collect(Collectors.toList());
            dto.setDiasItinerario(diasItinerario);
            
            // Convertir a JSON para el campo oculto
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String itinerarioJson = objectMapper.writeValueAsString(diasItinerario);
                dto.setDiasItinerarioJson(itinerarioJson);
            } catch (Exception e) {
                System.err.println("Error al convertir itinerario a JSON: " + e.getMessage());
            }
        }

        model.addAttribute("datosViaje", dto);
        model.addAttribute("grupo", grupo); // Necesario para el ID en el formulario

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
            return "grupos/editar-grupo";
        }

        try {
            // Verificar permisos y tiempo antes de actualizar
            GrupoViaje grupo = grupoViajeService.buscarGrupoPorId(idGrupo);
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Verificar permisos
            if (!permisosService.usuarioTienePermiso(usuario, grupo, "EDITAR_GRUPO")) {
                throw new AccessDeniedException("No tienes permiso para editar este grupo");
            }

            // Verificar tiempo límite (24 horas)
            LocalDateTime ahora = LocalDateTime.now();
            Duration tiempoDesdeCreacion = Duration.between(grupo.getFechaCreacion(), ahora);
            if (tiempoDesdeCreacion.toHours() >= 24) {
                // Redireccionar a la página de edición bloqueada con información del error
                model.addAttribute("grupo", grupo);
                model.addAttribute("horasTranscurridas", tiempoDesdeCreacion.toHours());
                usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
                usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);
                return "grupos/editar-grupo-bloqueado";
            }

            // Actualizar el grupo usando el servicio
            grupoViajeService.actualizarGrupoViaje(idGrupo, dto);
            redirectAttributes.addFlashAttribute("mensaje", "Grupo actualizado exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
            return "redirect:/grupos/mis-viajes";
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("mensaje", e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/grupos/mis-viajes";
        } catch (Exception e) {
            model.addAttribute("error", "Error al actualizar el grupo: " + e.getMessage());
            usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
            usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);
            return "grupos/editar-grupo";
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarGrupo(@PathVariable("id") Long idGrupo) {
        // Obtener el usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Usuario no autenticado"));
        }

        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener el grupo
        GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        // Verificar que el usuario tenga permisos para eliminar
        if (!permisosService.usuarioTienePermiso(usuario, grupo, "ELIMINAR_GRUPO")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "No tienes permiso para eliminar este grupo"));
        }

        try {
            return evaluarEliminacionGrupo(grupo, usuario);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar el grupo: " + e.getMessage()));
        }
    }

    private ResponseEntity<?> evaluarEliminacionGrupo(GrupoViaje grupo, Usuario solicitante) {
        // Obtener participantes aceptados (excluyendo al creador)
        List<ParticipanteGrupo> participantesAceptados = participanteGrupoRepository
            .findByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.ACEPTADO);
        
        // Obtener también participantes pendientes para información completa
        List<ParticipanteGrupo> participantesPendientes = participanteGrupoRepository
            .findByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.PENDIENTE);
        
        // Calcular tiempo desde creación
        LocalDateTime ahora = LocalDateTime.now();
        Duration tiempoDesdeCreacion = Duration.between(grupo.getFechaCreacion(), ahora);
        boolean esMenorA24Horas = tiempoDesdeCreacion.toHours() < 24;
        
        // REGLA 1: Si no hay participantes aceptados, se puede eliminar siempre
        if (participantesAceptados.isEmpty()) {
            eliminarGrupoDirectamente(grupo);
            String mensaje = participantesPendientes.isEmpty() ? 
                "Grupo eliminado (sin participantes)" :
                "Grupo eliminado (solo había solicitudes pendientes)";
            return ResponseEntity.ok(Map.of(
                "success", true,
                "mensaje", mensaje
            ));
        }
        
        // REGLA 2: Si han pasado menos de 24 horas, el creador puede eliminar 
        // el grupo notificando a todos los participantes
        if (esMenorA24Horas) {
            eliminarGrupoDirectamente(grupo);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "mensaje", "Grupo eliminado exitosamente. Los participantes han sido notificados.",
                "participantesNotificados", participantesAceptados.size() + participantesPendientes.size()
            ));
        }
        
        // REGLA 3: Si han pasado más de 24 horas Y hay participantes aceptados,
        // se requiere votación (por implementar) o justificación especial
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                "error", "No se puede eliminar el grupo automáticamente",
                "razon", "Han pasado más de 24 horas desde la creación y hay " + participantesAceptados.size() + " participantes activos",
                "solucion", "Contacta al soporte de AddVenture para solicitudes de eliminación especiales",
                "tiempoTranscurrido", String.format("%.1f horas", (double) tiempoDesdeCreacion.toMinutes() / 60.0),
                "participantesActivos", participantesAceptados.size(),
                "participantesPendientes", participantesPendientes.size(),
                "contactoSoporte", "support@addventure.com"
            ));
    }

    private void notificarEliminacionGrupo(GrupoViaje grupo) {
        // Obtener todos los participantes del grupo (de todos los estados)
        List<ParticipanteGrupo> participantesAceptados = participanteGrupoRepository
                .findByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.ACEPTADO);
        List<ParticipanteGrupo> participantesPendientes = participanteGrupoRepository
                .findByGrupoAndEstadoSolicitud(grupo, EstadoSolicitud.PENDIENTE);
        
        // Notificar a participantes aceptados
        for (ParticipanteGrupo participante : participantesAceptados) {
            try {
                crearNotificacionEliminacionGrupo(participante.getUsuario(), grupo, "GRUPO_ELIMINADO");
            } catch (Exception e) {
                System.err.println("Error al enviar notificación de eliminación a " + 
                                 participante.getUsuario().getEmail() + ": " + e.getMessage());
            }
        }
        
        // Notificar a participantes con solicitudes pendientes
        for (ParticipanteGrupo participante : participantesPendientes) {
            try {
                crearNotificacionEliminacionGrupo(participante.getUsuario(), grupo, "SOLICITUD_CANCELADA");
            } catch (Exception e) {
                System.err.println("Error al enviar notificación de cancelación a " + 
                                 participante.getUsuario().getEmail() + ": " + e.getMessage());
            }
        }
    }
    
    private void crearNotificacionEliminacionGrupo(Usuario usuario, GrupoViaje grupo, String tipo) {
        String contenido;
        if ("GRUPO_ELIMINADO".equals(tipo)) {
            contenido = "El grupo '" + grupo.getNombreViaje() + "' ha sido eliminado por el creador. " +
                       "Ya no tienes acceso al chat ni a la información del grupo.";
        } else {
            contenido = "Tu solicitud para unirte al grupo '" + grupo.getNombreViaje() + "' ha sido cancelada " +
                       "porque el grupo fue eliminado por el creador.";
        }
        
        // Crear la notificación directamente usando el repository
        Notificacion notificacion = Notificacion.builder()
                .tipo(tipo)
                .contenido(contenido)
                .usuario(usuario)
                .grupo(grupo)
                .leido(false)
                .estado("activo")
                .build();
                
        notificacionRepository.save(notificacion);
    }

    private void eliminarGrupoDirectamente(GrupoViaje grupo) {
        // 1. Notificar a todos los participantes sobre la eliminación
        notificarEliminacionGrupo(grupo);
        
        // 2. Eliminar notificaciones asociadas al grupo
        notificacionRepository.deleteAll(notificacionRepository.findByGrupo(grupo));
        
        // 3. Eliminar roles de usuarios asociados al grupo
        usuarioRolGrupoRepository.deleteAll(usuarioRolGrupoRepository.findByGrupo(grupo));
        
        // 4. Eliminar el viaje asociado
        Viaje viaje = grupo.getViaje();
        if (viaje != null) {
            viajeRepository.delete(viaje);
        }
        
        // 5. Eliminar el grupo (las otras relaciones se eliminan por cascade)
        grupoViajeRepository.delete(grupo);
    }

}
