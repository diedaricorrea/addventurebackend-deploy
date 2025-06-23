package com.add.venture.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.add.venture.model.GrupoViaje;
import com.add.venture.model.Notificacion;
import com.add.venture.model.Usuario;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.NotificacionRepository;

@Service
public class NotificacionServiceImpl implements INotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;
    
    @Autowired
    private GrupoViajeRepository grupoViajeRepository;

    @Override
    public Notificacion crearNotificacionSolicitudUnion(Usuario solicitante, Usuario lider, Long idGrupo, String nombreGrupo) {
        // Obtener el grupo para la referencia
        GrupoViaje grupo = grupoViajeRepository.findById(idGrupo).orElse(null);
        
        Notificacion notificacion = Notificacion.builder()
                .tipo("SOLICITUD_UNION")
                .contenido(solicitante.getNombre() + " " + solicitante.getApellidos() + 
                          " quiere unirse al grupo \"" + nombreGrupo + "\"")
                .usuario(lider)
                .grupo(grupo)
                .solicitante(solicitante)
                .leido(false)
                .fecha(LocalDateTime.now())
                .estado("activo")
                .build();
        
        return notificacionRepository.save(notificacion);
    }

    @Override
    public List<Notificacion> obtenerNotificacionesUsuario(Usuario usuario) {
        return notificacionRepository.findByUsuarioOrderByFechaDesc(usuario);
    }

    @Override
    public List<Notificacion> obtenerNotificacionesNoLeidas(Usuario usuario) {
        return notificacionRepository.findByUsuarioAndLeidoFalseOrderByFechaDesc(usuario);
    }

    @Override
    public long contarNotificacionesNoLeidas(Usuario usuario) {
        return notificacionRepository.countByUsuarioAndLeidoFalse(usuario);
    }

    @Override
    public void marcarComoLeida(Long idNotificacion) {
        notificacionRepository.findById(idNotificacion).ifPresent(notificacion -> {
            notificacion.setLeido(true);
            notificacion.setFechaLectura(LocalDateTime.now());
            notificacionRepository.save(notificacion);
        });
    }

    @Override
    public void marcarTodasComoLeidas(Usuario usuario) {
        List<Notificacion> notificacionesNoLeidas = notificacionRepository
                .findByUsuarioAndLeidoFalseOrderByFechaDesc(usuario);
        
        for (Notificacion notificacion : notificacionesNoLeidas) {
            notificacion.setLeido(true);
            notificacion.setFechaLectura(LocalDateTime.now());
        }
        
        notificacionRepository.saveAll(notificacionesNoLeidas);
    }
} 