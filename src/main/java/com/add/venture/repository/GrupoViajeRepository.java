package com.add.venture.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.add.venture.model.GrupoViaje;
import com.add.venture.model.Usuario;
import com.add.venture.model.Viaje;

public interface GrupoViajeRepository extends JpaRepository<GrupoViaje, Long> {

    /**
     * Verifica si un viaje ya está asignado a algún grupo
     * 
     * @param idViaje el ID del viaje a verificar
     * @return true si el viaje ya está asignado a un grupo, false en caso contrario
     */
    boolean existsByViajeIdViaje(Long idViaje);

    /**
     * Busca un grupo por su viaje asociado
     * 
     * @param viaje el viaje asociado al grupo
     * @return el grupo asociado al viaje, o null si no existe
     */
    GrupoViaje findByViaje(Viaje viaje);

    /**
     * Cuenta cuántos grupos ha creado un usuario
     * 
     * @param idUsuario el ID del usuario creador
     * @return el número de grupos creados por el usuario
     */
    @Query("SELECT COUNT(g) FROM GrupoViaje g WHERE g.creador.idUsuario = :idUsuario")
    long countByCreadorId(@Param("idUsuario") Long idUsuario);

    
    /**
     * Busca todos los grupos creados por un usuario
     * 
     * @param creador el usuario creador de los grupos
     * @return una lista de grupos creados por el usuario
     */
    List<GrupoViaje> findByCreador(Usuario creador);
    
    /**
     * Busca grupos creados por un usuario ordenados por fecha de creación descendente
     * 
     * @param creador el usuario creador
     * @return lista de grupos ordenados por fecha
     */
    List<GrupoViaje> findByCreadorOrderByFechaCreacionDesc(Usuario creador);
    
    /**
     * Busca todos los grupos por estado
     * 
     * @param estado el estado del grupo (ej: "activo", "cerrado", "finalizado")
     * @return una lista de grupos con el estado especificado
     */
    List<GrupoViaje> findByEstado(String estado);
}