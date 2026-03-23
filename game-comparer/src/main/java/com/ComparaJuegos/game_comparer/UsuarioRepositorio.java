package com.ComparaJuegos.game_comparer;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ComparaJuegos.game_comparer.models.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long>{
    
}
