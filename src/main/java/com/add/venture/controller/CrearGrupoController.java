package com.add.venture.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.add.venture.dto.CrearGrupoViajeDTO;
import com.add.venture.helper.UsuarioAutenticadoHelper;
import com.add.venture.service.IGrupoViajeService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/crear-grupo")
public class CrearGrupoController {

    @Autowired
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @Autowired
    private IGrupoViajeService grupoViajeService;

    @GetMapping
    public String mostrarFormularioCreacion(Model model) {
        // Cargar datos del usuario para la navbar y perfil
        usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);

        // Añadir el DTO para el formulario si no existe
        if (!model.containsAttribute("datosViaje")) {
            model.addAttribute("datosViaje", new CrearGrupoViajeDTO());
        }

        // Cargar tipos de viaje para el selector
        model.addAttribute("tiposViaje", grupoViajeService.obtenerTiposViaje());

        return "grupos/crear";
    }

    @PostMapping
    public String procesarCreacion(@Valid @ModelAttribute("datosViaje") CrearGrupoViajeDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validaciones personalizadas exhaustivas
        validarDatosPersonalizados(dto, result);

        // Verificar errores de validación
        if (result.hasErrors()) {
            // Cargar datos del usuario para la navbar y perfil
            usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
            usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);

            // Cargar tipos de viaje para el selector
            model.addAttribute("tiposViaje", grupoViajeService.obtenerTiposViaje());

            // Agregar mensaje de error general
            model.addAttribute("error", "Por favor corrige los errores del formulario antes de continuar.");

            return "grupos/crear";
        }

        try {
            // Crear el grupo de viaje
            grupoViajeService.crearGrupoViaje(dto);

            // Añadir mensaje de éxito como parámetros de URL
            return "redirect:/grupos?mensaje=" + URLEncoder.encode("Grupo de viaje creado exitosamente", StandardCharsets.UTF_8) + "&tipoMensaje=success";
        } catch (Exception e) {
            // Manejar errores
            model.addAttribute("error", "Error al crear el grupo: " + e.getMessage());

            // Cargar datos del usuario para la navbar y perfil
            usuarioAutenticadoHelper.cargarDatosUsuarioParaNavbar(model);
            usuarioAutenticadoHelper.cargarUsuarioParaPerfil(model);

            // Cargar tipos de viaje para el selector
            model.addAttribute("tiposViaje", grupoViajeService.obtenerTiposViaje());

            return "grupos/crear";
        }
    }

    private void validarDatosPersonalizados(CrearGrupoViajeDTO dto, BindingResult result) {
        // 1. Validar campos obligatorios que podrían estar vacíos
        if (dto.getNombreViaje() == null || dto.getNombreViaje().trim().isEmpty()) {
            result.addError(new FieldError("datosViaje", "nombreViaje", 
                "El nombre del viaje es obligatorio"));
        } else if (dto.getNombreViaje().trim().length() < 3) {
            result.addError(new FieldError("datosViaje", "nombreViaje", 
                "El nombre del viaje debe tener al menos 3 caracteres"));
        } else if (dto.getNombreViaje().trim().length() > 100) {
            result.addError(new FieldError("datosViaje", "nombreViaje", 
                "El nombre del viaje no puede exceder 100 caracteres"));
        }

        // 2. Validar destino principal
        if (dto.getDestinoPrincipal() == null || dto.getDestinoPrincipal().trim().isEmpty()) {
            result.addError(new FieldError("datosViaje", "destinoPrincipal", 
                "El destino principal es obligatorio"));
        } else if (dto.getDestinoPrincipal().trim().length() < 2) {
            result.addError(new FieldError("datosViaje", "destinoPrincipal", 
                "El destino principal debe tener al menos 2 caracteres"));
        } else if (dto.getDestinoPrincipal().trim().length() > 100) {
            result.addError(new FieldError("datosViaje", "destinoPrincipal", 
                "El destino principal no puede exceder 100 caracteres"));
        }

        // 3. Validar fechas
        if (dto.getFechaInicio() == null) {
            result.addError(new FieldError("datosViaje", "fechaInicio", 
                "La fecha de inicio es obligatoria"));
        } else if (dto.getFechaInicio().isBefore(LocalDate.now())) {
            result.addError(new FieldError("datosViaje", "fechaInicio", 
                "La fecha de inicio debe ser en el futuro"));
        }

        if (dto.getFechaFin() == null) {
            result.addError(new FieldError("datosViaje", "fechaFin", 
                "La fecha de fin es obligatoria"));
        }

        // Validar que la fecha de fin sea después de la fecha de inicio
        if (dto.getFechaInicio() != null && dto.getFechaFin() != null) {
            if (!dto.isFechaFinDespuesDeFechaInicio()) {
                result.addError(new FieldError("datosViaje", "fechaFin", 
                    "La fecha de fin debe ser igual o posterior a la fecha de inicio"));
            }
        }

        // 4. Validar descripción
        if (dto.getDescripcion() == null || dto.getDescripcion().trim().isEmpty()) {
            result.addError(new FieldError("datosViaje", "descripcion", 
                "La descripción del viaje es obligatoria"));
        } else if (dto.getDescripcion().trim().length() < 10) {
            result.addError(new FieldError("datosViaje", "descripcion", 
                "La descripción debe tener al menos 10 caracteres"));
        } else if (dto.getDescripcion().trim().length() > 1000) {
            result.addError(new FieldError("datosViaje", "descripcion", 
                "La descripción no puede exceder 1000 caracteres"));
        }

        // 5. Validar punto de encuentro
        if (dto.getPuntoEncuentro() == null || dto.getPuntoEncuentro().trim().isEmpty()) {
            result.addError(new FieldError("datosViaje", "puntoEncuentro", 
                "El punto de encuentro es obligatorio"));
        } else if (dto.getPuntoEncuentro().trim().length() < 5) {
            result.addError(new FieldError("datosViaje", "puntoEncuentro", 
                "El punto de encuentro debe tener al menos 5 caracteres"));
        } else if (dto.getPuntoEncuentro().trim().length() > 500) {
            result.addError(new FieldError("datosViaje", "puntoEncuentro", 
                "El punto de encuentro no puede exceder 500 caracteres"));
        }

        // 6. Validar número máximo de participantes
        if (dto.getMaxParticipantes() == null) {
            result.addError(new FieldError("datosViaje", "maxParticipantes", 
                "El número máximo de participantes es obligatorio"));
        } else if (dto.getMaxParticipantes() < 2) {
            result.addError(new FieldError("datosViaje", "maxParticipantes", 
                "Debe haber al menos 2 participantes"));
        } else if (dto.getMaxParticipantes() > 20) {
            result.addError(new FieldError("datosViaje", "maxParticipantes", 
                "No se pueden tener más de 20 participantes"));
        }

        // 7. Validar rango de edad
        if (dto.getRangoEdadMin() == null) {
            result.addError(new FieldError("datosViaje", "rangoEdadMin", 
                "La edad mínima es obligatoria"));
        } else if (dto.getRangoEdadMin() < 18) {
            result.addError(new FieldError("datosViaje", "rangoEdadMin", 
                "La edad mínima debe ser de al menos 18 años"));
        } else if (dto.getRangoEdadMin() > 80) {
            result.addError(new FieldError("datosViaje", "rangoEdadMin", 
                "La edad mínima no puede ser mayor a 80 años"));
        }

        if (dto.getRangoEdadMax() == null) {
            result.addError(new FieldError("datosViaje", "rangoEdadMax", 
                "La edad máxima es obligatoria"));
        } else if (dto.getRangoEdadMax() < 18) {
            result.addError(new FieldError("datosViaje", "rangoEdadMax", 
                "La edad máxima debe ser de al menos 18 años"));
        } else if (dto.getRangoEdadMax() > 80) {
            result.addError(new FieldError("datosViaje", "rangoEdadMax", 
                "La edad máxima no puede ser mayor a 80 años"));
        }

        // Validar rango de edad
        if (dto.getRangoEdadMin() != null && dto.getRangoEdadMax() != null) {
            if (!dto.isRangoEdadValido()) {
                result.addError(new FieldError("datosViaje", "rangoEdadMax", 
                    "La edad máxima debe ser mayor o igual a la edad mínima"));
            }
        }

        // 8. Validar URL de imagen si se proporciona
        if (dto.getImagenDestacada() != null && !dto.getImagenDestacada().trim().isEmpty()) {
            String imagenUrl = dto.getImagenDestacada().trim();
            if (!imagenUrl.startsWith("http://") && !imagenUrl.startsWith("https://")) {
                result.addError(new FieldError("datosViaje", "imagenDestacada", 
                    "La URL de la imagen debe comenzar con http:// o https://"));
            }
            // Validación adicional de formato URL
            try {
                new java.net.URL(imagenUrl);
            } catch (java.net.MalformedURLException e) {
                result.addError(new FieldError("datosViaje", "imagenDestacada", 
                    "La URL de la imagen no tiene un formato válido"));
            }
        }

        // 9. Validar etiquetas
        List<String> etiquetas = dto.getEtiquetas();
        if (etiquetas == null || etiquetas.isEmpty()) {
            result.addError(new FieldError("datosViaje", "etiquetas", 
                "Debe agregar al menos una etiqueta para el viaje"));
        } else {
            // Validar número de etiquetas (máximo 10)
            if (etiquetas.size() > 10) {
                result.addError(new FieldError("datosViaje", "etiquetas", 
                    "No puede agregar más de 10 etiquetas"));
            }
            
            // Validar cada etiqueta individualmente
            for (int i = 0; i < etiquetas.size(); i++) {
                String etiqueta = etiquetas.get(i);
                if (etiqueta == null || etiqueta.trim().isEmpty()) {
                    result.addError(new FieldError("datosViaje", "etiquetas", 
                        "Las etiquetas no pueden estar vacías"));
                    break;
                } else if (etiqueta.trim().length() > 20) {
                    result.addError(new FieldError("datosViaje", "etiquetas", 
                        "Las etiquetas no pueden tener más de 20 caracteres"));
                    break;
                }
            }
            
            // Validar etiquetas duplicadas
            long distinctCount = etiquetas.stream().map(String::toLowerCase).distinct().count();
            if (distinctCount != etiquetas.size()) {
                result.addError(new FieldError("datosViaje", "etiquetas", 
                    "No se permiten etiquetas duplicadas"));
            }
        }


    }
}