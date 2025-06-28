package com.add.venture.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import com.add.venture.dto.PerfilUsuarioDTO;
import com.add.venture.dto.RegistroUsuarioDTO;
import com.add.venture.model.Usuario;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.service.INotificacionService;
import com.add.venture.service.IUsuarioService;

@Component
public class UsuarioAutenticadoHelper {

    @Autowired
    private IUsuarioService usuarioService;
    
    @Autowired
    private INotificacionService notificacionService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    public void cargarDatosUsuarioParaNavbar(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {

            String correo = authentication.getName();
            RegistroUsuarioDTO usuarioDto = usuarioService.buscarPorEmail(correo);

            if (usuarioDto != null) {
                model.addAttribute("iniciales", usuarioDto.getIniciales());
                model.addAttribute("username", usuarioDto);

                // También cargar el perfil completo para el navbar (que necesita imagenPerfil)
                PerfilUsuarioDTO usuarioPerfil = usuarioService.buscarPerfilPorEmail(correo);
                if (usuarioPerfil != null) {
                    model.addAttribute("usuario", usuarioPerfil);
                }

                // Cargar número de notificaciones no leídas
                try {
                    Usuario usuarioEntity = usuarioRepository.findByEmail(correo).orElse(null);
                    if (usuarioEntity != null) {
                        long notificacionesNoLeidas = notificacionService.contarNotificacionesNoLeidas(usuarioEntity);
                        model.addAttribute("notificacionesNoLeidas", notificacionesNoLeidas);
                    } else {
                        model.addAttribute("notificacionesNoLeidas", 0L);
                    }
                } catch (Exception e) {
                    model.addAttribute("notificacionesNoLeidas", 0L);
                }
            }
        }
    }

    public void cargarUsuarioParaPerfil(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {

            String correo = authentication.getName();
            PerfilUsuarioDTO usuariodDto = usuarioService.buscarPerfilPorEmail(correo);
            if (usuariodDto != null) {
                model.addAttribute("iniciales", usuariodDto.getIniciales());
                System.out.println("imagenPortada: " + usuariodDto.getImagenPortada());
                model.addAttribute("usuario", usuariodDto); // para formulario de edición
            }
        }
    }

    

}
