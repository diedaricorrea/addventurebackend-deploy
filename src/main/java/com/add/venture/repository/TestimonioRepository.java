package com.add.venture.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.add.venture.model.Testimonio;
import com.add.venture.model.Usuario;

@Repository
public interface TestimonioRepository extends JpaRepository<Testimonio, Long> {
    
    /**
     * Obtiene testimonios destacados y aprobados para mostrar en el index
     * @param pageable paginación y ordenamiento
     * @return lista de testimonios destacados
     */
    @Query("SELECT t FROM Testimonio t WHERE t.aprobado = true AND t.destacado = true ORDER BY t.fecha DESC")
    List<Testimonio> findDestacados(Pageable pageable);
    
    /**
     * Obtiene todos los testimonios aprobados
     * @param pageable paginación
     * @return lista de testimonios aprobados
     */
    List<Testimonio> findByAprobadoTrueOrderByFechaDesc(Pageable pageable);
    
    /**
     * Obtiene testimonios pendientes de aprobación
     * @return lista de testimonios pendientes
     */
    List<Testimonio> findByAprobadoFalseOrderByFechaDesc();
    
    /**
     * Obtiene testimonios de un usuario específico
     * @param autor usuario autor
     * @return lista de testimonios del usuario
     */
    List<Testimonio> findByAutorOrderByFechaDesc(Usuario autor);
    
    /**
     * Cuenta testimonios pendientes de aprobación
     * @return cantidad de testimonios pendientes
     */
    long countByAprobadoFalse();
}
