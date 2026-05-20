/**
 * @file TestSecurityConfig.java
 * @author Equipo Caza Ofertas Gaming (COG)
 *
 * Configuración de seguridad específica para el perfil "test".
 * Permite todas las peticiones sin autenticación para facilitar las pruebas de integración,
 * pero habilita el formulario de login en /inicioSesion para permitir tests de UI con Playwright.
 * - Permite todas las peticiones (anyRequest().permitAll()).
 * - Deshabilita CSRF y logout.
 * - Habilita el formulario de login en /inicioSesion para tests de UI con Playwright.
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
 * @defgroup test_config Tests de Configuracion
 * @class TestSecurityConfig
 * @brief Configuración de seguridad para el perfil "test".
 *
 * Reemplaza a SeguridadConfig (que solo se activa con @Profile("!test")).
 * Permite todo el tráfico sin autenticación pero habilita el procesamiento
 * del formulario de login para que los tests de UI con Playwright puedan
 * simular sesiones reales.
 */
@Configuration
@EnableWebSecurity
@Profile("test")
public class TestSecurityConfig {

    /**
     * @brief Define la cadena de filtros de seguridad para el perfil test.
     *
     * - Permite todas las peticiones (anyRequest().permitAll()).
     * - Desactiva protección CSRF.
     * - Habilita el formulario de login en /inicioSesion con loginProcessingUrl
     *   para que Playwright pueda autenticarse correctamente.
     * - Deshabilita logout.
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
                .formLogin(form -> form
                        .loginPage("/inicioSesion")
                        .loginProcessingUrl("/inicioSesion")
                        .defaultSuccessUrl("/buscar", true)
                        .permitAll())
                .logout(logout -> logout.disable());
        return http.build();
    }

    /**
     * @brief Proporciona un codificador de contraseñas (BCrypt) para el perfil test.
     *
     * Es necesario porque el controlador controladorSesiones inyecta
     * PasswordEncoder. También es requerido por los tests de UI con Playwright
     * que guardan usuarios con contraseña encriptada en BCrypt.
     *
     * @return instancia de BCryptPasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}