package com.add.venture.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.add.venture.dto.TestimonioDTO;
import com.add.venture.model.GrupoViaje;
import com.add.venture.model.Testimonio;
import com.add.venture.model.Usuario;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.TestimonioRepository;

@Service
public class TestimonioServiceImpl implements ITestimonioService {

    @Autowired
    private TestimonioRepository testimonioRepository;
    
    @Autowired
    private GrupoViajeRepository grupoViajeRepository;

    @Override
    @Transactional
    public TestimonioDTO crearTestimonio(TestimonioDTO dto, Usuario autor) {
        Testimonio testimonio = Testimonio.builder()
                .comentario(dto.getComentario())
                .calificacion(dto.getCalificacion())
                .anonimo(dto.getAnonimo())
                .autor(autor)
                .build();
        
        // Si viene con idGrupo, asociarlo
        if (dto.getIdGrupo() != null) {
            GrupoViaje grupo = grupoViajeRepository.findById(dto.getIdGrupo())
                    .orElse(null);
            testimonio.setGrupo(grupo);
        }
        
        Testimonio saved = testimonioRepository.save(testimonio);
        return convertirADTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestimonioDTO> obtenerTestimoniosDestacados(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return testimonioRepository.findDestacados(pageable)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestimonioDTO> obtenerTestimoniosAprobados(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return testimonioRepository.findByAprobadoTrueOrderByFechaDesc(pageable)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestimonioDTO> obtenerTestimoniosPendientes() {
        return testimonioRepository.findByAprobadoFalseOrderByFechaDesc()
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TestimonioDTO aprobarTestimonio(Long idTestimonio) {
        Testimonio testimonio = testimonioRepository.findById(idTestimonio)
                .orElseThrow(() -> new RuntimeException("Testimonio no encontrado"));
        
        testimonio.setAprobado(true);
        Testimonio updated = testimonioRepository.save(testimonio);
        return convertirADTO(updated);
    }

    @Override
    @Transactional
    public TestimonioDTO marcarDestacado(Long idTestimonio, boolean destacado) {
        Testimonio testimonio = testimonioRepository.findById(idTestimonio)
                .orElseThrow(() -> new RuntimeException("Testimonio no encontrado"));
        
        // Solo se puede destacar si está aprobado
        if (destacado && !testimonio.getAprobado()) {
            throw new RuntimeException("Solo se pueden destacar testimonios aprobados");
        }
        
        testimonio.setDestacado(destacado);
        Testimonio updated = testimonioRepository.save(testimonio);
        return convertirADTO(updated);
    }

    @Override
    @Transactional
    public void eliminarTestimonio(Long idTestimonio) {
        testimonioRepository.deleteById(idTestimonio);
    }

    private TestimonioDTO convertirADTO(Testimonio testimonio) {
        TestimonioDTO dto = TestimonioDTO.builder()
                .idTestimonio(testimonio.getIdTestimonio())
                .comentario(testimonio.getComentario())
                .calificacion(testimonio.getCalificacion())
                .anonimo(testimonio.getAnonimo())
                .fecha(testimonio.getFecha())
                .aprobado(testimonio.getAprobado())
                .destacado(testimonio.getDestacado())
                .build();
        
        // Solo incluir datos del autor si NO es anónimo
        if (!testimonio.getAnonimo() && testimonio.getAutor() != null) {
            Usuario autor = testimonio.getAutor();
            dto.setNombreAutor(autor.getNombre());
            dto.setApellidoAutor(autor.getApellidos());
            dto.setCiudadAutor(autor.getCiudad());
            dto.setPaisAutor(autor.getPais());
            dto.setFotoPerfilAutor(autor.getFotoPerfil());
        }
        
        if (testimonio.getGrupo() != null) {
            dto.setIdGrupo(testimonio.getGrupo().getIdGrupo());
        }
        
        return dto;
    }
}
