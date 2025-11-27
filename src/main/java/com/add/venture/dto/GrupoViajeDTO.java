package com.add.venture.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class GrupoViajeDTO {
    private Long idGrupo;
    private String nombreViaje;
    private String estado;
    private Integer maxParticipantes;
    private LocalDateTime fechaCreacion;
    private ViajeDTO viaje;
    private CreadorDTO creador;
    private List<ParticipanteDTO> participantes;
    private List<EtiquetaDTO> etiquetas;

    // Clase interna para Viaje
    public static class ViajeDTO {
        private Long idViaje;
        private String descripcion;
        private String destinoPrincipal;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private String imagenDestacada;
        private Integer rangoEdadMin;
        private Integer rangoEdadMax;
        private Boolean esVerificado;

        // Getters y Setters
        public Long getIdViaje() {
            return idViaje;
        }

        public void setIdViaje(Long idViaje) {
            this.idViaje = idViaje;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDestinoPrincipal() {
            return destinoPrincipal;
        }

        public void setDestinoPrincipal(String destinoPrincipal) {
            this.destinoPrincipal = destinoPrincipal;
        }

        public LocalDate getFechaInicio() {
            return fechaInicio;
        }

        public void setFechaInicio(LocalDate fechaInicio) {
            this.fechaInicio = fechaInicio;
        }

        public LocalDate getFechaFin() {
            return fechaFin;
        }

        public void setFechaFin(LocalDate fechaFin) {
            this.fechaFin = fechaFin;
        }

        public String getImagenDestacada() {
            return imagenDestacada;
        }

        public void setImagenDestacada(String imagenDestacada) {
            this.imagenDestacada = imagenDestacada;
        }

        public Integer getRangoEdadMin() {
            return rangoEdadMin;
        }

        public void setRangoEdadMin(Integer rangoEdadMin) {
            this.rangoEdadMin = rangoEdadMin;
        }

        public Integer getRangoEdadMax() {
            return rangoEdadMax;
        }

        public void setRangoEdadMax(Integer rangoEdadMax) {
            this.rangoEdadMax = rangoEdadMax;
        }

        public Boolean getEsVerificado() {
            return esVerificado;
        }

        public void setEsVerificado(Boolean esVerificado) {
            this.esVerificado = esVerificado;
        }
    }

    // Clase interna para Creador
    public static class CreadorDTO {
        private Long idUsuario;
        private String nombre;
        private String apellidos;
        private String fotoPerfil;

        // Getters y Setters
        public Long getIdUsuario() {
            return idUsuario;
        }

        public void setIdUsuario(Long idUsuario) {
            this.idUsuario = idUsuario;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getApellidos() {
            return apellidos;
        }

        public void setApellidos(String apellidos) {
            this.apellidos = apellidos;
        }

        public String getFotoPerfil() {
            return fotoPerfil;
        }

        public void setFotoPerfil(String fotoPerfil) {
            this.fotoPerfil = fotoPerfil;
        }
    }

    // Clase interna para Participante
    public static class ParticipanteDTO {
        private Long idUsuario;
        private String nombre;
        private String apellidos;
        private String fotoPerfil;

        // Getters y Setters
        public Long getIdUsuario() {
            return idUsuario;
        }

        public void setIdUsuario(Long idUsuario) {
            this.idUsuario = idUsuario;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getApellidos() {
            return apellidos;
        }

        public void setApellidos(String apellidos) {
            this.apellidos = apellidos;
        }

        public String getFotoPerfil() {
            return fotoPerfil;
        }

        public void setFotoPerfil(String fotoPerfil) {
            this.fotoPerfil = fotoPerfil;
        }
    }

    // Clase interna para Etiqueta
    public static class EtiquetaDTO {
        private Integer idEtiqueta;
        private String nombreEtiqueta;

        // Getters y Setters
        public Integer getIdEtiqueta() {
            return idEtiqueta;
        }

        public void setIdEtiqueta(Integer idEtiqueta) {
            this.idEtiqueta = idEtiqueta;
        }

        public String getNombreEtiqueta() {
            return nombreEtiqueta;
        }

        public void setNombreEtiqueta(String nombreEtiqueta) {
            this.nombreEtiqueta = nombreEtiqueta;
        }
    }

    // Getters y Setters principales
    public Long getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(Long idGrupo) {
        this.idGrupo = idGrupo;
    }

    public String getNombreViaje() {
        return nombreViaje;
    }

    public void setNombreViaje(String nombreViaje) {
        this.nombreViaje = nombreViaje;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getMaxParticipantes() {
        return maxParticipantes;
    }

    public void setMaxParticipantes(Integer maxParticipantes) {
        this.maxParticipantes = maxParticipantes;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public ViajeDTO getViaje() {
        return viaje;
    }

    public void setViaje(ViajeDTO viaje) {
        this.viaje = viaje;
    }

    public CreadorDTO getCreador() {
        return creador;
    }

    public void setCreador(CreadorDTO creador) {
        this.creador = creador;
    }

    public List<ParticipanteDTO> getParticipantes() {
        return participantes;
    }

    public void setParticipantes(List<ParticipanteDTO> participantes) {
        this.participantes = participantes;
    }

    public List<EtiquetaDTO> getEtiquetas() {
        return etiquetas;
    }

    public void setEtiquetas(List<EtiquetaDTO> etiquetas) {
        this.etiquetas = etiquetas;
    }
}
