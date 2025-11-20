package com.add.venture.service;

import com.add.venture.model.Usuario;
import com.add.venture.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioDetallesService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Intentar buscar por email primero
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseGet(() -> 
                    // Si no se encuentra por email, intentar por teléfono
                    usuarioRepository.findByTelefono(username)
                        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username))
                );

        return new User(
                usuario.getEmail(),
                usuario.getContrasenaHash(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
