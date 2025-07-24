package com.add.venture.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.add.venture.model.GrupoViaje;
import com.add.venture.repository.BuscarGrupoRepository;

@Service
public class BuscarGrupoServiceImpl implements IBuscarGrupoService {

    @Autowired
    private BuscarGrupoRepository buscarGrupoRepository;

    @Override
    public Page<GrupoViaje> obtenerGrupos(Pageable pageable) {
        return buscarGrupoRepository.findByEstado("activo", pageable);
    }

    @Override
    public Page<GrupoViaje> buscarGrupos(String destinoPrincipal, LocalDate fechaInicio, LocalDate fechaFin,
            Pageable pageable) {
        
        String estado = "activo";

        if (destinoPrincipal != null && !destinoPrincipal.isBlank() && fechaInicio != null && fechaFin != null) {
            return buscarGrupoRepository.findByDestinoPrincipalContainingIgnoreCaseAndFechaInicioGreaterThanEqualAndFechaFinLessThanEqualAndEstado(
                destinoPrincipal, fechaInicio, fechaFin, estado, pageable);
        
        } else if (destinoPrincipal != null && !destinoPrincipal.isBlank()) {
            return buscarGrupoRepository.findByDestinoPrincipalContainingIgnoreCaseAndEstado(destinoPrincipal, estado, pageable);
        
        } else if (fechaInicio != null && fechaFin != null) {
            return buscarGrupoRepository.findByFechaInicioGreaterThanEqualAndFechaFinLessThanEqualAndEstado(fechaInicio, fechaFin, estado, pageable);
        
        } else if (fechaInicio != null) {
            return buscarGrupoRepository.findByFechaInicioGreaterThanEqualAndEstado(fechaInicio, estado, pageable);
        
        } else if (fechaFin != null) {
            return buscarGrupoRepository.findByFechaFinLessThanEqualAndEstado(fechaFin, estado, pageable);
        
        } else {
            return buscarGrupoRepository.findByEstado(estado, pageable);
        }
        
    }
    
}
