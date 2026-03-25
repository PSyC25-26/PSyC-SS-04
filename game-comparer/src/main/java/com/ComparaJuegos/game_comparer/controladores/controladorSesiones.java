package com.ComparaJuegos.game_comparer.controladores;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ComparaJuegos.game_comparer.UsuarioRepositorio;
import com.ComparaJuegos.game_comparer.models.Usuario;
import com.ComparaJuegos.game_comparer.models.Wishlist;

import jakarta.servlet.http.HttpSession;




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

    @GetMapping("/prueba_login") // Esta es la "dirección" de la prueba
    public String paginaDePrueba() {
        // Este String "home" debe ser el nombre exacto de tu archivo home.html
        return "prueba_login";
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
    
    //metodo para la creacion de wishlist (aun le falta plantilla y pagina)

    @PostMapping("/crear-wishlist")
    public String crearPropiaWishlist(@ModelAttribute Usuario usuarioActual,@RequestParam("nombre") String nombreLista) {
        Usuario usuarioDB = usuarioRepositorio.findById(usuarioActual.getId()).orElse(null);
        
        //creacion de wishlist (basico aun)
        if (usuarioDB != null) {
            
            Wishlist nueva = new Wishlist();
            nueva.setUsuario(usuarioDB);
            nueva.setNombre(nombreLista);
            usuarioDB.getWishlists().add(nueva);
            
            usuarioRepositorio.save(usuarioDB);
        }
        
        return "redirect:/perfil";
    }

    //metodo para ver el perfil del usuario
    @GetMapping("/perfil")
    public String verPerfil(Model model, HttpSession session) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioUsuario"); 

        if (usuarioLogueado == null) {
            return "redirect:/inicioSesion"; 
        }
        Usuario usuarioDB = usuarioRepositorio.findById(usuarioLogueado.getId()).orElse(null);
        model.addAttribute("usuario", usuarioDB);
        
        return "perfil.html";
    }
    
}
