package com.add.venture.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.add.venture.dto.PerfilUsuarioDTO;
import com.add.venture.dto.RegistroUsuarioDTO;
import com.add.venture.model.Usuario;
import com.add.venture.repository.UsuarioRepository;

@Service
public class UsuarioServiceImpl implements IUsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioDetallesService usuarioDetallesService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void crearUsuario(RegistroUsuarioDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setApellidos(dto.getApellido());
        usuario.setNombreUsuario(dto.getNombreUsuario());
        usuario.setEmail(dto.getEmail());
        usuario.setTelefono(dto.getTelefono());
        usuario.setPais(dto.getPais());
        usuario.setCiudad(dto.getCiudad());
        usuario.setFechaNacimiento(dto.getFechaNacimiento());
        usuario.setContrasenaHash(passwordEncoder.encode(dto.getContrasena()));
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setEsVerificado(false);
        usuario.setEstadoCuenta("activa");
        usuario.setEstado("activo");
        usuarioRepository.save(usuario);
    }

    @Override
    public boolean existeNombreUsuario(String nombreUsuario) {
        return usuarioRepository.existsByNombreUsuario(nombreUsuario);
    }

    @Override
    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    /**
     * Busca un usuario en la base de datos por su correo electrónico y lo mapea a
     * un DTO de registro.
     *
     * @param email el correo electrónico del usuario a buscar.
     * @return un objeto {@link RegistroUsuarioDTO} con los datos del usuario
     *         encontrado;
     *         si no se encuentra ningún usuario con el correo dado, retorna
     *         {@code null}.
     *
     *         <p>
     *         Este método utiliza el repositorio {@code usuarioRepository} para
     *         buscar
     *         un usuario por su email. Si el usuario existe, se crea un nuevo
     *         {@code RegistroUsuarioDTO} con sus datos principales.
     *         </p>
     *
     *         <p>
     *         Los dos últimos parámetros del DTO se establecen como {@code null}.
     *         </p>
     */
    @Override
    public RegistroUsuarioDTO buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .map(usuario -> new RegistroUsuarioDTO(
                        usuario.getNombre(),
                        usuario.getApellidos(),
                        usuario.getNombreUsuario(),
                        usuario.getEmail(),
                        usuario.getTelefono(),
                        usuario.getPais(),
                        usuario.getCiudad(),
                        usuario.getFechaNacimiento(),
                        null,
                        null))
                .orElse(null);
    }

    @Override
    public PerfilUsuarioDTO buscarPerfilPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .map(usuario -> new PerfilUsuarioDTO(
                        usuario.getNombre(),
                        usuario.getApellidos(),
                        usuario.getNombreUsuario(),
                        usuario.getEmail(),
                        usuario.getTelefono(),
                        usuario.getPais(),
                        usuario.getCiudad(),
                        usuario.getFechaNacimiento(),
                        usuario.getDescripcion(),
                        usuario.getFotoPerfil(),
                        usuario.getFotoPortada(),
                        usuario.getFechaRegistro() != null ? Timestamp.valueOf(usuario.getFechaRegistro()) : null // PASO DE Timestamp a LocalDateTime
                ))
                .orElse(null);
    }

    @Override
    public void actualizarPerfil(PerfilUsuarioDTO dto, MultipartFile imagenPerfil, MultipartFile imagenPortada) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setNombre(dto.getNombre());
        usuario.setApellidos(dto.getApellido());

        if (!usuario.getNombreUsuario().equals(dto.getUsername())) {
            if (usuarioRepository.existsByNombreUsuario(dto.getUsername())) {
                throw new RuntimeException("El nombre de usuario ya está en uso");
            }
            usuario.setNombreUsuario(dto.getUsername());
        }

        usuario.setTelefono(dto.getTelefono());
        usuario.setPais(dto.getPais());
        usuario.setCiudad(dto.getCiudad());
        usuario.setFechaNacimiento(dto.getFechaNacimiento());
        usuario.setDescripcion(dto.getBiografia());

        // Manejo de imágenes: eliminar la antigua antes de guardar la nueva
        if (imagenPerfil != null && !imagenPerfil.isEmpty()) {
            // Eliminar imagen antigua de perfil
            if (usuario.getFotoPerfil() != null && !usuario.getFotoPerfil().isEmpty()) {
                try {
                    java.nio.file.Path pathAntiguoPerfil = java.nio.file.Paths
                            .get("uploads/" + usuario.getFotoPerfil());
                    java.nio.file.Files.deleteIfExists(pathAntiguoPerfil);
                } catch (Exception e) {
                    e.printStackTrace(); // puedes cambiar por un logger
                }
            }
            String nombreArchivoPerfil = guardarArchivo(imagenPerfil);
            usuario.setFotoPerfil(nombreArchivoPerfil);
        }

        if (imagenPortada != null && !imagenPortada.isEmpty()) {
            // Eliminar imagen antigua de portada
            if (usuario.getFotoPortada() != null && !usuario.getFotoPortada().isEmpty()) {
                try {
                    java.nio.file.Path pathAntiguoPortada = java.nio.file.Paths
                            .get("uploads/" + usuario.getFotoPortada());
                    java.nio.file.Files.deleteIfExists(pathAntiguoPortada);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String nombreArchivoPortada = guardarArchivo(imagenPortada);
            usuario.setFotoPortada(nombreArchivoPortada);
        }

        usuarioRepository.save(usuario);

        // Reautenticación
        UserDetails userDetails = usuarioDetallesService.loadUserByUsername(usuario.getEmail());
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

    private String guardarArchivo(MultipartFile archivo) {
        try {
            String originalFilename = archivo.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String nuevoNombre = java.util.UUID.randomUUID().toString() + extension;

            String ruta = "uploads/"; // crea esta carpeta dentro de /src/main/resources/static
            java.nio.file.Path path = java.nio.file.Paths.get(ruta + nuevoNombre);
            java.nio.file.Files.createDirectories(path.getParent());
            archivo.transferTo(path);

            return nuevoNombre;
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar archivo", e);
        }
    }

    @Override
    public boolean existeNombreUsuarioExceptoActual(String nombreUsuario, String emailActual) {
        return usuarioRepository.existsByNombreUsuarioAndEmailNot(nombreUsuario, emailActual);
    }

    @Override
    public boolean existeTelefono(String telefono) {
        return usuarioRepository.existsByTelefono(telefono);
    }
}
