package com.add.venture.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.add.venture.model.GrupoViaje;
import com.add.venture.model.Resena;
import com.add.venture.model.Usuario;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {
    
    /**
     * Busca todas las reseñas de un grupo específico
     * 
     * @param grupo el grupo de viaje
     * @return lista de reseñas del grupo
     */
    List<Resena> findByGrupoOrderByFechaDesc(GrupoViaje grupo);
    
    /**
     * Busca todas las reseñas escritas por un autor específico
     * 
     * @param autor el usuario autor de las reseñas
     * @return lista de reseñas del autor
     */
    List<Resena> findByAutorOrderByFechaDesc(Usuario autor);
    
    /**
     * Busca todas las reseñas recibidas por un destinatario específico
     * 
     * @param destinatario el usuario destinatario de las reseñas
     * @return lista de reseñas del destinatario
     */
    List<Resena> findByDestinatarioOrderByFechaDesc(Usuario destinatario);
    
    /**
     * Busca una reseña específica entre dos usuarios en un grupo específico
     * 
     * @param autor el usuario que escribió la reseña
     * @param destinatario el usuario que recibió la reseña
     * @param grupo el grupo de viaje
     * @return la reseña si existe
     */
    Optional<Resena> findByAutorAndDestinatarioAndGrupo(Usuario autor, Usuario destinatario, GrupoViaje grupo);
    
    /**
     * Verifica si ya existe una reseña entre dos usuarios en un grupo específico
     * 
     * @param autor el usuario que escribió la reseña
     * @param destinatario el usuario que recibió la reseña
     * @param grupo el grupo de viaje
     * @return true si existe la reseña
     */
    boolean existsByAutorAndDestinatarioAndGrupo(Usuario autor, Usuario destinatario, GrupoViaje grupo);
    
    /**
     * Calcula el promedio de calificaciones de un usuario específico
     * 
     * @param destinatario el usuario del cual calcular el promedio
     * @return el promedio de calificaciones
     */
    @Query("SELECT AVG(r.calificacion) FROM Resena r WHERE r.destinatario = :destinatario")
    Double calcularPromedioCalificaciones(@Param("destinatario") Usuario destinatario);
    
    /**
     * Cuenta el número de reseñas recibidas por un usuario
     * 
     * @param destinatario el usuario del cual contar las reseñas
     * @return número de reseñas
     */
    long countByDestinatario(Usuario destinatario);
    
    /**
     * Busca todas las reseñas de un grupo específico con información del autor y destinatario
     * 
     * @param grupo el grupo de viaje
     * @return lista de reseñas con información completa
     */
    @Query("SELECT r FROM Resena r JOIN FETCH r.autor JOIN FETCH r.destinatario WHERE r.grupo = :grupo ORDER BY r.fecha DESC")
    List<Resena> findByGrupoWithUsuarios(@Param("grupo") GrupoViaje grupo);
    
    /**
     * Busca las reseñas más recientes de un usuario limitadas por cantidad
     * 
     * @param destinatario el usuario destinatario
     * @param limite número máximo de reseñas
     * @return lista limitada de reseñas más recientes
     */
    @Query("SELECT r FROM Resena r JOIN FETCH r.autor WHERE r.destinatario = :destinatario ORDER BY r.fecha DESC LIMIT :limite")
    List<Resena> findTopResenasDelUsuario(@Param("destinatario") Usuario destinatario, @Param("limite") int limite);
    
    /**
     * Cuenta reseñas positivas de un usuario (calificación >= 4)
     * 
     * @param destinatario el usuario destinatario
     * @return número de reseñas positivas
     */
    @Query("SELECT COUNT(r) FROM Resena r WHERE r.destinatario = :destinatario AND r.calificacion >= 4")
    long countResenasPositivas(@Param("destinatario") Usuario destinatario);
    
    /**
     * Verifica si un usuario tiene al menos el número mínimo de reseñas positivas
     * 
     * @param destinatario el usuario destinatario
     * @param minimoResenasPositivas número mínimo requerido
     * @return true si tiene suficientes reseñas positivas
     */
    @Query("SELECT COUNT(r) >= :minimoResenasPositivas FROM Resena r WHERE r.destinatario = :destinatario AND r.calificacion >= 4")
    boolean tieneMinimoResenasPositivas(@Param("destinatario") Usuario destinatario, @Param("minimoResenasPositivas") long minimoResenasPositivas);
    
    /**
     * Cuenta grupos distintos donde un usuario ha sido calificado
     * 
     * @param destinatario el usuario destinatario
     * @return número de grupos distintos donde ha recibido reseñas
     */
    @Query("SELECT COUNT(DISTINCT r.grupo) FROM Resena r WHERE r.destinatario = :destinatario")
    long countDistinctGruposByDestinatario(@Param("destinatario") Usuario destinatario);
} 