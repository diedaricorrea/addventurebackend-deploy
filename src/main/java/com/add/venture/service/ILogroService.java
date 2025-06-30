package com.add.venture.service;

import java.util.List;

import com.add.venture.model.Usuario;
import com.add.venture.model.UsuarioLogro;

public interface ILogroService {
    
    /**
     * Verifica y otorga el logro "Pioneer" cuando un usuario completa su primer viaje
     * 
     * @param usuario el usuario que completó el viaje
     */
    void verificarLogroPioneer(Usuario usuario);
    
    /**
     * Verifica y otorga el logro "Pathfinder" cuando un usuario crea su primer grupo
     * 
     * @param usuario el usuario que creó el grupo
     */
    void verificarLogroPathfinder(Usuario usuario);
    
    /**
     * Verifica y otorga el logro "Verificado" cuando un usuario es verificado
     * 
     * @param usuario el usuario verificado
     */
    void verificarLogroVerificado(Usuario usuario);
    
    /**
     * Obtiene todos los logros de un usuario
     * 
     * @param usuario el usuario
     * @return lista de logros del usuario
     */
    List<UsuarioLogro> obtenerLogrosDeUsuario(Usuario usuario);
    
    /**
     * Obtiene los logros más recientes de un usuario
     * 
     * @param usuario el usuario
     * @param limite número máximo de logros
     * @return lista limitada de logros
     */
    List<UsuarioLogro> obtenerLogrosRecientes(Usuario usuario, int limite);
    
    /**
     * Cuenta el total de logros de un usuario
     * 
     * @param usuario el usuario
     * @return número total de logros
     */
    long contarLogrosDeUsuario(Usuario usuario);
    
    /**
     * Inicializa los logros básicos en la base de datos si no existen
     */
    void inicializarLogrosBasicos();
} 