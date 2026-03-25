package com.ComparaJuegos.game_comparer.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SeguridadConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/registro", "/inicioSesion", "/iniciar", "/css/**", "/estilos.css").permitAll()// Sin sesion puede acceder a registro y login
                        .anyRequest().authenticated()//Sin sesion al resto de paginas no puede acceder
                )
                .formLogin(form -> form
                        .loginPage("/inicioSesion") //Escucha en el iniciosesion mediante el form implementado ahi
                        .defaultSuccessUrl("/prueba_login", true) // A donde vas al iniciar sesion
                        .permitAll()
                )
                .logout(logout -> logout.permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
