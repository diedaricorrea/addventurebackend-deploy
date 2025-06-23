package com.add.venture.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.add.venture.model.Notificacion;
import com.add.venture.model.Usuario;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    
    /**
     * Busca notificaciones de un usuario ordenadas por fecha descendente
     * 
     * @param usuario el usuario cuyas notificaciones se buscan
     * @return lista de notificaciones del usuario
     */
    List<Notificacion> findByUsuarioOrderByFechaDesc(Usuario usuario);
    
    /**
     * Busca notificaciones no leídas de un usuario
     * 
     * @param usuario el usuario cuyas notificaciones no leídas se buscan
     * @return lista de notificaciones no leídas
     */
    List<Notificacion> findByUsuarioAndLeidoFalseOrderByFechaDesc(Usuario usuario);
    
    /**
     * Cuenta las notificaciones no leídas de un usuario
     * 
     * @param usuario el usuario cuyas notificaciones no leídas se cuentan
     * @return número de notificaciones no leídas
     */
    long countByUsuarioAndLeidoFalse(Usuario usuario);
    
    /**
     * Busca notificaciones por tipo y usuario
     * 
     * @param tipo tipo de notificación
     * @param usuario el usuario
     * @return lista de notificaciones del tipo especificado
     */
    List<Notificacion> findByTipoAndUsuario(String tipo, Usuario usuario);
} 