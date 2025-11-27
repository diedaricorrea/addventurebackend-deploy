package com.add.venture.dto;

import java.util.List;

public class MisViajesResponseDTO {
    private List<GrupoViajeDTO> gruposCreados;
    private List<GrupoViajeDTO> gruposUnidos;
    private List<GrupoViajeDTO> gruposCerrados;
    private int totalGrupos;

    // Getters y Setters
    public List<GrupoViajeDTO> getGruposCreados() {
        return gruposCreados;
    }

    public void setGruposCreados(List<GrupoViajeDTO> gruposCreados) {
        this.gruposCreados = gruposCreados;
    }

    public List<GrupoViajeDTO> getGruposUnidos() {
        return gruposUnidos;
    }

    public void setGruposUnidos(List<GrupoViajeDTO> gruposUnidos) {
        this.gruposUnidos = gruposUnidos;
    }

    public List<GrupoViajeDTO> getGruposCerrados() {
        return gruposCerrados;
    }

    public void setGruposCerrados(List<GrupoViajeDTO> gruposCerrados) {
        this.gruposCerrados = gruposCerrados;
    }

    public int getTotalGrupos() {
        return totalGrupos;
    }

    public void setTotalGrupos(int totalGrupos) {
        this.totalGrupos = totalGrupos;
    }
}
