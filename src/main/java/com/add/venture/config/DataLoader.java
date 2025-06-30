package com.add.venture.config;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.add.venture.model.Permiso;
import com.add.venture.model.Rol;
import com.add.venture.model.Usuario;
import com.add.venture.model.GrupoViaje;
import com.add.venture.model.UsuarioRolGrupo;
import com.add.venture.repository.PermisoRepository;
import com.add.venture.repository.RolRepository;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.UsuarioRolGrupoRepository;
import com.add.venture.service.ILogroService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private GrupoViajeRepository grupoViajeRepository;
    
    @Autowired
    private UsuarioRolGrupoRepository usuarioRolGrupoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ILogroService logroService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Iniciando carga de datos del sistema...");
        
        cargarPermisos();
        cargarRoles();
        cargarUsuariosDePrueba();
        verificarYCorregirRolesFaltantes();
        inicializarLogros();
        
        logger.info("Carga de datos completada.");
    }

    private void cargarPermisos() {
        logger.info("Cargando permisos del sistema...");
        
        for (Permiso.PermisosSistema permisoEnum : Permiso.PermisosSistema.values()) {
            if (permisoRepository.findByNombrePermiso(permisoEnum.getNombre()).isEmpty()) {
                Permiso permiso = Permiso.builder()
                        .nombrePermiso(permisoEnum.getNombre())
                        .descripcion(permisoEnum.getDescripcion())
                        .categoria(permisoEnum.getCategoria())
                        .estado("activo")
                        .build();
                
                permisoRepository.save(permiso);
                logger.debug("Permiso creado: {}", permisoEnum.getNombre());
            }
        }
        
        logger.info("Permisos cargados correctamente.");
    }

    private void cargarRoles() {
        logger.info("Cargando roles del sistema...");
        
        // Obtener todos los permisos para asignar a los roles
        List<Permiso> todosLosPermisos = permisoRepository.findByEstado("activo");
        
        // ===== ROL: ADMIN_SISTEMA =====
        crearRolSiNoExiste(
            Rol.RolesSistema.ADMIN_SISTEMA,
            new HashSet<>(todosLosPermisos) // Admin tiene todos los permisos
        );
        
        // ===== ROL: LIDER_GRUPO =====
        Set<String> permisosLider = Set.of(
            Permiso.PermisosSistema.EDITAR_GRUPO.getNombre(),
            Permiso.PermisosSistema.ELIMINAR_GRUPO.getNombre(),
            Permiso.PermisosSistema.CERRAR_GRUPO.getNombre(),
            Permiso.PermisosSistema.INVITAR_MIEMBROS.getNombre(),
            Permiso.PermisosSistema.EXPULSAR_MIEMBROS.getNombre(),
            Permiso.PermisosSistema.APROBAR_SOLICITUDES.getNombre(),
            Permiso.PermisosSistema.ASIGNAR_ROLES.getNombre(),
            Permiso.PermisosSistema.VER_LISTA_MIEMBROS.getNombre(),
            Permiso.PermisosSistema.ACCEDER_CHAT.getNombre(),
            Permiso.PermisosSistema.ENVIAR_MENSAJES.getNombre(),
            Permiso.PermisosSistema.ELIMINAR_MENSAJES.getNombre(),
            Permiso.PermisosSistema.COMPARTIR_ARCHIVOS.getNombre(),
            Permiso.PermisosSistema.EDITAR_ITINERARIO.getNombre(),
            Permiso.PermisosSistema.VER_ITINERARIO.getNombre(),
            Permiso.PermisosSistema.VER_ESTADISTICAS.getNombre()
        );
        crearRolSiNoExiste(
            Rol.RolesSistema.LIDER_GRUPO,
            obtenerPermisosPorNombres(todosLosPermisos, permisosLider)
        );
        
        // ===== ROL: CO_LIDER =====
        Set<String> permisosCoLider = Set.of(
            Permiso.PermisosSistema.INVITAR_MIEMBROS.getNombre(),
            Permiso.PermisosSistema.APROBAR_SOLICITUDES.getNombre(),
            Permiso.PermisosSistema.VER_LISTA_MIEMBROS.getNombre(),
            Permiso.PermisosSistema.ACCEDER_CHAT.getNombre(),
            Permiso.PermisosSistema.ENVIAR_MENSAJES.getNombre(),
            Permiso.PermisosSistema.ELIMINAR_MENSAJES.getNombre(),
            Permiso.PermisosSistema.COMPARTIR_ARCHIVOS.getNombre(),
            Permiso.PermisosSistema.EDITAR_ITINERARIO.getNombre(),
            Permiso.PermisosSistema.VER_ITINERARIO.getNombre()
        );
        crearRolSiNoExiste(
            Rol.RolesSistema.CO_LIDER,
            obtenerPermisosPorNombres(todosLosPermisos, permisosCoLider)
        );
        
        // ===== ROL: MIEMBRO =====
        Set<String> permisosMiembro = Set.of(
            Permiso.PermisosSistema.VER_LISTA_MIEMBROS.getNombre(),
            Permiso.PermisosSistema.ACCEDER_CHAT.getNombre(),
            Permiso.PermisosSistema.ENVIAR_MENSAJES.getNombre(),
            Permiso.PermisosSistema.COMPARTIR_ARCHIVOS.getNombre(),
            Permiso.PermisosSistema.VER_ITINERARIO.getNombre()
        );
        crearRolSiNoExiste(
            Rol.RolesSistema.MIEMBRO,
            obtenerPermisosPorNombres(todosLosPermisos, permisosMiembro)
        );
        
        // ===== ROL: MIEMBRO_PREMIUM =====
        Set<String> permisosMiembroPremium = Set.of(
            Permiso.PermisosSistema.INVITAR_MIEMBROS.getNombre(),
            Permiso.PermisosSistema.VER_LISTA_MIEMBROS.getNombre(),
            Permiso.PermisosSistema.ACCEDER_CHAT.getNombre(),
            Permiso.PermisosSistema.ENVIAR_MENSAJES.getNombre(),
            Permiso.PermisosSistema.COMPARTIR_ARCHIVOS.getNombre(),
            Permiso.PermisosSistema.EDITAR_ITINERARIO.getNombre(),
            Permiso.PermisosSistema.VER_ITINERARIO.getNombre()
        );
        crearRolSiNoExiste(
            Rol.RolesSistema.MIEMBRO_PREMIUM,
            obtenerPermisosPorNombres(todosLosPermisos, permisosMiembroPremium)
        );
        
        logger.info("Roles cargados correctamente.");
    }

    private void crearRolSiNoExiste(Rol.RolesSistema rolEnum, Set<Permiso> permisos) {
        if (rolRepository.findByNombreRol(rolEnum.getNombre()).isEmpty()) {
            Rol rol = Rol.builder()
                    .nombreRol(rolEnum.getNombre())
                    .descripcion(rolEnum.getDescripcion())
                    .nivelJerarquia(rolEnum.getNivel())
                    .esRolSistema(true)
                    .estado("activo")
                    .permisos(permisos)
                    .build();
            
            rolRepository.save(rol);
            logger.debug("Rol creado: {} con {} permisos", rolEnum.getNombre(), permisos.size());
        }
    }

    private Set<Permiso> obtenerPermisosPorNombres(List<Permiso> todosLosPermisos, Set<String> nombresPermisos) {
        Set<Permiso> permisos = new HashSet<>();
        for (Permiso permiso : todosLosPermisos) {
            if (nombresPermisos.contains(permiso.getNombrePermiso())) {
                permisos.add(permiso);
            }
        }
        return permisos;
    }

    // ==========================================
    // USUARIOS DE PRUEBA - COMENTAR/ELIMINAR EN PRODUCCIÓN
    // ==========================================
    private void cargarUsuariosDePrueba() {
        logger.info("Cargando usuarios de prueba...");
        
        // USUARIO 1: Administrador de prueba
        if (usuarioRepository.findByEmail("diedari20diez@gmail.com").isEmpty()) {
            Usuario admin = Usuario.builder()
                    .nombre("Carlos")
                    .apellidos("Administrador")
                    .nombreUsuario("carlos_admin")
                    .email("diedari20diez@gmail.com")
                    .telefono("+57 300 123 4567")
                    .pais("Colombia")
                    .ciudad("Bogotá")
                    .fechaNacimiento(LocalDate.of(1990, 5, 15))
                    .contrasenaHash(passwordEncoder.encode("danielunp210"))
                    .esVerificado(true)
                    .estadoCuenta("activa")
                    .estado("activo")
                    .descripcion("Usuario administrador para pruebas del sistema")
                    .build();
            
            usuarioRepository.save(admin);
            logger.debug("Usuario de prueba creado: admin@addventure.com / admin123");
        }
        
        // USUARIO 2: Usuario regular de prueba
        if (usuarioRepository.findByEmail("maria@addventure.com").isEmpty()) {
            Usuario maria = Usuario.builder()
                    .nombre("María")
                    .apellidos("Viajera")
                    .nombreUsuario("maria_viajera")
                    .email("maria@addventure.com")
                    .telefono("+57 310 987 6543")
                    .pais("Colombia")
                    .ciudad("Medellín")
                    .fechaNacimiento(LocalDate.of(1995, 8, 22))
                    .contrasenaHash(passwordEncoder.encode("maria123"))
                    .esVerificado(true)
                    .estadoCuenta("activa")
                    .estado("activo")
                    .descripcion("Apasionada por los viajes de aventura y la fotografía")
                    .build();
            
            usuarioRepository.save(maria);
            logger.debug("Usuario de prueba creado: maria@addventure.com / maria123");
        }
        
        logger.info("Usuarios de prueba cargados correctamente.");
        logger.warn("RECORDATORIO: Eliminar usuarios de prueba antes de producción");
    }
    // ==========================================
    // FIN USUARIOS DE PRUEBA
    // ==========================================
    
    private void verificarYCorregirRolesFaltantes() {
        logger.info("🔍 Verificando roles faltantes en grupos existentes...");
        
        try {
            List<GrupoViaje> todosLosGrupos = grupoViajeRepository.findAll();
            Optional<Rol> rolLiderOpt = rolRepository.findByNombreRol("LÍDER_GRUPO");
            
            if (rolLiderOpt.isEmpty()) {
                logger.warn("⚠️ Rol LÍDER_GRUPO no encontrado, saltando verificación");
                return;
            }
            
            Rol rolLider = rolLiderOpt.get();
            int rolesAsignados = 0;
            
            for (GrupoViaje grupo : todosLosGrupos) {
                Usuario creador = grupo.getCreador();
                
                // Verificar si el creador ya tiene rol asignado
                Optional<UsuarioRolGrupo> rolExistente = usuarioRolGrupoRepository
                        .findActiveByUsuarioAndGrupo(creador, grupo);
                
                if (rolExistente.isEmpty()) {
                    // Asignar rol de LÍDER_GRUPO al creador
                    UsuarioRolGrupo nuevoRol = UsuarioRolGrupo.builder()
                            .usuario(creador)
                            .grupo(grupo)
                            .rol(rolLider)
                            .asignadoPor(null) // Auto-asignado por el sistema
                            .estado("activo")
                            .build();
                    
                    usuarioRolGrupoRepository.save(nuevoRol);
                    rolesAsignados++;
                    
                    logger.debug("✅ Rol LÍDER_GRUPO asignado a {} en grupo: {}", 
                               creador.getEmail(), grupo.getNombreViaje());
                }
            }
            
            if (rolesAsignados > 0) {
                logger.info("🎯 Total roles de líder asignados: {}", rolesAsignados);
            } else {
                logger.info("✅ Todos los creadores ya tienen sus roles asignados");
            }
            
        } catch (Exception e) {
            logger.error("⚠️ Error al verificar roles faltantes: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void inicializarLogros() {
        logger.info("🏆 Inicializando logros del sistema...");
        
        try {
            logroService.inicializarLogrosBasicos();
            logger.info("✅ Logros del sistema inicializados correctamente");
        } catch (Exception e) {
            logger.error("⚠️ Error al inicializar logros: {}", e.getMessage());
            e.printStackTrace();
        }
    }
} 