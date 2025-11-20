package com.add.venture.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.add.venture.dto.HomeDataDTO;
import com.add.venture.model.Usuario;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.service.INotificacionService;

@RestController
@RequestMapping("/api/home")
@CrossOrigin(origins = "http://localhost:4200")
public class HomeRestController {
    
    @Autowired
    private INotificacionService notificacionService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<HomeDataDTO> getHomeData() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        HomeDataDTO homeData = new HomeDataDTO();
        
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {

            // El username puede ser email o teléfono gracias a UsuarioDetallesService
            String username = authentication.getName();
            
            // Buscar usuario por email primero
            Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseGet(() -> usuarioRepository.findByTelefono(username).orElse(null));

            if (usuario != null) {
                // Crear iniciales
                String iniciales = "";
                if (usuario.getNombre() != null && !usuario.getNombre().isEmpty()) {
                    iniciales += usuario.getNombre().charAt(0);
                }
                if (usuario.getApellidos() != null && !usuario.getApellidos().isEmpty()) {
                    iniciales += usuario.getApellidos().charAt(0);
                }
                
                homeData.setIniciales(iniciales.toUpperCase());
                homeData.setUsername(usuario.getNombre() + " " + usuario.getApellidos());
                homeData.setEmail(usuario.getEmail());
                homeData.setImagenPerfil(usuario.getFotoPerfil());
                homeData.setImagenPortada(usuario.getFotoPortada());
                homeData.setAuthenticated(true);

                // Cargar notificaciones no leídas
                try {
                    long notificacionesNoLeidas = notificacionService.contarNotificacionesNoLeidas(usuario);
                    homeData.setNotificacionesNoLeidas(notificacionesNoLeidas);
                } catch (Exception e) {
                    homeData.setNotificacionesNoLeidas(0L);
                }
            } else {
                // Usuario autenticado pero no encontrado en BD
                homeData.setAuthenticated(false);
                homeData.setNotificacionesNoLeidas(0L);
            }
        } else {
            // Usuario no autenticado
            homeData.setAuthenticated(false);
            homeData.setNotificacionesNoLeidas(0L);
        }

        return ResponseEntity.ok(homeData);
    }
}
