package com.add.venture.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.add.venture.model.GrupoViaje;
import com.add.venture.model.Permiso;
import com.add.venture.model.Rol;
import com.add.venture.model.Usuario;
import com.add.venture.model.UsuarioRolGrupo;
import com.add.venture.repository.RolRepository;
import com.add.venture.repository.UsuarioRolGrupoRepository;

@Service
@Transactional
public class PermisosServiceImpl implements IPermisosService {

    @Autowired
    private UsuarioRolGrupoRepository usuarioRolGrupoRepository;
    
    @Autowired
    private RolRepository rolRepository;

    @Override
    public boolean usuarioTienePermiso(Usuario usuario, GrupoViaje grupo, String nombrePermiso) {
        // Verificar si es el creador del grupo (tiene todos los permisos)
        if (esCreadorDelGrupo(usuario, grupo)) {
            return true;
        }
        
        // Obtener el rol del usuario en el grupo
        Optional<UsuarioRolGrupo> usuarioRolGrupo = usuarioRolGrupoRepository
                .findActiveByUsuarioAndGrupo(usuario, grupo);
        
        if (usuarioRolGrupo.isEmpty()) {
            return false;
        }
        
        // Verificar si el rol tiene el permiso específico
        Rol rol = usuarioRolGrupo.get().getRol();
        return rol.getPermisos().stream()
                .anyMatch(permiso -> permiso.getNombrePermiso().equals(nombrePermiso) && 
                                   "activo".equals(permiso.getEstado()));
    }

    @Override
    public boolean usuarioTieneAlgunPermiso(Usuario usuario, GrupoViaje grupo, List<String> nombresPermisos) {
        return nombresPermisos.stream()
                .anyMatch(permiso -> usuarioTienePermiso(usuario, grupo, permiso));
    }

    @Override
    public boolean usuarioTieneTodosLosPermisos(Usuario usuario, GrupoViaje grupo, List<String> nombresPermisos) {
        return nombresPermisos.stream()
                .allMatch(permiso -> usuarioTienePermiso(usuario, grupo, permiso));
    }

    @Override
    public boolean esCreadorDelGrupo(Usuario usuario, GrupoViaje grupo) {
        return grupo.getCreador() != null && 
               grupo.getCreador().getIdUsuario().equals(usuario.getIdUsuario());
    }

    @Override
    public boolean puedeGestionarUsuario(Usuario gestor, Usuario objetivo, GrupoViaje grupo) {
        // El creador puede gestionar a cualquiera
        if (esCreadorDelGrupo(gestor, grupo)) {
            return true;
        }
        
        // Nadie puede gestionar al creador excepto él mismo
        if (esCreadorDelGrupo(objetivo, grupo)) {
            return false;
        }
        
        // Obtener roles de ambos usuarios
        Optional<UsuarioRolGrupo> rolGestor = usuarioRolGrupoRepository
                .findActiveByUsuarioAndGrupo(gestor, grupo);
        Optional<UsuarioRolGrupo> rolObjetivo = usuarioRolGrupoRepository
                .findActiveByUsuarioAndGrupo(objetivo, grupo);
        
        if (rolGestor.isEmpty()) {
            return false;
        }
        
        // Si el objetivo no tiene rol, puede ser gestionado por cualquiera con rol
        if (rolObjetivo.isEmpty()) {
            return true;
        }
        
        // Comparar jerarquías (nivel menor = más poder)
        Integer nivelGestor = rolGestor.get().getRol().getNivelJerarquia();
        Integer nivelObjetivo = rolObjetivo.get().getRol().getNivelJerarquia();
        
        return nivelGestor != null && nivelObjetivo != null && nivelGestor < nivelObjetivo;
    }

    @Override
    public UsuarioRolGrupo asignarRolEnGrupo(Usuario usuario, GrupoViaje grupo, Rol rol, Usuario asignadoPor) {
        // Verificar si ya tiene un rol activo en el grupo
        Optional<UsuarioRolGrupo> rolExistente = usuarioRolGrupoRepository
                .findActiveByUsuarioAndGrupo(usuario, grupo);
        
        if (rolExistente.isPresent()) {
            // Actualizar el rol existente
            UsuarioRolGrupo rolActual = rolExistente.get();
            rolActual.setRol(rol);
            rolActual.setAsignadoPor(asignadoPor != null ? asignadoPor.getIdUsuario() : null);
            return usuarioRolGrupoRepository.save(rolActual);
        } else {
            // Crear nuevo rol
            UsuarioRolGrupo nuevoRol = UsuarioRolGrupo.builder()
                    .usuario(usuario)
                    .grupo(grupo)
                    .rol(rol)
                    .asignadoPor(asignadoPor != null ? asignadoPor.getIdUsuario() : null)
                    .estado("activo")
                    .build();
            return usuarioRolGrupoRepository.save(nuevoRol);
        }
    }

