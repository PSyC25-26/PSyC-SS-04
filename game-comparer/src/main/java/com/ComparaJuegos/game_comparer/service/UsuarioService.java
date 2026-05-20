package com.ComparaJuegos.game_comparer.service;

import com.ComparaJuegos.game_comparer.UsuarioRepositorio;
import com.ComparaJuegos.game_comparer.models.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @brief Servicio de autenticación de usuarios para Spring Security.
 *
 * Implementa {@link UserDetailsService} para que Spring Security pueda
 * cargar los datos de un usuario durante el proceso de login.
 * El identificador de autenticación es el email del usuario.
 */
@Service
public class UsuarioService implements UserDetailsService {
    @Autowired
    private UsuarioRepositorio repositorio;

    /**
     * @brief Carga un usuario por su email para autenticación.
     *
     * Busca el usuario en base de datos por email y construye un objeto
     * {@link UserDetails} con sus credenciales y rol. Si el usuario no
     * tiene rol asignado, se le otorga el rol por defecto {@code USER}.
     *
     * @param email Email del usuario que intenta autenticarse.
     * @return {@link UserDetails} con las credenciales y roles del usuario.
     * @throws UsernameNotFoundException Si no existe ningún usuario con ese email.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = repositorio.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));
        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getContrasena())
                .roles(usuario.getRol() != null ? usuario.getRol().name() : "USER")
                .build();
    }
}
