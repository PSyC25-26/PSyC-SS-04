package com.ComparaJuegos.game_comparer;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;

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

    @Test
    void testRegistrarUsuario(){
        Usuario usuario1 = new Usuario();
        usuario1.setContrasena("12345");

        when(passwordEncoder.encode(usuario1.getContrasena())).thenReturn("ContraEncriptada");

        String resultado = controlador.registrarUsuario(usuario1);

        assertEquals("redirect:/inicioSesion", resultado);
        assertEquals("ContraEncriptada", usuario1.getContrasena());
        
        verify(passwordEncoder).encode("12345");
        verify(usuarioRepositorio).save(usuario1);
    }

    @Test
    void testCrearPropiaWishlist(){
        Usuario usuarioAct = new Usuario();
        usuarioAct.setId(1L);

        Usuario usuarioBD = new Usuario();
        usuarioBD.setId(1L);
        usuarioBD.setWishlists(new ArrayList<>());

        when(usuarioRepositorio.findById(usuarioAct.getId())).thenReturn(Optional.of(usuarioBD));

        String resultado = controlador.crearPropiaWishlist(usuarioAct, "MisDeseados");

        assertEquals("redirect:/perfil", resultado);

        assertEquals(1, usuarioBD.getWishlists().size());
        assertEquals("MisDeseados", usuarioBD.getWishlists().get(0).getNombre());

        verify(usuarioRepositorio).save(usuarioBD);
        
    }

    @Test
    void testCrearPropialista_UsuarioNoExist(){
        Usuario usuarioAct = new Usuario();
        usuarioAct.setId(1L);

        Usuario usuarioBD = new Usuario();
        usuarioBD.setId(2L);

        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.empty());

        String resultado = controlador.crearPropiaWishlist(usuarioAct, "MisDeseadosImp");

        assertEquals("redirect:/perfil", resultado);

        verify(usuarioRepositorio, never()).save(any());
    }
}
