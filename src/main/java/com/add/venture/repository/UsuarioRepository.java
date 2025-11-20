package com.add.venture.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.add.venture.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Método para buscar un usuario por su nombre de usuario
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    // Método para buscar un usuario por su email
    Optional<Usuario> findByEmail(String email);

    // Método para buscar un usuario por su teléfono
    Optional<Usuario> findByTelefono(String telefono);

    // Método para verificar si un nombre de usuario ya existe
    boolean existsByNombreUsuario(String nombreUsuario);

    // Método para verificar si un email ya existe
    boolean existsByEmail(String email);

    // Método para verificar si un teléfono ya existe
    boolean existsByTelefono(String telefono);

    // Método para verificar si un nombre de usuario existe, excepto el del usuario actual
    boolean existsByNombreUsuarioAndEmailNot(String nombreUsuario, String email);
}
