package com.add.venture.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.add.venture.model.GrupoViaje;
import com.add.venture.model.MensajeGrupo;
import com.add.venture.repository.GrupoViajeRepository;
import com.add.venture.repository.MensajeGrupoRepository;

@Service
public class ImageCleanupService {

    @Autowired
    private GrupoViajeRepository grupoViajeRepository;
    
    @Autowired
    private MensajeGrupoRepository mensajeGrupoRepository;

    /**
     * Tarea programada que se ejecuta diariamente a las 2:00 AM
     * Elimina las imágenes de los grupos que han sido cerrados hace más de 1 mes
     */
    @Scheduled(cron = "0 0 2 * * ?") // Cada día a las 2:00 AM
    public void eliminarImagenesAntiguas() {
        System.out.println("=== Iniciando limpieza de imágenes antiguas ===");
        
        try {
            // Calcular la fecha límite (1 mes atrás desde hoy)
            LocalDateTime fechaLimite = LocalDateTime.now().minusMonths(1);
            
            // Buscar grupos cerrados o concluidos hace más de 1 mes
            List<GrupoViaje> gruposCerrados = grupoViajeRepository
                .findGruposCerradosAntiguos(
                    List.of("cerrado", "concluido"), 
                    fechaLimite
                );
            
            System.out.println("Grupos cerrados hace más de 1 mes: " + gruposCerrados.size());
            
            int imagenesEliminadas = 0;
            
            for (GrupoViaje grupo : gruposCerrados) {
                // Obtener todos los mensajes de tipo imagen de este grupo
                List<MensajeGrupo> imagenes = mensajeGrupoRepository
                    .findByGrupoAndTipoMensajeOrderByFechaEnvioDesc(grupo, "imagen");
                
                for (MensajeGrupo mensaje : imagenes) {
                    if (mensaje.getArchivoUrl() != null && !mensaje.getArchivoUrl().isEmpty()) {
                        // Eliminar el archivo físico
                        String rutaArchivo = mensaje.getArchivoUrl();
                        if (rutaArchivo.startsWith("/")) {
                            rutaArchivo = rutaArchivo.substring(1);
                        }
                        
                        Path filePath = Paths.get(rutaArchivo);
                        File file = filePath.toFile();
                        
                        if (file.exists()) {
                            try {
                                Files.delete(filePath);
                                System.out.println("Imagen eliminada: " + rutaArchivo);
                                imagenesEliminadas++;
                            } catch (Exception e) {
                                System.err.println("Error al eliminar archivo: " + rutaArchivo + " - " + e.getMessage());
                            }
                        }
                        
                        // Eliminar el mensaje de la base de datos
                        mensajeGrupoRepository.delete(mensaje);
                    }
                }
            }
            
            System.out.println("=== Limpieza completada: " + imagenesEliminadas + " imágenes eliminadas ===");
            
        } catch (Exception e) {
            System.err.println("Error en la limpieza de imágenes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Método manual para ejecutar la limpieza (útil para testing)
     */
    public void ejecutarLimpiezaManual() {
        eliminarImagenesAntiguas();
    }
}
