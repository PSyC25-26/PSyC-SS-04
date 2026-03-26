package com.ComparaJuegos.game_comparer.controladores;

import com.ComparaJuegos.game_comparer.UsuarioRepositorio;
import com.ComparaJuegos.game_comparer.WishlistRepositorio;
import com.ComparaJuegos.game_comparer.dto.ResultadoBusquedaDTO;
import com.ComparaJuegos.game_comparer.models.Usuario;
import com.ComparaJuegos.game_comparer.models.Wishlist;
import com.ComparaJuegos.game_comparer.service.BusquedaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class BusquedaControlador {

    private final BusquedaService busquedaService;
    private final UsuarioRepositorio usuarioRepositorio;
    private final WishlistRepositorio wishlistRepositorio;

    public BusquedaControlador(BusquedaService busquedaService,
                                UsuarioRepositorio usuarioRepositorio,
                                WishlistRepositorio wishlistRepositorio) {
        this.busquedaService = busquedaService;
        this.usuarioRepositorio = usuarioRepositorio;
        this.wishlistRepositorio = wishlistRepositorio;
    }

    @GetMapping("/buscar")
    public String buscar(@RequestParam(required = false) String q,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model) {
        List<ResultadoBusquedaDTO> resultados = List.of();
        if (q != null && !q.isBlank()) {
            resultados = busquedaService.buscar(q);
        }

        Usuario usuario = usuarioRepositorio.findByEmail(userDetails.getUsername()).orElseThrow();
        List<Wishlist> wishlists = wishlistRepositorio.findByUsuario(usuario);

        model.addAttribute("q", q);
        model.addAttribute("resultados", resultados);
        model.addAttribute("wishlists", wishlists);
        return "buscar";
    }

    @PostMapping("/wishlist/agregar")
    public String agregar(@ModelAttribute ResultadoBusquedaDTO dto,
                          @RequestParam Long wishlistId) {
        Long id = busquedaService.agregarAWishlist(dto, wishlistId);
        return "redirect:/wishlist/" + id;
    }

    @GetMapping("/wishlist/{id}")
    public String verWishlist(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        Usuario usuario = usuarioRepositorio.findByEmail(userDetails.getUsername()).orElseThrow();
        Wishlist wishlist = wishlistRepositorio.findById(id).orElseThrow();

        if (!wishlist.getUsuario().getId().equals(usuario.getId())) {
            return "redirect:/buscar";
        }

        model.addAttribute("wishlist", wishlist);
        return "detalle-wishlist";
    }
}
