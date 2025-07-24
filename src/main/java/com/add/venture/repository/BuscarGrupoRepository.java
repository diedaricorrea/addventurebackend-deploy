package com.add.venture.repository;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.add.venture.model.GrupoViaje;

@Repository
public interface BuscarGrupoRepository extends JpaRepository<GrupoViaje, Long> {

    //Buscar según estado del grupo
    @Query("SELECT g FROM GrupoViaje g WHERE g.estado = :estado")
    Page<GrupoViaje> findByEstado(@Param("estado") String estado, Pageable pageable);

    //Buscar según destino principal y estado
    @Query("SELECT g FROM GrupoViaje g JOIN g.viaje v WHERE LOWER(v.destinoPrincipal) LIKE LOWER(CONCAT('%', :destinoPrincipal, '%')) AND g.estado = :estado")
    Page<GrupoViaje> findByDestinoPrincipalContainingIgnoreCaseAndEstado(@Param("destinoPrincipal") String destinoPrincipal, @Param("estado") String estado, Pageable pageable);

    //Buscar según fecha de inicio del viaje y estado
    @Query("SELECT g FROM GrupoViaje g JOIN g.viaje v WHERE v.fechaInicio >= :fechaInicio AND g.estado = :estado")
    Page<GrupoViaje> findByFechaInicioGreaterThanEqualAndEstado(@Param("fechaInicio") LocalDate fechaInicio, @Param("estado") String estado, Pageable pageable);

    //Buscar según fecha de fin del viaje y estado
    @Query("SELECT g FROM GrupoViaje g JOIN g.viaje v WHERE v.fechaFin <= :fechaFin AND g.estado = :estado")
    Page<GrupoViaje> findByFechaFinLessThanEqualAndEstado(@Param("fechaFin") LocalDate fechaFin, @Param("estado") String estado, Pageable pageable);

    //Buscar según fecha de inicio, fecha de fin y estado
    @Query("SELECT g FROM GrupoViaje g JOIN g.viaje v WHERE v.fechaInicio >= :fechaInicio AND v.fechaFin <= :fechaFin AND g.estado = :estado")
    Page<GrupoViaje> findByFechaInicioGreaterThanEqualAndFechaFinLessThanEqualAndEstado(@Param("fechaInicio") LocalDate fechaInicio, @Param("fechaFin") LocalDate fechaFin, @Param("estado") String estado, Pageable pageable);

    //Buscar según fecha de inicio, fecha de fin, destino principal y estado
    @Query("SELECT g FROM GrupoViaje g JOIN g.viaje v WHERE LOWER(v.destinoPrincipal) LIKE LOWER(CONCAT('%', :destinoPrincipal, '%')) AND v.fechaInicio >= :fechaInicio AND v.fechaFin <= :fechaFin AND g.estado = :estadoGrupo")
    Page<GrupoViaje> findByDestinoPrincipalContainingIgnoreCaseAndFechaInicioGreaterThanEqualAndFechaFinLessThanEqualAndEstado(
        @Param("destinoPrincipal") String destinoPrincipal, @Param("fechaInicio") LocalDate fechaInicio, @Param("fechaFin") LocalDate fechaFin, @Param("estado") String estado, Pageable pageable);
    
}
