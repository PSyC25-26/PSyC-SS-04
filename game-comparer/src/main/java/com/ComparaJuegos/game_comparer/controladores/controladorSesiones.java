package com.ComparaJuegos.game_comparer.controladores;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class controladorSesiones {
    
    @GetMapping("/iniciar")

    public String inicio(){
        return "plantilla";
    }

    @GetMapping("/inicioSesion")
    public String IniciarSesion() {
        return "inicioSesion";
    }

    @GetMapping("/registro")
    public String registrarse() {
        return "registro";
    }
    

 
}
