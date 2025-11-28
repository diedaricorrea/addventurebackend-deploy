package com.add.venture.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.add.venture.dto.TestimonioDTO;
import com.add.venture.model.Usuario;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.service.ITestimonioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/testimonios")
@CrossOrigin(origins = "http://localhost:4200")
public class TestimonioRestController {

    @Autowired
    private ITestimonioService testimonioService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Crear un nuevo testimonio
     * POST /api/testimonios
     */
    @PostMapping
    public ResponseEntity<?> crearTestimonio(@Valid @RequestBody TestimonioDTO testimonioDTO) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            Usuario autor = usuarioRepository.findByEmail(username)
                    .orElseGet(() -> usuarioRepository.findByTelefono(username).orElse(null));
            
            if (autor == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }
            
            TestimonioDTO nuevoTestimonio = testimonioService.crearTestimonio(testimonioDTO, autor);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "¡Gracias por compartir tu experiencia! Tu testimonio será revisado pronto.");
            response.put("testimonio", nuevoTestimonio);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Obtener testimonios destacados para el index (público)
     * GET /api/testimonios/destacados
     */
    @GetMapping("/destacados")
    public ResponseEntity<List<TestimonioDTO>> obtenerDestacados(
            @RequestParam(defaultValue = "6") int limit) {
        List<TestimonioDTO> testimonios = testimonioService.obtenerTestimoniosDestacados(limit);
        return ResponseEntity.ok(testimonios);
    }

    /**
     * Obtener todos los testimonios aprobados (público)
     * GET /api/testimonios/aprobados
     */
    @GetMapping("/aprobados")
    public ResponseEntity<List<TestimonioDTO>> obtenerAprobados(
            @RequestParam(defaultValue = "20") int limit) {
        List<TestimonioDTO> testimonios = testimonioService.obtenerTestimoniosAprobados(limit);
        return ResponseEntity.ok(testimonios);
    }

    /**
     * Obtener testimonios pendientes de aprobación (solo admin)
     * GET /api/testimonios/pendientes
     */
    @GetMapping("/pendientes")
    public ResponseEntity<?> obtenerPendientes() {
        try {
            // TODO: Verificar que el usuario sea admin
            List<TestimonioDTO> testimonios = testimonioService.obtenerTestimoniosPendientes();
            return ResponseEntity.ok(testimonios);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permisos para esta acción"));
        }
    }

    /**
     * Aprobar un testimonio (solo admin)
     * PUT /api/testimonios/{id}/aprobar
     */
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarTestimonio(@PathVariable Long id) {
        try {
            // TODO: Verificar que el usuario sea admin
            TestimonioDTO testimonio = testimonioService.aprobarTestimonio(id);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Testimonio aprobado exitosamente",
                "testimonio", testimonio
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Marcar/desmarcar testimonio como destacado (solo admin)
     * PUT /api/testimonios/{id}/destacar
     */
    @PutMapping("/{id}/destacar")
    public ResponseEntity<?> marcarDestacado(
            @PathVariable Long id,
            @RequestParam boolean destacado) {
        try {
            // TODO: Verificar que el usuario sea admin
            TestimonioDTO testimonio = testimonioService.marcarDestacado(id, destacado);
            return ResponseEntity.ok(Map.of(
                "mensaje", destacado ? "Testimonio marcado como destacado" : "Testimonio desmarcado",
                "testimonio", testimonio
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Eliminar un testimonio (solo admin o autor)
     * DELETE /api/testimonios/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarTestimonio(@PathVariable Long id) {
        try {
            // TODO: Verificar permisos (admin o autor del testimonio)
            testimonioService.eliminarTestimonio(id);
            return ResponseEntity.ok(Map.of("mensaje", "Testimonio eliminado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
