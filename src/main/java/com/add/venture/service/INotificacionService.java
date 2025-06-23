package com.add.venture.service;

import java.util.List;

import com.add.venture.model.Notificacion;
import com.add.venture.model.Usuario;

public interface INotificacionService {
    
    /**
     * Crea una notificación de solicitud de unión a grupo
     * 
     * @param solicitante usuario que solicita unirse
     * @param lider líder del grupo
     * @param idGrupo ID del grupo
     * @param nombreGrupo nombre del grupo
     * @return la notificación creada
     */
    Notificacion crearNotificacionSolicitudUnion(Usuario solicitante, Usuario lider, Long idGrupo, String nombreGrupo);
    
    /**
     * Obtiene todas las notificaciones de un usuario
     * 
     * @param usuario el usuario
     * @return lista de notificaciones
     */
    List<Notificacion> obtenerNotificacionesUsuario(Usuario usuario);
    
    /**
     * Obtiene notificaciones no leídas de un usuario
     * 
     * @param usuario el usuario
     * @return lista de notificaciones no leídas
     */
    List<Notificacion> obtenerNotificacionesNoLeidas(Usuario usuario);
    
    /**
     * Cuenta las notificaciones no leídas de un usuario
     * 
     * @param usuario el usuario
     * @return número de notificaciones no leídas
     */
    long contarNotificacionesNoLeidas(Usuario usuario);
    
    /**
     * Marca una notificación como leída
     * 
     * @param idNotificacion ID de la notificación
     */
    void marcarComoLeida(Long idNotificacion);
    
    /**
     * Marca todas las notificaciones de un usuario como leídas
     * 
     * @param usuario el usuario
     */
    void marcarTodasComoLeidas(Usuario usuario);
} 