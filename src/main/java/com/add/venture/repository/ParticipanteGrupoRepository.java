package com.add.venture.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.add.venture.model.GrupoViaje;
import com.add.venture.model.ParticipanteGrupo;
import com.add.venture.model.ParticipanteGrupo.EstadoSolicitud;
import com.add.venture.model.ParticipanteGrupoId;
import com.add.venture.model.Usuario;

public interface ParticipanteGrupoRepository extends JpaRepository<ParticipanteGrupo, ParticipanteGrupoId> {
    
    /**
     * Verifica si un usuario es participante de un grupo
     * 
     * @param usuario el usuario a verificar
     * @param grupo el grupo a verificar
     * @return true si el usuario es participante del grupo, false en caso contrario
     */
    boolean existsByUsuarioAndGrupo(Usuario usuario, GrupoViaje grupo);
    
    /**
     * Busca un participante por usuario y grupo
     * 
     * @param usuario el usuario a buscar
     * @param grupo el grupo a buscar
     * @return el participante encontrado, o vacío si no existe
     */
    Optional<ParticipanteGrupo> findByUsuarioAndGrupo(Usuario usuario, GrupoViaje grupo);
    
    /**
     * Busca participantes por estado de solicitud en un grupo
     * 
     * @param grupo el grupo
     * @param estado el estado de la solicitud
     * @return lista de participantes con el estado especificado
     */
    List<ParticipanteGrupo> findByGrupoAndEstadoSolicitud(GrupoViaje grupo, EstadoSolicitud estado);
    
    /**
     * Busca participantes aceptados de un grupo
     * 
     * @param grupo el grupo
     * @return lista de participantes aceptados
     */
    List<ParticipanteGrupo> findByGrupoAndEstadoSolicitudOrderByFechaUnionAsc(GrupoViaje grupo, EstadoSolicitud estado);
    
    /**
     * Cuenta participantes aceptados de un grupo
     * 
     * @param grupo el grupo
     * @param estado el estado de la solicitud
     * @return número de participantes con el estado especificado
     */
    long countByGrupoAndEstadoSolicitud(GrupoViaje grupo, EstadoSolicitud estado);

    /**
     * Busca participaciones de un usuario por estado de solicitud
     * 
     * @param usuario el usuario
     * @param estado el estado de la solicitud
     * @return lista de participaciones del usuario con el estado especificado
     */
    List<ParticipanteGrupo> findByUsuarioAndEstadoSolicitud(Usuario usuario, EstadoSolicitud estado);
    
    /**
     * Busca participaciones de un usuario con estado específico ordenadas por fecha de unión descendente
     * 
     * @param usuario el usuario participante
     * @param estadoSolicitud el estado de la solicitud
     * @return lista de participaciones ordenadas por fecha
     */
    List<ParticipanteGrupo> findByUsuarioAndEstadoSolicitudOrderByFechaUnionDesc(Usuario usuario, EstadoSolicitud estadoSolicitud);
}