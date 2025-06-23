package com.add.venture.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.add.venture.dto.PerfilUsuarioDTO;
import com.add.venture.helper.UsuarioAutenticadoHelper;
import com.add.venture.service.IUsuarioService;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    @Autowired
    private UsuarioAutenticadoHelper usuarioHelper;

    @Autowired
    private IUsuarioService usuarioService;

    @GetMapping
    public String mostrarVistaPerfil(Model model) {
        usuarioHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioHelper.cargarUsuarioParaPerfil(model);
        return "user/perfil";
    }

    // Configura el data binder ANTES de procesar la petición
    @InitBinder("usuario")
    // "No intentes asignar los valores del formulario llamados imagenPerfil y
    // imagenPortada al objeto usuario (PerfilUsuarioDTO), porque esos campos no le
    // pertenecen."
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("imagenPerfil", "imagenPortada");
    }

    @GetMapping("/configuracion")
    public String mostrarVistaConfiguracion(Model model) {
        usuarioHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioHelper.cargarUsuarioParaPerfil(model);
        return "user/configuracion";
    }

    @PostMapping("/configuracion")
    public String actualizarConfiguracion(
            @ModelAttribute("usuario") PerfilUsuarioDTO perfilDto,
            @RequestParam(value = "imagenPerfil", required = false) MultipartFile imagenPerfil,
            @RequestParam(value = "imagenPortada", required = false) MultipartFile imagenPortada,
            Model model) {

        usuarioService.actualizarPerfil(perfilDto, imagenPerfil, imagenPortada);

        usuarioHelper.cargarDatosUsuarioParaNavbar(model);
        usuarioHelper.cargarUsuarioParaPerfil(model);
        model.addAttribute("mensaje", "Perfil actualizado correctamente");

        return "user/configuracion";
    }
}
