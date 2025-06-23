package com.add.venture.service;

import java.time.LocalDate;
import java.util.List;

import com.add.venture.dto.CrearGrupoViajeDTO;
import com.add.venture.model.GrupoViaje;
import com.add.venture.model.TipoViaje;

public interface IGrupoViajeService {
    
    /**
     * Crea un nuevo grupo de viaje con su viaje asociado
     * 
     * @param dto datos para la creación del grupo y viaje
     * @return el grupo creado
     * @throws RuntimeException si el viaje ya está asignado a otro grupo
     */
    GrupoViaje crearGrupoViaje(CrearGrupoViajeDTO dto);
    
    /**
     * Verifica si un viaje ya está asignado a un grupo
     * 
     * @param idViaje el ID del viaje a verificar
     * @return true si el viaje ya está asignado, false en caso contrario
     */
    boolean viajeYaAsignado(Long idViaje);
    
    /**
     * Obtiene todos los tipos de viaje disponibles
     * 
     * @return lista de tipos de viaje
     */
    List<TipoViaje> obtenerTiposViaje();
    
    /**
     * Busca grupos de viaje según los filtros especificados
     * 
     * @param destino destino principal del viaje
     * @param fechaInicio fecha de inicio del viaje
     * @param fechaFin fecha de fin del viaje
     * @param idTipoViaje ID del tipo de viaje
     * @param rangoEdad rango de edad de los participantes
     * @param verificado si el grupo está verificado
     * @param etiquetas lista de etiquetas separadas por comas
     * @param ordenar criterio de ordenación
     * @return lista de grupos que cumplen con los filtros
     */
    List<GrupoViaje> buscarGrupos(
            String destino,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Long idTipoViaje,
            String rangoEdad,
            Boolean verificado,
            List<String> etiquetas,
            String ordenar);

    /**
     * Busca grupos de viaje por ID
     * 
     * @param id el ID del grupo a buscar
     * @return el grupo encontrado o null si no existe
     */
    GrupoViaje buscarGrupoPorId(Long id);

    /**
     * Busca grupos de viaje por ID y lanza una excepción si no se encuentra
     * 
     * @param id el ID del grupo a buscar
     * @return el grupo encontrado
     * @throws RuntimeException si el grupo no existe
     */
    GrupoViaje actualizarGrupoViaje(Long idGrupo, CrearGrupoViajeDTO dto);
}