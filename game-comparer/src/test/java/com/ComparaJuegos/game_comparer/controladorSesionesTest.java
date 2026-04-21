package com.ComparaJuegos.game_comparer;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
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

        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.empty());

        String resultado = controlador.crearPropiaWishlist(usuarioAct, "MisDeseadosImp");

        assertEquals("redirect:/perfil", resultado);

        verify(usuarioRepositorio, never()).save(any());
    }

    @Test
    void testVerPerfil() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@mail.com");

        UserDetails userDetails = org.mockito.Mockito.mock(UserDetails.class);

        when(userDetails.getUsername()).thenReturn("test@mail.com");
        when(usuarioRepositorio.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(usuario));

        String vista = controlador.verPerfil(modelo, userDetails);

        assertEquals("perfil", vista);

        verify(modelo).addAttribute("usuario", usuario);
        verify(usuarioRepositorio).findByEmail("test@mail.com");
    }

    @Test
    void testVerPerfil_usuarioNoExiste() {
        UserDetails userDetails = org.mockito.Mockito.mock(UserDetails.class);

        when(userDetails.getUsername()).thenReturn("noexiste@mail.com");
        when(usuarioRepositorio.findByEmail("noexiste@mail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                java.util.NoSuchElementException.class,
                () -> controlador.verPerfil(modelo, userDetails)
        );
    }

}
