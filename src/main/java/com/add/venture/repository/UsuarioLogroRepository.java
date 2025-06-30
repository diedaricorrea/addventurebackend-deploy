package com.add.venture.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.add.venture.model.Logro;
import com.add.venture.model.Usuario;
import com.add.venture.model.UsuarioLogro;
import com.add.venture.model.UsuarioLogroId;

@Repository
public interface UsuarioLogroRepository extends JpaRepository<UsuarioLogro, UsuarioLogroId> {
    
    /**
     * Busca todos los logros de un usuario específico
     * 
     * @param usuario el usuario
     * @return lista de logros del usuario ordenados por fecha
     */
    @Query("SELECT ul FROM UsuarioLogro ul JOIN FETCH ul.logro WHERE ul.usuario = :usuario ORDER BY ul.fechaOtorgado DESC")
    List<UsuarioLogro> findByUsuarioOrderByFechaOtorgadoDesc(@Param("usuario") Usuario usuario);
    
    /**
     * Verifica si un usuario ya tiene un logro específico
     * 
     * @param usuario el usuario
     * @param logro el logro
     * @return true si el usuario ya tiene el logro
     */
    boolean existsByUsuarioAndLogro(Usuario usuario, Logro logro);
    
    /**
     * Cuenta el número total de logros de un usuario
     * 
     * @param usuario el usuario
     * @return número de logros
     */
    long countByUsuario(Usuario usuario);
    
    /**
     * Busca los logros más recientes de un usuario limitados por cantidad
     * 
     * @param usuario el usuario
     * @param limite número máximo de logros
     * @return lista limitada de logros más recientes
     */
    @Query("SELECT ul FROM UsuarioLogro ul JOIN FETCH ul.logro WHERE ul.usuario = :usuario ORDER BY ul.fechaOtorgado DESC LIMIT :limite")
    List<UsuarioLogro> findTopLogrosByUsuario(@Param("usuario") Usuario usuario, @Param("limite") int limite);
} 