package com.add.venture.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.add.venture.dto.ActualizarPerfilDTO;
import com.add.venture.dto.PerfilResponseDTO;
import com.add.venture.dto.PerfilResponseDTO.AutorResenaDTO;
import com.add.venture.dto.PerfilResponseDTO.GrupoSimpleDTO;
import com.add.venture.dto.PerfilResponseDTO.LogroDTO;
import com.add.venture.dto.PerfilResponseDTO.ResenaDTO;
import com.add.venture.dto.PerfilResponseDTO.ViajePerfilDTO;
import com.add.venture.model.GrupoViaje;
import com.add.venture.model.ParticipanteGrupo;
import com.add.venture.model.Resena;
import com.add.venture.model.Usuario;
import com.add.venture.model.UsuarioLogro;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.ParticipanteGrupoRepository;
import com.add.venture.repository.ResenaRepository;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.service.ILogroService;

@RestController
@RequestMapping("/api/perfil")
@CrossOrigin(origins = "http://localhost:4200")
public class PerfilRestController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private ILogroService logroService;

    @Autowired
    private GrupoViajeRepository grupoViajeRepository;

    @Autowired
    private ParticipanteGrupoRepository participanteGrupoRepository;

    /**
     * Obtener el perfil del usuario autenticado
     */
    @GetMapping
    public ResponseEntity<PerfilResponseDTO> getPerfilPropio() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseGet(() -> usuarioRepository.findByTelefono(username).orElse(null));

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        PerfilResponseDTO perfilResponse = construirPerfilResponse(usuario, true);
        return ResponseEntity.ok(perfilResponse);
    }

    /**
     * Obtener el perfil de otro usuario por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PerfilResponseDTO> getPerfilUsuario(@PathVariable Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);

        if (usuario == null || !"activa".equals(usuario.getEstadoCuenta())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        PerfilResponseDTO perfilResponse = construirPerfilResponse(usuario, false);
        return ResponseEntity.ok(perfilResponse);
    }

    /**
     * Construye el DTO de respuesta del perfil
     */
    private PerfilResponseDTO construirPerfilResponse(Usuario usuario, boolean esPerfilPropio) {
        PerfilResponseDTO response = new PerfilResponseDTO();

        // Datos básicos
        response.setIdUsuario(usuario.getIdUsuario());
        response.setNombre(usuario.getNombre());
        response.setApellidos(usuario.getApellidos());
        response.setUsername(usuario.getNombreUsuario());
        response.setCiudad(usuario.getCiudad());
        response.setPais(usuario.getPais());
        response.setBiografia(usuario.getDescripcion());
        response.setImagenPerfil(usuario.getFotoPerfil());
        response.setImagenPortada(usuario.getFotoPortada());
        response.setEsPerfilPropio(esPerfilPropio);

        // Solo mostrar email y teléfono si es perfil propio
        if (esPerfilPropio) {
            response.setEmail(usuario.getEmail());
            response.setTelefono(usuario.getTelefono());
        }

        // Calcular edad si tiene fecha de nacimiento
        if (usuario.getFechaNacimiento() != null) {
            int edad = java.time.LocalDate.now().getYear() - usuario.getFechaNacimiento().getYear();
            if (java.time.LocalDate.now().getDayOfYear() < usuario.getFechaNacimiento().getDayOfYear()) {
                edad--;
            }
            response.setEdad(edad);
        }

        // Iniciales
        String iniciales = "";
        if (usuario.getNombre() != null && !usuario.getNombre().isEmpty()) {
            iniciales += usuario.getNombre().charAt(0);
        }
        if (usuario.getApellidos() != null && !usuario.getApellidos().isEmpty()) {
            iniciales += usuario.getApellidos().charAt(0);
        }
        response.setIniciales(iniciales.toUpperCase());

        // Fecha de registro formateada
        if (usuario.getFechaRegistro() != null) {
            String mes = usuario.getFechaRegistro().getMonth()
                    .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES"));
            int anio = usuario.getFechaRegistro().getYear();
            String fechaFormateada = mes.substring(0, 1).toUpperCase() + mes.substring(1) + " " + anio;
            response.setFechaRegistroFormateada(fechaFormateada);
        }

        // Estadísticas de reseñas
        long totalResenas = resenaRepository.countByDestinatario(usuario);
        Double promedioCalificaciones = resenaRepository.calcularPromedioCalificaciones(usuario);
        String promedioFormateado = promedioCalificaciones != null
                ? String.format("%.1f", promedioCalificaciones)
                : "0.0";

        response.setTotalResenas(totalResenas);
        response.setPromedioCalificaciones(promedioFormateado);

        // Verificar si el usuario está verificado (5+ reseñas con promedio >= 4)
        boolean verificado = totalResenas >= 5 && promedioCalificaciones != null && promedioCalificaciones >= 4.0;
        response.setVerificado(verificado);

        // Viajes completados (grupos donde ha sido calificado)
        long viajesCompletados = resenaRepository.countDistinctGruposByDestinatario(usuario);
        response.setViajesCompletados(viajesCompletados);

        // Verificar y otorgar logros antes de cargar
        logroService.verificarLogroPioneer(usuario);
        logroService.verificarLogroPathfinder(usuario);
        logroService.verificarLogroVerificado(usuario);

        // Logros
        List<UsuarioLogro> logrosUsuario = logroService.obtenerLogrosDeUsuario(usuario);
        long totalLogros = logroService.contarLogrosDeUsuario(usuario);
        response.setTotalLogros(totalLogros);
        response.setLogros(convertirLogros(logrosUsuario));

        // Reseñas recientes (últimas 5)
        List<Resena> resenasRecientes = resenaRepository.findTopResenasDelUsuario(usuario, 5);
        response.setResenasRecientes(convertirResenas(resenasRecientes));

        // Viajes
        cargarViajes(usuario, response);

        return response;
    }

    /**
     * Convierte las reseñas a DTOs
     */
    private List<ResenaDTO> convertirResenas(List<Resena> resenas) {
        return resenas.stream().map(resena -> {
            ResenaDTO dto = new ResenaDTO();
            dto.setIdResena(resena.getIdResena());
            dto.setComentario(resena.getComentario());
            dto.setCalificacion(resena.getCalificacion());
            
            if (resena.getFecha() != null) {
                dto.setFecha(resena.getFecha().toString());
            }

            // Autor
            AutorResenaDTO autor = new AutorResenaDTO();
            autor.setIdUsuario(resena.getAutor().getIdUsuario());
            autor.setNombre(resena.getAutor().getNombre());
            autor.setApellidos(resena.getAutor().getApellidos());
            autor.setFotoPerfil(resena.getAutor().getFotoPerfil());
            
            String inicialesAutor = "";
            if (resena.getAutor().getNombre() != null && !resena.getAutor().getNombre().isEmpty()) {
                inicialesAutor += resena.getAutor().getNombre().charAt(0);
            }
            if (resena.getAutor().getApellidos() != null && !resena.getAutor().getApellidos().isEmpty()) {
                inicialesAutor += resena.getAutor().getApellidos().charAt(0);
            }
            autor.setIniciales(inicialesAutor.toUpperCase());
            dto.setAutor(autor);

            // Grupo
            GrupoSimpleDTO grupo = new GrupoSimpleDTO();
            grupo.setIdGrupo(resena.getGrupo().getIdGrupo());
            grupo.setNombreViaje(resena.getGrupo().getNombreViaje());
            dto.setGrupo(grupo);

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Convierte los logros a DTOs
     */
    private List<LogroDTO> convertirLogros(List<UsuarioLogro> usuarioLogros) {
        return usuarioLogros.stream().map(ul -> {
            LogroDTO dto = new LogroDTO();
            dto.setIdLogro(ul.getLogro().getIdLogro());
            dto.setNombre(ul.getLogro().getNombre());
            dto.setDescripcion(ul.getLogro().getDescripcion());
            dto.setIcono(ul.getLogro().getIcono());
            if (ul.getFechaOtorgado() != null) {
                dto.setFechaOtorgado(ul.getFechaOtorgado().toString());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Carga los datos de viajes (próximos e historial)
     */
    private void cargarViajes(Usuario usuario, PerfilResponseDTO response) {
        List<GrupoViaje> gruposCreados = grupoViajeRepository.findByCreadorOrderByFechaCreacionDesc(usuario);
        List<ParticipanteGrupo> participaciones = participanteGrupoRepository
                .findByUsuarioAndEstadoSolicitudOrderByFechaUnionDesc(
                        usuario, ParticipanteGrupo.EstadoSolicitud.ACEPTADO);

        List<ViajePerfilDTO> proximosViajes = new ArrayList<>();
        List<ViajePerfilDTO> historialViajes = new ArrayList<>();

        // Procesar grupos creados
        for (GrupoViaje grupo : gruposCreados) {
            ViajePerfilDTO viaje = convertirGrupoAViajeDTO(grupo);
            if ("activo".equals(grupo.getEstado())) {
                proximosViajes.add(viaje);
            } else if ("cerrado".equals(grupo.getEstado()) || "concluido".equals(grupo.getEstado())) {
                historialViajes.add(viaje);
            }
        }

        // Procesar grupos donde participa
        for (ParticipanteGrupo participacion : participaciones) {
            GrupoViaje grupo = participacion.getGrupo();
            ViajePerfilDTO viaje = convertirGrupoAViajeDTO(grupo);
            if ("activo".equals(grupo.getEstado())) {
                proximosViajes.add(viaje);
            } else if ("cerrado".equals(grupo.getEstado()) || "concluido".equals(grupo.getEstado())) {
                historialViajes.add(viaje);
            }
        }

        response.setProximosViajes(proximosViajes);
        response.setHistorialViajes(historialViajes);
        response.setTotalProximosViajes(proximosViajes.size());
        response.setTotalHistorialViajes(historialViajes.size());
    }

    /**
     * Convierte un GrupoViaje a ViajePerfilDTO
     */
    private ViajePerfilDTO convertirGrupoAViajeDTO(GrupoViaje grupo) {
        ViajePerfilDTO dto = new ViajePerfilDTO();
        dto.setIdGrupo(grupo.getIdGrupo());
        dto.setNombreViaje(grupo.getNombreViaje());
        dto.setEstado(grupo.getEstado());
        
        // Obtener datos del viaje asociado
        if (grupo.getViaje() != null) {
            dto.setDestinoPrincipal(grupo.getViaje().getDestinoPrincipal());
            dto.setImagenDestacada(grupo.getViaje().getImagenDestacada());
            
            if (grupo.getViaje().getFechaInicio() != null) {
                dto.setFechaInicio(grupo.getViaje().getFechaInicio().toString());
            }
            if (grupo.getViaje().getFechaFin() != null) {
                dto.setFechaFin(grupo.getViaje().getFechaFin().toString());
            }
        }
        
        return dto;
    }

    /**
     * Actualizar perfil del usuario autenticado (sin imágenes)
     */
    @PutMapping
    public ResponseEntity<?> actualizarPerfil(@RequestBody ActualizarPerfilDTO dto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String username = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(username)
                    .orElseGet(() -> usuarioRepository.findByTelefono(username).orElse(null));

            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            // Actualizar datos básicos
            usuario.setNombre(dto.getNombre());
            usuario.setApellidos(dto.getApellidos());

            // Verificar si el username cambió y si está disponible
            if (!usuario.getNombreUsuario().equals(dto.getUsername())) {
                if (usuarioRepository.existsByNombreUsuario(dto.getUsername())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "El nombre de usuario ya está en uso"));
                }
                usuario.setNombreUsuario(dto.getUsername());
            }

            usuario.setTelefono(dto.getTelefono());
            usuario.setPais(dto.getPais());
            usuario.setCiudad(dto.getCiudad());
            usuario.setFechaNacimiento(dto.getFechaNacimiento());
            usuario.setDescripcion(dto.getBiografia());

            usuarioRepository.save(usuario);

            // Devolver el perfil actualizado
            PerfilResponseDTO perfilActualizado = construirPerfilResponse(usuario, true);
            return ResponseEntity.ok(perfilActualizado);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar el perfil: " + e.getMessage()));
        }
    }

    /**
     * Subir imagen de perfil
     */
    @PostMapping("/imagen-perfil")
    public ResponseEntity<?> subirImagenPerfil(@RequestParam("imagen") MultipartFile imagen) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String username = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(username)
                    .orElseGet(() -> usuarioRepository.findByTelefono(username).orElse(null));

            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            // Validar archivo
            if (imagen.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No se ha seleccionado ninguna imagen"));
            }

            String contentType = imagen.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El archivo debe ser una imagen"));
            }

            // Eliminar imagen anterior si existe
            if (usuario.getFotoPerfil() != null && !usuario.getFotoPerfil().isEmpty()) {
                try {
                    Path pathAntiguo = Paths.get("uploads/" + usuario.getFotoPerfil());
                    Files.deleteIfExists(pathAntiguo);
                } catch (Exception e) {
                    // Ignorar errores al eliminar archivo antiguo
                }
            }

            // Guardar nueva imagen
            String nombreArchivo = UUID.randomUUID().toString() + "_" + imagen.getOriginalFilename();
            Path uploadPath = Paths.get("uploads");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(nombreArchivo);
            Files.copy(imagen.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            usuario.setFotoPerfil(nombreArchivo);
            usuarioRepository.save(usuario);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Imagen de perfil actualizada correctamente",
                    "imagenUrl", nombreArchivo));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar la imagen: " + e.getMessage()));
        }
    }

    /**
     * Subir imagen de portada
     */
    @PostMapping("/imagen-portada")
    public ResponseEntity<?> subirImagenPortada(@RequestParam("imagen") MultipartFile imagen) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String username = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(username)
                    .orElseGet(() -> usuarioRepository.findByTelefono(username).orElse(null));

            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            // Validar archivo
            if (imagen.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No se ha seleccionado ninguna imagen"));
            }

            String contentType = imagen.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El archivo debe ser una imagen"));
            }

            // Eliminar imagen anterior si existe
            if (usuario.getFotoPortada() != null && !usuario.getFotoPortada().isEmpty()) {
                try {
                    Path pathAntiguo = Paths.get("uploads/" + usuario.getFotoPortada());
                    Files.deleteIfExists(pathAntiguo);
                } catch (Exception e) {
                    // Ignorar errores al eliminar archivo antiguo
                }
            }

            // Guardar nueva imagen
            String nombreArchivo = UUID.randomUUID().toString() + "_" + imagen.getOriginalFilename();
            Path uploadPath = Paths.get("uploads");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(nombreArchivo);
            Files.copy(imagen.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            usuario.setFotoPortada(nombreArchivo);
            usuarioRepository.save(usuario);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Imagen de portada actualizada correctamente",
                    "imagenUrl", nombreArchivo));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar la imagen: " + e.getMessage()));
        }
    }
}
