/**
 * @file TestSecurityConfig.java
 * @author Equipo Caza Ofertas Gaming (COG)
 * 
 * Configuración de seguridad específica para el perfil "test".
 * Desactiva completamente la seguridad para facilitar las pruebas de integración:
 * - Permite todas las peticiones sin autenticación.
 * - Deshabilita CSRF, formulario de login y logout.
 * - Proporciona un PasswordEncoder (BCrypt) para que los controladores que lo requieran
 *   puedan funcionar sin errores.
 */

package com.ComparaJuegos.game_comparer.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para el perfil "test".
 * Reemplaza a SeguridadConfig (que solo se activa con @Profile("!test")).
 */
@Configuration
@EnableWebSecurity
@Profile("test")
public class TestSecurityConfig {

    /**
     * Define la cadena de filtros de seguridad.
     * - Permite todas las peticiones (anyRequest().permitAll()).
     * - Desactiva protección CSRF.
     * - Deshabilita el formulario de login y logout.
     * 
     * @param http objeto HttpSecurity para configurar la seguridad.
     * @return SecurityFilterChain configurado.
     * @throws Exception si ocurre algún error en la configuración.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());
        return http.build();
    }

    /**
     * Proporciona un codificador de contraseñas (BCrypt) para el perfil test.
     * Es necesario porque el controlador controladorSesiones inyecta
     * PasswordEncoder.
     * 
     * @return instancia de BCryptPasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}