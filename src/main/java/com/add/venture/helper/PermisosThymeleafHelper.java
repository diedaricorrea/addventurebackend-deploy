package com.add.venture.helper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.add.venture.model.GrupoViaje;
import com.add.venture.model.Rol;
import com.add.venture.model.Usuario;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.service.IPermisosService;

@Component("permisos")
public class PermisosThymeleafHelper {

    @Autowired
    private IPermisosService permisosService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoViajeRepository grupoViajeRepository;

    /**
     * Verifica si el usuario actual tiene un permiso específico en un grupo
     * 
     * @param idGrupo ID del grupo
     * @param nombrePermiso Nombre del permiso a verificar
     * @return true si tiene el permiso, false en caso contrario
     */
    public boolean tienePermiso(Long idGrupo, String nombrePermiso) {
        try {
            Usuario usuario = obtenerUsuarioActual();
            GrupoViaje grupo = obtenerGrupo(idGrupo);
            
            if (usuario == null || grupo == null) {
                return false;
            }
            
            return permisosService.usuarioTienePermiso(usuario, grupo, nombrePermiso);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica si el usuario actual tiene cualquiera de los permisos especificados en un grupo
     * 
     * @param idGrupo ID del grupo
     * @param nombresPermisos Lista de nombres de permisos
     * @return true si tiene al menos uno de los permisos, false en caso contrario
     */
    public boolean tieneAlgunPermiso(Long idGrupo, List<String> nombresPermisos) {
        try {
            Usuario usuario = obtenerUsuarioActual();
            GrupoViaje grupo = obtenerGrupo(idGrupo);
            
            if (usuario == null || grupo == null) {
                return false;
            }
            
            return permisosService.usuarioTieneAlgunPermiso(usuario, grupo, nombresPermisos);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica si el usuario actual es el creador de un grupo
     * 
     * @param idGrupo ID del grupo
     * @return true si es el creador, false en caso contrario
     */
    public boolean esCreador(Long idGrupo) {
        try {
            Usuario usuario = obtenerUsuarioActual();
            GrupoViaje grupo = obtenerGrupo(idGrupo);
            
            if (usuario == null || grupo == null) {
                return false;
            }
            
            return permisosService.esCreadorDelGrupo(usuario, grupo);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica si el usuario actual puede gestionar a otro usuario en un grupo
     * 
     * @param idGrupo ID del grupo
     * @param idUsuarioObjetivo ID del usuario que se quiere gestionar
     * @return true si puede gestionarlo, false en caso contrario
     */
    public boolean puedeGestionar(Long idGrupo, Long idUsuarioObjetivo) {
        try {
            Usuario gestor = obtenerUsuarioActual();
            Usuario objetivo = usuarioRepository.findById(idUsuarioObjetivo).orElse(null);
            GrupoViaje grupo = obtenerGrupo(idGrupo);
            
            if (gestor == null || objetivo == null || grupo == null) {
                return false;
            }
            
            return permisosService.puedeGestionarUsuario(gestor, objetivo, grupo);
        } catch (Exception e) {
            return false;
        }
    }
    

    /**
     * Obtiene el rol del usuario actual en un grupo
     * 
     * @param idGrupo ID del grupo
     * @return Nombre del rol o "Sin rol" si no tiene rol
     */
    public String obtenerRol(Long idGrupo) {
        try {
            Usuario usuario = obtenerUsuarioActual();
            GrupoViaje grupo = obtenerGrupo(idGrupo);
            
            if (usuario == null || grupo == null) {
                return "Sin rol";
            }
            
            Rol rol = permisosService.obtenerRolEnGrupo(usuario, grupo);
            return rol != null ? rol.getDescripcion() : "Sin rol";
        } catch (Exception e) {
            return "Sin rol";
        }
    }

    /**
     * Obtiene el nombre del rol del usuario actual en un grupo
     * 
     * @param idGrupo ID del grupo
     * @return Nombre técnico del rol o null si no tiene rol
     */
    public String obtenerNombreRol(Long idGrupo) {
        try {
            Usuario usuario = obtenerUsuarioActual();
            GrupoViaje grupo = obtenerGrupo(idGrupo);
            
            if (usuario == null || grupo == null) {
                return null;
            }
            
            Rol rol = permisosService.obtenerRolEnGrupo(usuario, grupo);
            return rol != null ? rol.getNombreRol() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verifica si el usuario actual puede acceder al chat de un grupo
     * 
     * @param idGrupo ID del grupo
     * @return true si puede acceder al chat, false en caso contrario
     */
    public boolean puedeAccederChat(Long idGrupo) {
        return tienePermiso(idGrupo, "ACCEDER_CHAT");
    }

    /**
     * Verifica si el usuario actual puede enviar mensajes en un grupo
     * 
     * @param idGrupo ID del grupo
     * @return true si puede enviar mensajes, false en caso contrario
     */
    public boolean puedeEnviarMensajes(Long idGrupo) {
        return tienePermiso(idGrupo, "ENVIAR_MENSAJES");
    }

    /**
     * Verifica si el usuario actual puede editar el grupo
     * 
     * @param idGrupo ID del grupo
     * @return true si puede editar el grupo, false en caso contrario
     */
    public boolean puedeEditarGrupo(Long idGrupo) {
        return tienePermiso(idGrupo, "EDITAR_GRUPO");
    }

    /**
     * Verifica si el usuario actual puede eliminar el grupo
     * 
     * @param idGrupo ID del grupo
     * @return true si puede eliminar el grupo, false en caso contrario
     */
    public boolean puedeEliminarGrupo(Long idGrupo) {
        return tienePermiso(idGrupo, "ELIMINAR_GRUPO");
    }

    /**
     * Verifica si el usuario actual puede expulsar miembros
     * 
     * @param idGrupo ID del grupo
     * @return true si puede expulsar miembros, false en caso contrario
     */
    public boolean puedeExpulsarMiembros(Long idGrupo) {
        return tienePermiso(idGrupo, "EXPULSAR_MIEMBROS");
    }

    /**
     * Verifica si el usuario actual puede asignar roles
     * 
     * @param idGrupo ID del grupo
     * @return true si puede asignar roles, false en caso contrario
     */
    public boolean puedeAsignarRoles(Long idGrupo) {
        return tienePermiso(idGrupo, "ASIGNAR_ROLES");
    }

    // ===== MÉTODOS PRIVADOS =====

    private Usuario obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return null;
        }

        String email = auth.getName();
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    private GrupoViaje obtenerGrupo(Long idGrupo) {
        if (idGrupo == null) {
            return null;
        }
        return grupoViajeRepository.findById(idGrupo).orElse(null);
    }
} 