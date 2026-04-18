package com.ComparaJuegos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;

import com.ComparaJuegos.game_comparer.UsuarioRepositorio;
import com.ComparaJuegos.game_comparer.controladores.controladorSesiones;
import com.ComparaJuegos.game_comparer.models.Usuario;

@ExtendWith(MockitoExtension.class)
class controladorSesionesTest {

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Model modelo;

    @InjectMocks
    private controladorSesiones controlador;

    @Test
    void testInicio(){
        String vista = controlador.inicio();
        assertEquals("principal", vista);
    }

    @Test
    void testIniciarSesion(){
        String iniciarSes = controlador.IniciarSesion();
        assertEquals("inicioSesion", iniciarSes);
    }

    @Test
    void testRegistro(){
        Usuario usuario1 = new Usuario();

        String registrarse = controlador.registrarse(modelo);

        verify(modelo).addAttribute("usuario", usuario1);

        assertEquals("registro", registrarse);
    }

    @Test
    void testPruebaLogin(){
        String pagPrue = controlador.paginaDePrueba();
        assertEquals("prueba_login", pagPrue);
    }
}
