package com.add.venture.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.add.venture.model.GrupoViaje;

public interface IBuscarGrupoService {

    Page<GrupoViaje> obtenerGrupos(Pageable pageable);

    Page<GrupoViaje> buscarGrupos(String destinoPrincipal, LocalDate fechaInicio, LocalDate fechaFin, Pageable pageable);
    
}
