package com.ComparaJuegos.game_comparer;

import com.ComparaJuegos.game_comparer.models.Juego;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JuegoRepositorio extends JpaRepository<Juego, Long> {
    Optional<Juego> findFirstByNameIgnoreCase(String name);
}
