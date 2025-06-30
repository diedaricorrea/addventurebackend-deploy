package com.add.venture.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.add.venture.model.GrupoViaje;
import com.add.venture.model.Logro;
import com.add.venture.model.ParticipanteGrupo;
import com.add.venture.model.Usuario;
import com.add.venture.model.UsuarioLogro;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.LogroRepository;
import com.add.venture.repository.ParticipanteGrupoRepository;
import com.add.venture.repository.ResenaRepository;
import com.add.venture.repository.UsuarioLogroRepository;

@Service
@Transactional
public class LogroServiceImpl implements ILogroService {

    @Autowired
    private LogroRepository logroRepository;
    
    @Autowired
    private UsuarioLogroRepository usuarioLogroRepository;
    
    @Autowired
    private GrupoViajeRepository grupoViajeRepository;
    
    @Autowired
    private ParticipanteGrupoRepository participanteGrupoRepository;
    
    @Autowired
    private ResenaRepository resenaRepository;

    @Override
    public void verificarLogroPioneer(Usuario usuario) {
        try {
            // Buscar el logro Pioneer
            Optional<Logro> logroPioneer = logroRepository.findByNombre("Pioneer");
            if (logroPioneer.isEmpty()) {
                return; // Si no existe el logro, no hacer nada
            }
            
            // Verificar si el usuario ya tiene este logro
            if (usuarioLogroRepository.existsByUsuarioAndLogro(usuario, logroPioneer.get())) {
                return; // El usuario ya tiene este logro
            }
            
            // Verificar si el usuario ha participado en al menos un viaje cerrado/concluido
            List<ParticipanteGrupo> participaciones = participanteGrupoRepository
                    .findByUsuarioAndEstadoSolicitud(usuario, ParticipanteGrupo.EstadoSolicitud.ACEPTADO);
            
            boolean haCompletadoViaje = participaciones.stream()
                    .anyMatch(p -> "cerrado".equals(p.getGrupo().getEstado()) || "concluido".equals(p.getGrupo().getEstado()));
            
            if (haCompletadoViaje) {
                otorgarLogro(usuario, logroPioneer.get());
            }
        } catch (Exception e) {
            System.out.println("Error verificando logro Pioneer: " + e.getMessage());
        }
    }

    @Override
    public void verificarLogroPathfinder(Usuario usuario) {
        try {
            // Buscar el logro Pathfinder
            Optional<Logro> logroPathfinder = logroRepository.findByNombre("Pathfinder");
            if (logroPathfinder.isEmpty()) {
                return; // Si no existe el logro, no hacer nada
            }
            
            // Verificar si el usuario ya tiene este logro
            if (usuarioLogroRepository.existsByUsuarioAndLogro(usuario, logroPathfinder.get())) {
                return; // El usuario ya tiene este logro
            }
            
            // Verificar si el usuario ha creado al menos un grupo
            List<GrupoViaje> gruposCreados = grupoViajeRepository.findByCreador(usuario);
            
            if (!gruposCreados.isEmpty()) {
                otorgarLogro(usuario, logroPathfinder.get());
            }
        } catch (Exception e) {
            System.out.println("Error verificando logro Pathfinder: " + e.getMessage());
        }
    }

    @Override
    public void verificarLogroVerificado(Usuario usuario) {
        try {
            // Buscar el logro Verificado
            Optional<Logro> logroVerificado = logroRepository.findByNombre("Verificado");
            if (logroVerificado.isEmpty()) {
                return; // Si no existe el logro, no hacer nada
            }
            
            // Verificar si el usuario ya tiene este logro
            if (usuarioLogroRepository.existsByUsuarioAndLogro(usuario, logroVerificado.get())) {
                return; // El usuario ya tiene este logro
            }
            
            // Verificar si el usuario tiene al menos 5 reseñas positivas (calificación >= 4)
            boolean tieneResenasPositivas = resenaRepository.tieneMinimoResenasPositivas(usuario, 5L);
            
            if (tieneResenasPositivas) {
                otorgarLogro(usuario, logroVerificado.get());
                System.out.println("Usuario " + usuario.getEmail() + " obtuvo el logro 'Verificado' por tener 5+ reseñas positivas");
            }
        } catch (Exception e) {
            System.out.println("Error verificando logro Verificado: " + e.getMessage());
        }
    }

    @Override
    public List<UsuarioLogro> obtenerLogrosDeUsuario(Usuario usuario) {
        return usuarioLogroRepository.findByUsuarioOrderByFechaOtorgadoDesc(usuario);
    }

    @Override
    public List<UsuarioLogro> obtenerLogrosRecientes(Usuario usuario, int limite) {
        return usuarioLogroRepository.findTopLogrosByUsuario(usuario, limite);
    }

    @Override
    public long contarLogrosDeUsuario(Usuario usuario) {
        return usuarioLogroRepository.countByUsuario(usuario);
    }

    @Override
    public void inicializarLogrosBasicos() {
        try {
            // Crear logro Pioneer si no existe
            if (logroRepository.findByNombre("Pioneer").isEmpty()) {
                Logro pioneer = Logro.builder()
                        .nombre("Pioneer")
                        .descripcion("Completó su primer viaje")
                        .icono("bi bi-flag")
                        .puntosRequeridos(0)
                        .puntosOtorgados(10)
                        .build();
                logroRepository.save(pioneer);
            }
            
            // Crear logro Pathfinder si no existe
            if (logroRepository.findByNombre("Pathfinder").isEmpty()) {
                Logro pathfinder = Logro.builder()
                        .nombre("Pathfinder")
                        .descripcion("Creó su primer grupo")
                        .icono("bi bi-compass")
                        .puntosRequeridos(0)
                        .puntosOtorgados(15)
                        .build();
                logroRepository.save(pathfinder);
            }
            
            // Crear logro Verificado si no existe
            if (logroRepository.findByNombre("Verificado").isEmpty()) {
                Logro verificado = Logro.builder()
                        .nombre("Verificado")
                        .descripcion("Obtuvo 5+ reseñas positivas")
                        .icono("bi bi-shield-check")
                        .puntosRequeridos(0)
                        .puntosOtorgados(25)
                        .build();
                logroRepository.save(verificado);
            }
        } catch (Exception e) {
            System.out.println("Error inicializando logros básicos: " + e.getMessage());
        }
    }
    
    /**
     * Método privado para otorgar un logro a un usuario
     */
    private void otorgarLogro(Usuario usuario, Logro logro) {
        try {
            UsuarioLogro usuarioLogro = UsuarioLogro.builder()
                    .usuario(usuario)
                    .logro(logro)
                    .fechaOtorgado(LocalDate.now())
                    .build();
            
            usuarioLogroRepository.save(usuarioLogro);
            
            System.out.println("Logro '" + logro.getNombre() + "' otorgado al usuario " + usuario.getEmail());
        } catch (Exception e) {
            System.out.println("Error otorgando logro: " + e.getMessage());
        }
    }
} 