    @Override
    public UsuarioRolGrupo asignarRolLiderCreador(Usuario creador, GrupoViaje grupo) {
        Rol rolLider = obtenerRolPorNombre(Rol.RolesSistema.LIDER_GRUPO.getNombre());
        return asignarRolEnGrupo(creador, grupo, rolLider, null);
    }

    @Override
    public UsuarioRolGrupo cambiarRolEnGrupo(Usuario usuario, GrupoViaje grupo, Rol nuevoRol, Usuario cambiadoPor) {
        return asignarRolEnGrupo(usuario, grupo, nuevoRol, cambiadoPor);
    }

    @Override
    public void removerRolEnGrupo(Usuario usuario, GrupoViaje grupo, Usuario removidoPor) {
        Optional<UsuarioRolGrupo> rolExistente = usuarioRolGrupoRepository
                .findActiveByUsuarioAndGrupo(usuario, grupo);
        
        if (rolExistente.isPresent()) {
            UsuarioRolGrupo rol = rolExistente.get();
            rol.setEstado("inactivo");
            usuarioRolGrupoRepository.save(rol);
        }
    }

    @Override
    public Rol obtenerRolEnGrupo(Usuario usuario, GrupoViaje grupo) {
        // Si es el creador, siempre retornar rol de líder
        if (esCreadorDelGrupo(usuario, grupo)) {
            return obtenerRolPorNombre(Rol.RolesSistema.LIDER_GRUPO.getNombre());
        }
        
        Optional<UsuarioRolGrupo> usuarioRolGrupo = usuarioRolGrupoRepository
                .findActiveByUsuarioAndGrupo(usuario, grupo);
        
        return usuarioRolGrupo.map(UsuarioRolGrupo::getRol).orElse(null);
    }

    @Override
    public Set<Permiso> obtenerPermisosEnGrupo(Usuario usuario, GrupoViaje grupo) {
        Rol rol = obtenerRolEnGrupo(usuario, grupo);
        return rol != null ? rol.getPermisos() : Set.of();
    }

    @Override
    public List<UsuarioRolGrupo> obtenerMiembrosConRoles(GrupoViaje grupo) {
        return usuarioRolGrupoRepository.findActiveByGrupo(grupo);
    }

    @Override
    public List<GrupoViaje> obtenerGruposConRol(Usuario usuario, String nombreRol) {
        List<UsuarioRolGrupo> rolesUsuario = usuarioRolGrupoRepository.findActiveByUsuario(usuario);
        
        return rolesUsuario.stream()
                .filter(urg -> urg.getRol().getNombreRol().equals(nombreRol))
                .map(UsuarioRolGrupo::getGrupo)
                .collect(Collectors.toList());
    }

    @Override
    public Rol obtenerRolPorNombre(String nombreRol) {
        return rolRepository.findByNombreRol(nombreRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + nombreRol));
    }

    @Override
    public List<Rol> obtenerRolesDisponibles() {
        return rolRepository.findByEstado("activo");
    }

    @Override
    public List<Rol> obtenerRolesAsignablesPor(Usuario usuario, GrupoViaje grupo) {
        // El creador puede asignar cualquier rol
        if (esCreadorDelGrupo(usuario, grupo)) {
            return obtenerRolesDisponibles();
        }
        
        // Otros usuarios solo pueden asignar roles de nivel inferior
        Rol rolUsuario = obtenerRolEnGrupo(usuario, grupo);
        if (rolUsuario == null) {
            return List.of();
        }
        
        return rolRepository.findRolesConNivelMenorOIgual(rolUsuario.getNivelJerarquia() + 1);
    }

    @Override
    public boolean puedeAsignarRoles(Usuario usuario, GrupoViaje grupo) {
        // El creador siempre puede asignar roles descriptivos
        if (esCreadorDelGrupo(usuario, grupo)) {
            return true;
        }
        
        // Verificar si tiene permisos específicos para asignar roles
        return usuarioTienePermiso(usuario, grupo, "ASIGNAR_ROLES") ||
               usuarioTienePermiso(usuario, grupo, "GESTIONAR_MIEMBROS");
    }
} 