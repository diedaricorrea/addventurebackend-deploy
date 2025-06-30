package com.add.venture.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.add.venture.dto.PerfilUsuarioDTO;
import com.add.venture.helper.UsuarioAutenticadoHelper;
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
import com.add.venture.service.IUsuarioService;

import java.util.List;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    @Autowired
    private UsuarioAutenticadoHelper usuarioHelper;

    @Autowired
    private IUsuarioService usuarioService;

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

    @GetMapping
    public String mostrarVistaPerfil(Model model) {
        usuarioHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioHelper.cargarUsuarioParaPerfil(model);
        
        // Cargar reseñas del usuario autenticado
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElse(null);
            if (usuario != null) {
                cargarDatosResenasParaPerfil(usuario, model);
            }
        }
        
        return "user/perfil";
    }

    @GetMapping("/{id}")
    public String verPerfilDeOtroUsuario(@PathVariable("id") Long idUsuario, Model model) {
        // Cargar datos del usuario para la navbar 
        usuarioHelper.cargarDatosUsuarioParaNavbar(model);
        
        // Buscar el usuario a mostrar
        Usuario usuarioAMostrar = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Verificar que el usuario existe y está activo
        if (!"activo".equals(usuarioAMostrar.getEstado())) {
            throw new RuntimeException("Usuario no disponible");
        }
        
        // Convertir a DTO para usar la misma estructura que el perfil propio
        PerfilUsuarioDTO usuarioDto = convertirAPerfilDTO(usuarioAMostrar);
        
        // Agregar el usuario al modelo usando la misma variable que el perfil propio
        model.addAttribute("usuario", usuarioDto);
        model.addAttribute("iniciales", usuarioDto.getIniciales());
        model.addAttribute("esPerfilPropio", false); // Indicar que no es el perfil propio
        
        // Cargar reseñas del usuario a mostrar
        cargarDatosResenasParaPerfil(usuarioAMostrar, model);
        
        return "user/perfil"; // Usar la misma vista que el perfil propio
    }
    
    private PerfilUsuarioDTO convertirAPerfilDTO(Usuario usuario) {
        PerfilUsuarioDTO dto = new PerfilUsuarioDTO();
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellidos()); // DTO usa 'apellido' (singular)
        dto.setUsername(usuario.getNombreUsuario());
        dto.setEmail(usuario.getEmail());
        dto.setTelefono(usuario.getTelefono());
        dto.setBiografia(usuario.getDescripcion());
        dto.setCiudad(usuario.getCiudad());
        dto.setPais(usuario.getPais());
        dto.setFechaNacimiento(usuario.getFechaNacimiento());
        dto.setImagenPerfil(usuario.getFotoPerfil());
        dto.setImagenPortada(usuario.getFotoPortada());
        
        // Convertir LocalDateTime a Timestamp para fechaRegistro
        if (usuario.getFechaRegistro() != null) {
            dto.setFechaRegistro(java.sql.Timestamp.valueOf(usuario.getFechaRegistro()));
        }
        
        return dto;
    }

    // Configura el data binder ANTES de procesar la petición
    @InitBinder("usuario")
    // "No intentes asignar los valores del formulario llamados imagenPerfil y
    // imagenPortada al objeto usuario (PerfilUsuarioDTO), porque esos campos no le
    // pertenecen."
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("imagenPerfil", "imagenPortada");
    }

    @GetMapping("/configuracion")
    public String mostrarVistaConfiguracion(Model model) {
        usuarioHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioHelper.cargarUsuarioParaPerfil(model);
        return "user/configuracion";
    }

    @PostMapping("/configuracion")
    public String actualizarConfiguracion(
            @ModelAttribute("usuario") PerfilUsuarioDTO perfilDto,
            @RequestParam(value = "imagenPerfil", required = false) MultipartFile imagenPerfil,
            @RequestParam(value = "imagenPortada", required = false) MultipartFile imagenPortada,
            Model model) {

        usuarioService.actualizarPerfil(perfilDto, imagenPerfil, imagenPortada);

        usuarioHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioHelper.cargarUsuarioParaPerfil(model);
        model.addAttribute("mensaje", "Perfil actualizado correctamente");

        return "user/configuracion";
    }
    
    /**
     * Método auxiliar para cargar datos de reseñas, logros y viajes en el perfil
     */
    private void cargarDatosResenasParaPerfil(Usuario usuario, Model model) {
        // Obtener las últimas 5 reseñas del usuario
        List<Resena> resenasRecientes = resenaRepository.findTopResenasDelUsuario(usuario, 5);
        model.addAttribute("resenasRecientes", resenasRecientes);
        
        // Calcular estadísticas de reseñas
        Double promedioCalificaciones = resenaRepository.calcularPromedioCalificaciones(usuario);
        long totalResenas = resenaRepository.countByDestinatario(usuario);
        
        // Formatear el promedio para mostrar solo 1 decimal
        String promedioFormateado = "0.0";
        if (promedioCalificaciones != null) {
            promedioFormateado = String.format("%.1f", promedioCalificaciones);
        }
        
        model.addAttribute("promedioCalificaciones", promedioFormateado);
        model.addAttribute("totalResenas", totalResenas);
        
        // Verificar y otorgar logros antes de cargar
        logroService.verificarLogroPioneer(usuario);
        logroService.verificarLogroPathfinder(usuario);
        logroService.verificarLogroVerificado(usuario);
        
        // Obtener logros del usuario
        List<UsuarioLogro> logrosUsuario = logroService.obtenerLogrosDeUsuario(usuario);
        long totalLogros = logroService.contarLogrosDeUsuario(usuario);
        
        model.addAttribute("logrosUsuario", logrosUsuario);
        model.addAttribute("totalLogros", totalLogros);
        
        // Cargar datos de viajes
        cargarDatosViajes(usuario, model);
    }
    
    /**
     * Método auxiliar para cargar datos de viajes (próximos e historial)
     */
    private void cargarDatosViajes(Usuario usuario, Model model) {
        // Obtener grupos creados por el usuario
        List<GrupoViaje> gruposCreados = grupoViajeRepository.findByCreadorOrderByFechaCreacionDesc(usuario);
        
        // Obtener grupos donde participa
        List<ParticipanteGrupo> participaciones = participanteGrupoRepository.findByUsuarioAndEstadoSolicitudOrderByFechaUnionDesc(
                usuario, ParticipanteGrupo.EstadoSolicitud.ACEPTADO);
        
        // Separar próximos viajes (grupos activos) e historial (grupos cerrados/concluidos)
        List<GrupoViaje> proximosViajes = new java.util.ArrayList<>();
        List<GrupoViaje> historialViajes = new java.util.ArrayList<>();
        
        // Procesar grupos creados
        for (GrupoViaje grupo : gruposCreados) {
            if ("activo".equals(grupo.getEstado())) {
                proximosViajes.add(grupo);
            } else if ("cerrado".equals(grupo.getEstado()) || "concluido".equals(grupo.getEstado())) {
                historialViajes.add(grupo);
            }
        }
        
        // Procesar grupos donde participa
        for (ParticipanteGrupo participacion : participaciones) {
            GrupoViaje grupo = participacion.getGrupo();
            if ("activo".equals(grupo.getEstado())) {
                proximosViajes.add(grupo);
            } else if ("cerrado".equals(grupo.getEstado()) || "concluido".equals(grupo.getEstado())) {
                historialViajes.add(grupo);
            }
        }
        
        // Calcular viajes completados (solo aquellos donde el usuario ha sido calificado)
        long viajesCompletados = resenaRepository.countDistinctGruposByDestinatario(usuario);
        
        // Agregar datos al modelo
        model.addAttribute("proximosViajes", proximosViajes);
        model.addAttribute("historialViajes", historialViajes);
        model.addAttribute("viajesCompletados", viajesCompletados);
        model.addAttribute("totalProximosViajes", proximosViajes.size());
        model.addAttribute("totalHistorialViajes", historialViajes.size());
    }
}
