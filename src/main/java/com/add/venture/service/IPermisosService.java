package com.add.venture.service;

import java.util.List;
import java.util.Set;

import com.add.venture.model.GrupoViaje;
import com.add.venture.model.Permiso;
import com.add.venture.model.Rol;
import com.add.venture.model.Usuario;
import com.add.venture.model.UsuarioRolGrupo;

public interface IPermisosService {
    
    // ===== VERIFICACIÓN DE PERMISOS =====
    
    /**
     * Verifica si un usuario tiene un permiso específico en un grupo
     */
    boolean usuarioTienePermiso(Usuario usuario, GrupoViaje grupo, String nombrePermiso);
    
    /**
     * Verifica si un usuario tiene cualquiera de los permisos especificados en un grupo
     */
    boolean usuarioTieneAlgunPermiso(Usuario usuario, GrupoViaje grupo, List<String> nombresPermisos);
    
    /**
     * Verifica si un usuario tiene todos los permisos especificados en un grupo
     */
    boolean usuarioTieneTodosLosPermisos(Usuario usuario, GrupoViaje grupo, List<String> nombresPermisos);
    
    /**
     * Verifica si un usuario es el creador de un grupo
     */
    boolean esCreadorDelGrupo(Usuario usuario, GrupoViaje grupo);
    
    /**
     * Verifica si un usuario puede realizar una acción sobre otro usuario en un grupo
     * (basado en jerarquía de roles)
     */
    boolean puedeGestionarUsuario(Usuario gestor, Usuario objetivo, GrupoViaje grupo);
    
    /**
     * Verifica si un usuario puede asignar roles descriptivos en un grupo
     */
    boolean puedeAsignarRoles(Usuario usuario, GrupoViaje grupo);
    
    // ===== GESTIÓN DE ROLES =====
    
    /**
     * Asigna un rol a un usuario en un grupo específico
     */
    UsuarioRolGrupo asignarRolEnGrupo(Usuario usuario, GrupoViaje grupo, Rol rol, Usuario asignadoPor);
    
    /**
     * Asigna el rol de líder automáticamente al creador del grupo
     */
    UsuarioRolGrupo asignarRolLiderCreador(Usuario creador, GrupoViaje grupo);
    
    /**
     * Cambia el rol de un usuario en un grupo
     */
    UsuarioRolGrupo cambiarRolEnGrupo(Usuario usuario, GrupoViaje grupo, Rol nuevoRol, Usuario cambiadoPor);
    
    /**
     * Remueve el rol de un usuario en un grupo
     */
    void removerRolEnGrupo(Usuario usuario, GrupoViaje grupo, Usuario removidoPor);
    
    // ===== CONSULTAS DE INFORMACIÓN =====
    
    /**
     * Obtiene el rol de un usuario en un grupo específico
     */
    Rol obtenerRolEnGrupo(Usuario usuario, GrupoViaje grupo);
    
    /**
     * Obtiene todos los permisos de un usuario en un grupo
     */
    Set<Permiso> obtenerPermisosEnGrupo(Usuario usuario, GrupoViaje grupo);
    
    /**
     * Obtiene todos los miembros de un grupo con sus roles
     */
    List<UsuarioRolGrupo> obtenerMiembrosConRoles(GrupoViaje grupo);
    
    /**
     * Obtiene todos los grupos donde un usuario tiene un rol específico
     */
    List<GrupoViaje> obtenerGruposConRol(Usuario usuario, String nombreRol);
    
    // ===== GESTIÓN DE ROLES DEL SISTEMA =====
    
    /**
     * Obtiene un rol por su nombre
     */
    Rol obtenerRolPorNombre(String nombreRol);
    
    /**
     * Obtiene todos los roles disponibles para asignar
     */
    List<Rol> obtenerRolesDisponibles();
    
    /**
     * Obtiene roles que un usuario puede asignar (basado en jerarquía)
     */
    List<Rol> obtenerRolesAsignablesPor(Usuario usuario, GrupoViaje grupo);
} 