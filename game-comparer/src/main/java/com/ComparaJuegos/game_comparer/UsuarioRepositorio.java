package com.ComparaJuegos.game_comparer;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ComparaJuegos.game_comparer.models.Usuario;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long>{
    Optional<Usuario> findByEmail(String email); //Funcion para en el log in buscar por email
}
