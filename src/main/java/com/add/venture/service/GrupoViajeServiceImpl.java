package com.add.venture.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.add.venture.dto.CrearGrupoViajeDTO;
import com.add.venture.dto.DiaItinerarioDTO;
import com.add.venture.model.Etiqueta;
import com.add.venture.model.GrupoViaje;
import com.add.venture.model.Itinerario;
import com.add.venture.model.ParticipanteGrupo;
import com.add.venture.model.TipoViaje;
import com.add.venture.model.Usuario;
import com.add.venture.model.Viaje;
import com.add.venture.model.ParticipanteGrupo.EstadoSolicitud;
import com.add.venture.repository.EtiquetaRepository;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.TipoViajeRepository;
import com.add.venture.repository.UsuarioRepository;
import com.add.venture.repository.ViajeRepository;

@Service
public class GrupoViajeServiceImpl implements IGrupoViajeService {

    @Autowired
    private GrupoViajeRepository grupoViajeRepository;

    @Autowired
    private ViajeRepository viajeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TipoViajeRepository tipoViajeRepository;

    @Autowired
    private EtiquetaRepository etiquetaRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public GrupoViaje crearGrupoViaje(CrearGrupoViajeDTO dto) {
        // Obtener el usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Usuario creador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Crear el viaje
        Viaje viaje = new Viaje();
        viaje.setDestinoPrincipal(dto.getDestinoPrincipal());
        viaje.setFechaInicio(dto.getFechaInicio());
        viaje.setFechaFin(dto.getFechaFin());
        viaje.setDescripcion(dto.getDescripcion());
        viaje.setPuntoEncuentro(dto.getPuntoEncuentro());
        viaje.setImagenDestacada(dto.getImagenDestacada());
        viaje.setRangoEdadMin(dto.getRangoEdadMin());
        viaje.setRangoEdadMax(dto.getRangoEdadMax());
        viaje.setFechaCreacion(LocalDateTime.now());
        viaje.setEsVerificado(false);
        viaje.setEstado("activo");

        // Asignar tipo de viaje si se especificó
        if (dto.getIdTipoViaje() != null) {
            TipoViaje tipoViaje = tipoViajeRepository.findById(dto.getIdTipoViaje())
                    .orElseThrow(() -> new RuntimeException("Tipo de viaje no encontrado"));
            viaje.setTipo(tipoViaje);
        }

        // Guardar el viaje
        viaje = viajeRepository.save(viaje);

        // Crear el grupo de viaje
        GrupoViaje grupo = new GrupoViaje();
        grupo.setNombreViaje(dto.getNombreViaje());
        grupo.setFechaCreacion(LocalDateTime.now());
        grupo.setEstado("activo");
        grupo.setMaxParticipantes(dto.getMaxParticipantes());
        grupo.setCreador(creador);
        grupo.setViaje(viaje);

        // Guardar el grupo sin establecer la relación bidireccional
        grupo = grupoViajeRepository.save(grupo);

        // Establecer la relación bidireccional manualmente después de guardar
        // pero no volver a guardar el viaje para evitar la recursión
        viaje.setGrupo(grupo);

        // Procesar etiquetas si se especificaron
        if (dto.getEtiquetas() != null && !dto.getEtiquetas().isEmpty()) {
            Set<Etiqueta> etiquetas = new HashSet<>();
            for (String nombreEtiqueta : dto.getEtiquetas()) {
                Etiqueta etiqueta = etiquetaRepository.findByNombreEtiqueta(nombreEtiqueta)
                        .orElseGet(() -> {
                            Etiqueta nuevaEtiqueta = new Etiqueta();
                            nuevaEtiqueta.setNombreEtiqueta(nombreEtiqueta);
                            return etiquetaRepository.save(nuevaEtiqueta);
                        });
                etiquetas.add(etiqueta);
            }
            grupo.setEtiquetas(etiquetas);
        }

        // Añadir al creador como participante
        ParticipanteGrupo participante = new ParticipanteGrupo();
        participante.setUsuario(creador);
        participante.setGrupo(grupo);
        participante.setRolParticipante("CREADOR");
        participante.setEstadoSolicitud(EstadoSolicitud.ACEPTADO);
        participante.setFechaUnion(LocalDateTime.now());

        // Inicializar conjuntos si son nulos
        if (grupo.getParticipantes() == null) {
            grupo.setParticipantes(new HashSet<>());
        }
        grupo.getParticipantes().add(participante);
        // Procesar itinerario desde JSON si existe
        if (dto.getDiasItinerarioJson() != null && !dto.getDiasItinerarioJson().isEmpty()) {
            try {
                List<DiaItinerarioDTO> diasItinerario = objectMapper.readValue(
                        dto.getDiasItinerarioJson(),
                        new TypeReference<List<DiaItinerarioDTO>>() {
                        });
                dto.setDiasItinerario(diasItinerario);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error al procesar el itinerario: " + e.getMessage());
            }
        }

        // Procesar itinerario si se especificó
        if (dto.getDiasItinerario() != null && !dto.getDiasItinerario().isEmpty()) {
            Set<Itinerario> itinerarios = new HashSet<>();
            for (DiaItinerarioDTO diaDTO : dto.getDiasItinerario()) {
                Itinerario itinerario = new Itinerario();
                itinerario.setDiaNumero(diaDTO.getDiaNumero());
                itinerario.setTitulo(diaDTO.getTitulo());
                itinerario.setDescripcion(diaDTO.getDescripcion());
                itinerario.setPuntoPartida(diaDTO.getPuntoPartida());
                itinerario.setPuntoLlegada(diaDTO.getPuntoLlegada());
                itinerario.setDuracionEstimada(diaDTO.getDuracionEstimada());
                itinerario.setGrupo(grupo);
                itinerarios.add(itinerario);
            }
            grupo.setItinerarios(itinerarios);
        }

        // Guardar el grupo final sin volver a establecer la relación con el viaje
        return grupoViajeRepository.save(grupo);
    }

