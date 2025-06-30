package com.add.venture.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.add.venture.model.Logro;

@Repository
public interface LogroRepository extends JpaRepository<Logro, Long> {
    
    /**
     * Busca un logro por su nombre
     * 
     * @param nombre nombre del logro
     * @return logro si existe
     */
    Optional<Logro> findByNombre(String nombre);
    
    /**
     * Busca todos los logros ordenados por puntos requeridos
     * 
     * @return lista de logros ordenados
     */
    List<Logro> findAllByOrderByPuntosRequeridosAsc();
} 