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
import com.add.venture.model.Usuario;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.service.IUsuarioService;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    @Autowired
    private UsuarioAutenticadoHelper usuarioHelper;

    @Autowired
    private IUsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String mostrarVistaPerfil(Model model) {
        usuarioHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioHelper.cargarUsuarioParaPerfil(model);
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
}