    @Override
    public boolean viajeYaAsignado(Long idViaje) {
        return grupoViajeRepository.existsByViajeIdViaje(idViaje);
    }

    @Override
    public List<TipoViaje> obtenerTiposViaje() {
        return tipoViajeRepository.findAll();
    }

    @Override
    public List<GrupoViaje> buscarGrupos(
            String destino,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Long idTipoViaje,
            String rangoEdad,
            Boolean verificado,
            List<String> etiquetas,
            String ordenar) {

        // En una implementación real, aquí se realizaría una consulta con filtros
        // Por ahora, simplemente devolvemos todos los grupos
        return grupoViajeRepository.findAll();

        // Para una implementación más completa, se podría usar Specification o QueryDSL
        // para construir consultas dinámicas basadas en los filtros proporcionados
    }

    @Override
    public GrupoViaje buscarGrupoPorId(Long id) {
        return grupoViajeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grupo de viaje no encontrado"));
    }

    @Override
    public GrupoViaje actualizarGrupoViaje(Long idGrupo, CrearGrupoViajeDTO dto) {
        // Buscar el grupo por id
        GrupoViaje grupo = grupoViajeRepository.findById(idGrupo)
                .orElseThrow(() -> new RuntimeException("Grupo de viaje no encontrado"));

        // Obtener el viaje asociado
        Viaje viaje = grupo.getViaje();
        if (viaje == null) {
            throw new RuntimeException("Viaje asociado no encontrado");
        }

        // Actualizar campos del viaje
        viaje.setDestinoPrincipal(dto.getDestinoPrincipal());
        viaje.setFechaInicio(dto.getFechaInicio());
        viaje.setFechaFin(dto.getFechaFin());
        viaje.setDescripcion(dto.getDescripcion());
        viaje.setPuntoEncuentro(dto.getPuntoEncuentro());
        viaje.setImagenDestacada(dto.getImagenDestacada());
        viaje.setRangoEdadMin(dto.getRangoEdadMin());
        viaje.setRangoEdadMax(dto.getRangoEdadMax());
        // No se actualiza fechaCreacion ni estado, salvo que quieras hacerlo

        // Actualizar tipo de viaje si se especificó
        if (dto.getIdTipoViaje() != null) {
            TipoViaje tipoViaje = tipoViajeRepository.findById(dto.getIdTipoViaje())
                    .orElseThrow(() -> new RuntimeException("Tipo de viaje no encontrado"));
            viaje.setTipo(tipoViaje);
        } else {
            viaje.setTipo(null);
        }

        viajeRepository.save(viaje);

        // Actualizar campos del grupo
        grupo.setNombreViaje(dto.getNombreViaje());
        // No se actualiza fechaCreacion ni estado salvo que quieras hacerlo

        // Actualizar etiquetas
        if (dto.getEtiquetas() != null) {
            Set<Etiqueta> etiquetas = new HashSet<>();
            for (String nombreEtiqueta : dto.getEtiquetas()) {
                Etiqueta etiqueta = etiquetaRepository.findByNombreEtiqueta(nombreEtiqueta)
                        .orElseGet(() -> {
                            Etiqueta nuevaEtiqueta = new Etiqueta();
                            nuevaEtiqueta.setNombreEtiqueta(nombreEtiqueta);
                            return etiquetaRepository.save(nuevaEtiqueta);
                        });
                etiquetas.add(etiqueta);
            }
            grupo.setEtiquetas(etiquetas);
        } else {
            grupo.setEtiquetas(new HashSet<>());
        }

        // Actualizar itinerarios
        if (dto.getDiasItinerarioJson() != null && !dto.getDiasItinerarioJson().isEmpty()) {
            try {
                List<DiaItinerarioDTO> diasItinerario = objectMapper.readValue(
                        dto.getDiasItinerarioJson(),
                        new TypeReference<List<DiaItinerarioDTO>>() {
                        });
                dto.setDiasItinerario(diasItinerario);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error al procesar el itinerario: " + e.getMessage());
            }
        }

        if (dto.getDiasItinerario() != null) {
            // Limpiar itinerarios actuales antes de agregar nuevos
            if (grupo.getItinerarios() != null) {
                grupo.getItinerarios().clear();
            } else {
                grupo.setItinerarios(new HashSet<>());
            }
            for (DiaItinerarioDTO diaDTO : dto.getDiasItinerario()) {
                Itinerario itinerario = new Itinerario();
                itinerario.setDiaNumero(diaDTO.getDiaNumero());
                itinerario.setTitulo(diaDTO.getTitulo());
                itinerario.setDescripcion(diaDTO.getDescripcion());
                itinerario.setPuntoPartida(diaDTO.getPuntoPartida());
                itinerario.setPuntoLlegada(diaDTO.getPuntoLlegada());
                itinerario.setDuracionEstimada(diaDTO.getDuracionEstimada());
                itinerario.setGrupo(grupo);
                grupo.getItinerarios().add(itinerario);
            }
        }

        // Guardar grupo actualizado
        return grupoViajeRepository.save(grupo);
    }

}