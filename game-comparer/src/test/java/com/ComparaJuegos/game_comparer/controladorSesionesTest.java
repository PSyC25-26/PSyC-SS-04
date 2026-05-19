/**
 * @file controladorSesionesTest.java
 * @author Equipo Caza Ofertas Gaming (COG)
 * 
 * Pruebas unitarias para el controlador de sesiones (controladorSesiones).
 * Se utiliza Mockito para simular el repositorio de usuarios, el codificador
 * de contraseñas y el modelo de Spring.
 * Se cubren los casos de éxito y error de los métodos:
 * - incio, inicioSesion, registro, pruebaLogin
 * - registrarUsuario, crearPropiaWishlist, verPerfil
 */

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

/**
 * Clase de pruebas unitarias para controladorSesiones.
 * Se simulan las dependencias con Mockito.
 */
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

    /**
     * Prueba el método inicio().
     * Verifica que devuelve la vista "principal".
     */
    @Test
    void testInicio() {
        String vista = controlador.inicio();
        assertEquals("principal", vista);
    }

    /**
     * Prueba el método IniciarSesion().
     * Verifica que devuelve la vista "inicioSesion".
     */
    @Test
    void testIniciarSesion() {
        String iniciarSes = controlador.IniciarSesion();
        assertEquals("inicioSesion", iniciarSes);
    }

    /**
     * Prueba el método registrarse().
     * Comprueba que se añade un nuevo usuario al modelo y que la vista es
     * "registro".
     */
    @Test
    void testRegistro() {
        Usuario usuario1 = new Usuario();

        String registrarse = controlador.registrarse(modelo);

        verify(modelo).addAttribute("usuario", usuario1);
        assertEquals("registro", registrarse);
    }

    /**
     * Prueba el método paginaDePrueba().
     * Verifica que devuelve la vista "prueba_login".
     */
    @Test
    void testPruebaLogin() {
        String pagPrue = controlador.paginaDePrueba();
        assertEquals("prueba_login", pagPrue);
    }

    /**
     * Prueba el método registrarUsuario().
     * Simula el registro de un usuario, encriptando la contraseña y guardándolo.
     * Comprueba la redirección y que se llama al repositorio.
     */
    @Test
    void testRegistrarUsuario() {
        Usuario usuario1 = new Usuario();
        usuario1.setContrasena("12345");

        when(passwordEncoder.encode(usuario1.getContrasena())).thenReturn("ContraEncriptada");

        String resultado = controlador.registrarUsuario(usuario1);

        assertEquals("redirect:/inicioSesion", resultado);
        assertEquals("ContraEncriptada", usuario1.getContrasena());

        verify(passwordEncoder).encode("12345");
        verify(usuarioRepositorio).save(usuario1);
    }

    /**
     * Prueba el método crearPropiaWishlist() cuando el usuario existe en BD.
     * Verifica que se crea una wishlist y se asocia correctamente.
     */
    @Test
    void testCrearPropiaWishlist() {
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

    /**
     * Prueba el método crearPropiaWishlist() cuando el usuario NO existe en BD.
     * No debe crearse la wishlist ni guardarse nada.
     */
    @Test
    void testCrearPropialista_UsuarioNoExist() {
        Usuario usuarioAct = new Usuario();
        usuarioAct.setId(1L);

        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.empty());

        String resultado = controlador.crearPropiaWishlist(usuarioAct, "MisDeseadosImp");

        assertEquals("redirect:/perfil", resultado);
        verify(usuarioRepositorio, never()).save(any());
    }

    /**
     * Prueba el método verPerfil() cuando el usuario existe.
     * Comprueba que se añade al modelo y devuelve la vista correcta.
     */
    @Test
    void testVerPerfil() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@mail.com");

        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuario));

        String vista = controlador.verPerfil(modelo, 1L);

        assertEquals("perfil", vista);
        verify(modelo).addAttribute("usuario", usuario);
        verify(usuarioRepositorio).findById(1L);
    }

    /**
     * Prueba el método verPerfil() cuando el usuario NO existe.
     * Debe lanzar una excepción RuntimeException.
     */
    @Test
    void testVerPerfil_usuarioNoExiste() {
        when(usuarioRepositorio.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> controlador.verPerfil(modelo, 999L));
    }
}