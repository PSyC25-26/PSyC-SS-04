package com.ComparaJuegos.game_comparer;

import com.ComparaJuegos.game_comparer.models.Usuario;
import com.ComparaJuegos.game_comparer.models.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WishlistRepositorio extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUsuario(Usuario usuario);
}
