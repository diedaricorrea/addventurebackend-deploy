package com.add.venture.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

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
        
        Notificacion nuevaNotificacion = notificacionRepository.save(notificacion);
        
        // Enviar notificación por WebSocket al líder del grupo
        messagingTemplate.convertAndSend("/queue/notificaciones/" + lider.getIdUsuario(), nuevaNotificacion);
        
        return nuevaNotificacion;
    }

    @Override
    public Notificacion crearNotificacionSolicitudRechazada(Usuario solicitante, String nombreGrupo) {
        Notificacion notificacion = Notificacion.builder()
                .tipo("SOLICITUD_RECHAZADA")
                .contenido("Tu solicitud para unirte al grupo \"" + nombreGrupo + "\" ha sido rechazada. " +
                          "Puedes volver a intentar (máximo 3 intentos por grupo).")
                .usuario(solicitante)
                .grupo(null) // No necesitamos la referencia al grupo para esta notificación
                .solicitante(null)
                .leido(false)
                .fecha(LocalDateTime.now())
                .estado("activo")
                .build();
        
        Notificacion nuevaNotificacion = notificacionRepository.save(notificacion);
        
        // Enviar notificación por WebSocket al solicitante
        messagingTemplate.convertAndSend("/queue/notificaciones/" + solicitante.getIdUsuario(), nuevaNotificacion);
        
        return nuevaNotificacion;
    }

    @Override
    public Notificacion crearNotificacionSolicitudRechazada(Usuario solicitante, String nombreGrupo, int intentosUsados, int intentosMaximos) {
        String mensaje;
        if (intentosUsados >= intentosMaximos) {
            mensaje = "Tu solicitud para unirte al grupo \"" + nombreGrupo + "\" ha sido rechazada. " +
                     "Has alcanzado el límite máximo de " + intentosMaximos + " intentos.";
        } else {
            int intentosRestantes = intentosMaximos - intentosUsados;
            mensaje = "Tu solicitud para unirte al grupo \"" + nombreGrupo + "\" ha sido rechazada. " +
                     "Te quedan " + intentosRestantes + " intento" + (intentosRestantes == 1 ? "" : "s") + " más.";
        }
        
        Notificacion notificacion = Notificacion.builder()
                .tipo("SOLICITUD_RECHAZADA")
                .contenido(mensaje)
                .usuario(solicitante)
                .grupo(null)
                .solicitante(null)
                .leido(false)
                .fecha(LocalDateTime.now())
                .estado("activo")
                .build();
        
        Notificacion nuevaNotificacion = notificacionRepository.save(notificacion);
        
        // Enviar notificación por WebSocket al solicitante
        messagingTemplate.convertAndSend("/queue/notificaciones/" + solicitante.getIdUsuario(), nuevaNotificacion);
        
        return nuevaNotificacion;
    }

    @Override
    public Notificacion crearNotificacionSolicitudAceptada(Usuario solicitante, String nombreGrupo) {
        Notificacion notificacion = Notificacion.builder()
                .tipo("SOLICITUD_ACEPTADA")
                .contenido("¡Felicidades! Tu solicitud para unirte al grupo \"" + nombreGrupo + "\" ha sido aceptada. " +
                          "Ya eres oficialmente parte del grupo.")
                .usuario(solicitante)
                .grupo(null) // No necesitamos la referencia al grupo para esta notificación
                .solicitante(null)
                .leido(false)
                .fecha(LocalDateTime.now())
                .estado("activo")
                .build();
        
        Notificacion nuevaNotificacion = notificacionRepository.save(notificacion);
        
        // Enviar notificación por WebSocket al solicitante
        messagingTemplate.convertAndSend("/queue/notificaciones/" + solicitante.getIdUsuario(), nuevaNotificacion);
        
        return nuevaNotificacion;
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

    @Override
    public void eliminarTodasLasNotificaciones(Usuario usuario) {
        List<Notificacion> todasLasNotificaciones = notificacionRepository
                .findByUsuarioOrderByFechaDesc(usuario);
        
        notificacionRepository.deleteAll(todasLasNotificaciones);
    }
} 