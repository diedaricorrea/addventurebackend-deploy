package com.add.venture.service;

import java.util.List;

import com.add.venture.dto.TestimonioDTO;
import com.add.venture.model.Usuario;

public interface ITestimonioService {
    
    /**
     * Crea un nuevo testimonio
     * @param testimonioDTO datos del testimonio
     * @param autor usuario que crea el testimonio
     * @return testimonio creado
     */
    TestimonioDTO crearTestimonio(TestimonioDTO testimonioDTO, Usuario autor);
    
    /**
     * Obtiene testimonios destacados para mostrar en el index
     * @param limit cantidad máxima de testimonios
     * @return lista de testimonios destacados
     */
    List<TestimonioDTO> obtenerTestimoniosDestacados(int limit);
    
    /**
     * Obtiene todos los testimonios aprobados
     * @param limit cantidad máxima
     * @return lista de testimonios aprobados
     */
    List<TestimonioDTO> obtenerTestimoniosAprobados(int limit);
    
    /**
     * Obtiene testimonios pendientes de aprobación (solo admin)
     * @return lista de testimonios pendientes
     */
    List<TestimonioDTO> obtenerTestimoniosPendientes();
    
    /**
     * Aprueba un testimonio (solo admin)
     * @param idTestimonio id del testimonio
     * @return testimonio aprobado
     */
    TestimonioDTO aprobarTestimonio(Long idTestimonio);
    
    /**
     * Marca un testimonio como destacado (solo admin)
     * @param idTestimonio id del testimonio
     * @param destacado true para destacar, false para quitar
     * @return testimonio actualizado
     */
    TestimonioDTO marcarDestacado(Long idTestimonio, boolean destacado);
    
    /**
     * Elimina un testimonio
     * @param idTestimonio id del testimonio
     */
    void eliminarTestimonio(Long idTestimonio);
}
