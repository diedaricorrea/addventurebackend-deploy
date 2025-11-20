package com.add.venture.controller;

import com.add.venture.dto.AuthResponseDTO;
import com.add.venture.dto.LoginRequestDTO;
import com.add.venture.dto.RegistroUsuarioDTO;
import com.add.venture.dto.UserInfoDTO;
import com.add.venture.model.Usuario;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.security.JwtService;
import com.add.venture.service.IUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final IUsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            // Intentar autenticar con el username proporcionado (puede ser email o teléfono)
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            // Cargar el usuario
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
            
            // Buscar el usuario completo en la base de datos
            Usuario usuario = usuarioRepository.findByEmail(loginRequest.getUsername())
                .orElseGet(() -> usuarioRepository.findByTelefono(loginRequest.getUsername())
                    .orElse(null));

            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Usuario no encontrado"));
            }

            // Generar el token JWT
            String token = jwtService.generateToken(userDetails);

            // Crear el DTO de información del usuario
            UserInfoDTO userInfo = UserInfoDTO.builder()
                .id(usuario.getIdUsuario())
                .nombreUsuario(usuario.getNombreUsuario())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellidos())
                .iniciales(obtenerIniciales(usuario))
                .imagenPerfil(usuario.getFotoPerfil())
                .roles(List.of("ROLE_USER"))
                .build();

            // Crear la respuesta
            AuthResponseDTO response = AuthResponseDTO.builder()
                .token(token)
                .tipo("Bearer")
                .usuario(userInfo)
                .build();

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Usuario o contraseña incorrectos");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Error al iniciar sesión: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistroUsuarioDTO registroDTO) {
        try {
            // Registrar el usuario usando el servicio existente
            usuarioService.crearUsuario(registroDTO);
            
            // Buscar el usuario recién creado
            Usuario nuevoUsuario = usuarioRepository.findByEmail(registroDTO.getEmail())
                .orElseThrow(() -> new Exception("Usuario creado pero no encontrado"));

            // Cargar UserDetails
            UserDetails userDetails = userDetailsService.loadUserByUsername(nuevoUsuario.getEmail());

            // Generar token JWT
            String token = jwtService.generateToken(userDetails);

            // Crear información del usuario
            UserInfoDTO userInfo = UserInfoDTO.builder()
                .id(nuevoUsuario.getIdUsuario())
                .nombreUsuario(nuevoUsuario.getNombreUsuario())
                .email(nuevoUsuario.getEmail())
                .nombre(nuevoUsuario.getNombre())
                .apellido(nuevoUsuario.getApellidos())
                .iniciales(obtenerIniciales(nuevoUsuario))
                .imagenPerfil(nuevoUsuario.getFotoPerfil())
                .roles(List.of("ROLE_USER"))
                .build();

            // Crear respuesta
            AuthResponseDTO response = AuthResponseDTO.builder()
                .token(token)
                .tipo("Bearer")
                .usuario(userInfo)
                .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Error al registrar usuario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsernameAvailability(@RequestParam String username) {
        boolean available = !usuarioRepository.existsByNombreUsuario(username);
        return ResponseEntity.ok(Map.of("available", available));
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmailAvailability(@RequestParam String email) {
        boolean available = !usuarioRepository.existsByEmail(email);
        return ResponseEntity.ok(Map.of("available", available));
    }

    private String obtenerIniciales(Usuario usuario) {
        String iniciales = "";
        if (usuario.getNombre() != null && !usuario.getNombre().isEmpty()) {
            iniciales += usuario.getNombre().charAt(0);
        }
        if (usuario.getApellidos() != null && !usuario.getApellidos().isEmpty()) {
            iniciales += usuario.getApellidos().charAt(0);
        }
        return iniciales.toUpperCase();
    }
}
