package com.ComparaJuegos.game_comparer.controladores;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.ComparaJuegos.game_comparer.UsuarioRepositorio;
import com.ComparaJuegos.game_comparer.models.Usuario;




@Controller
public class controladorSesiones {

    @GetMapping("/iniciar")

    public String inicio(){
        return "principal";
    }

    @GetMapping("/inicioSesion")
    public String IniciarSesion() {
        return "inicioSesion";
    }

    //Dirección al formulario a secas
    @GetMapping("/registro")
    public String registrarse(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    //Necesario para que funcione lo siguiente, sino se queja
    private final UsuarioRepositorio usuarioRepositorio;

    controladorSesiones(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    //Esto recibe lo del formulario y lo guarda
    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute Usuario usuario) {
        //TODO: process POST request
        usuarioRepositorio.save(usuario);
        return "redirect:/inicioSesion";
    }
    
    
 
}
