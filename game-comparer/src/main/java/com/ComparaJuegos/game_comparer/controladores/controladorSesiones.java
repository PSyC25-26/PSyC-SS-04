package com.ComparaJuegos.game_comparer.controladores;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;

    controladorSesiones(UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping({ "/", "/iniciar" })
    public String inicio() {
        return "principal";
    }

    @GetMapping("/inicioSesion")
    public String IniciarSesion() {
        return "inicioSesion";
    }

    // Dirección al formulario a secas
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

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute Usuario usuario, Model model) {
        try {
            usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
            usuarioRepositorio.save(usuario);
            return "redirect:/inicioSesion";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorEmail", true);
            return "registro";
        } catch (Exception e) {
            model.addAttribute("errorServidor", true);
            return "registro";
        }
    }

    // metodo para la creacion de wishlist (aun le falta plantilla y pagina)

    @PostMapping("/crear-wishlist")
    public String crearPropiaWishlist(@ModelAttribute Usuario usuarioActual,
            @RequestParam("nombre") String nombreLista) {
        Usuario usuarioDB = usuarioRepositorio.findById(usuarioActual.getId()).orElse(null);

        // creacion de wishlist (basico aun)
        if (usuarioDB != null) {

            Wishlist nueva = new Wishlist();
            nueva.setUsuario(usuarioDB);
            nueva.setNombre(nombreLista);
            usuarioDB.getWishlists().add(nueva);

            usuarioRepositorio.save(usuarioDB);
        }

        return "redirect:/perfil";
    }

    // metodo para ver el perfil del usuario
    @GetMapping("/perfil")
    public String verPerfil(Model model,
            @RequestParam(required = false) Long testUserId) {
        Usuario usuario;
        if (testUserId != null) {
            // Modo test: obtener usuario por ID directamente (sin autenticación requerida)
            usuario = usuarioRepositorio.findById(testUserId)
                    .orElseThrow(() -> new RuntimeException("Usuario test no encontrado con ID: " + testUserId));
        } else {
            // Modo normal: obtener usuario autenticado desde el contexto de seguridad
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) auth.getPrincipal();
                usuario = usuarioRepositorio.findByEmail(userDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userDetails.getUsername()));
            } else {
                throw new RuntimeException("No hay usuario autenticado ni testUserId proporcionado");
            }
        }
        model.addAttribute("usuario", usuario);
        return "perfil";
    }

}
