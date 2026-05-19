/**
 * @file BusquedaControladorTests.java
 * @author Equipo Caza Ofertas Gaming (COG)
 * 
 * Pruebas unitarias para el controlador de búsqueda (BusquedaControlador).
 * Se utiliza Mockito para simular los servicios y repositorios.
 * Se cubren los casos de éxito y error de los métodos agregar, eliminar,
 * buscar y ver wishlist.
 */

package com.ComparaJuegos.game_comparer;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;

import com.ComparaJuegos.game_comparer.controladores.BusquedaControlador;
import com.ComparaJuegos.game_comparer.dto.ResultadoBusquedaDTO;
import com.ComparaJuegos.game_comparer.models.Usuario;
import com.ComparaJuegos.game_comparer.models.Wishlist;
import com.ComparaJuegos.game_comparer.service.BusquedaService;

/**
 * Clase de pruebas unitarias para BusquedaControlador.
 * Se simulan las dependencias con Mockito.
 */
@ExtendWith(MockitoExtension.class)
class BusquedaControladorTests {

	@Mock
	private BusquedaService busquedaService;

	@Mock
	private UsuarioRepositorio usuarioRepositorio;

	@Mock
	private WishlistRepositorio wishlistRepositorio;

	@Mock
	private UserDetails detallesUsuario;

	@Mock
	private Model modelo;

	@InjectMocks
	private BusquedaControlador controlador;

	/**
	 * Prueba el método agregarAWishlist.
	 * Verifica que devuelve la redirección correcta y que se llama al servicio.
	 */
	@Test
	void testAgreagarAWishlist() {

		ResultadoBusquedaDTO dto_resul = new ResultadoBusquedaDTO();
		long id_wishlist = 1L;

		when(busquedaService.agregarAWishlist(dto_resul, id_wishlist)).thenReturn(99L);

		String resultado = controlador.agregar(dto_resul, id_wishlist);

		assertEquals("redirect:/wishlist/99", resultado);
		verify(busquedaService).agregarAWishlist(dto_resul, id_wishlist);
	}

	/**
	 * Prueba el método eliminarDeWishlist.
	 * Verifica que redirige correctamente y que se invoca al servicio.
	 */
	@Test
	void testEliminarWishlist() {
		long wishlistId = 1L;
		long juegoId = 2L;

		String resultado = controlador.eliminar(wishlistId, juegoId);

		assertEquals("redirect:/wishlist/1", resultado);
		verify(busquedaService).eliminarDeWishlist(wishlistId, juegoId);
	}

	/**
	 * Prueba el método buscar cuando se encuentra el juego.
	 * Se simula que el usuario existe y que la búsqueda devuelve resultados.
	 */
	@Test
	void testBuscarJuegos() {
		String id = "Battlefield";

		when(detallesUsuario.getUsername()).thenReturn("userTest@gmail.com");

		Usuario usuario = new Usuario();
		when(usuarioRepositorio.findByEmail("userTest@gmail.com")).thenReturn(Optional.of(usuario));

		List<ResultadoBusquedaDTO> resultadosTest = List.of(new ResultadoBusquedaDTO());
		when(busquedaService.buscar(id)).thenReturn(resultadosTest);
		when(wishlistRepositorio.findByUsuario(usuario)).thenReturn(List.of());

		String vista = controlador.buscar(id, detallesUsuario, modelo);

		verify(busquedaService).buscar(id);
		verify(modelo).addAttribute("q", id);
		verify(modelo).addAttribute("resultados", resultadosTest);
		assertEquals("buscar", vista);
	}

	/**
	 * Prueba el método buscar cuando el parámetro de búsqueda es null.
	 * No debe llamarse al servicio de búsqueda.
	 */
	@Test
	void busquedaNull() {
		when(detallesUsuario.getUsername()).thenReturn("usuarioTest@gmail.com");

		Usuario usuario = new Usuario();
		when(usuarioRepositorio.findByEmail("usuarioTest@gmail.com")).thenReturn(Optional.of(usuario));
		when(wishlistRepositorio.findByUsuario(usuario)).thenReturn(List.of());

		String vista = controlador.buscar(null, detallesUsuario, modelo);

		verify(busquedaService, never()).buscar(any());
		verify(modelo).addAttribute("q", null);
		verify(modelo).addAttribute(eq("resultados"), anyList());
		verify(modelo).addAttribute(eq("wishlists"), anyList());
		assertEquals("buscar", vista);
	}

	/**
	 * Prueba el método buscar cuando el usuario no existe en la base de datos.
	 * Debe lanzar NoSuchElementException.
	 */
	@Test
	void BusquedaDeJuegoNoExistente() {
		String q = "heldorse";

		when(detallesUsuario.getUsername()).thenReturn("userTestNull@gmail.com");
		when(usuarioRepositorio.findByEmail("userTestNull@gmail.com")).thenReturn(Optional.empty());

		assertThrows(NoSuchElementException.class, () -> {
			controlador.buscar(q, detallesUsuario, modelo);
		});
	}

	/**
	 * Prueba el acceso a una wishlist existente y perteneciente al usuario.
	 * Debe devolver la vista "detalle-wishlist".
	 */
	@Test
	void EntrarWishlist() {
		Usuario usuario = new Usuario();
		usuario.setId(1L);

		Wishlist listaDeseos = new Wishlist();
		listaDeseos.setId(10L);
		Usuario usuarioDos = new Usuario();
		usuarioDos.setId(1L);
		listaDeseos.setUsuario(usuarioDos);

		when(detallesUsuario.getUsername()).thenReturn("UsuarioWishlist@gmail.com");
		when(usuarioRepositorio.findByEmail("UsuarioWishlist@gmail.com")).thenReturn(Optional.of(usuario));
		when(wishlistRepositorio.findById(10L)).thenReturn(Optional.of(listaDeseos));

		String vista = controlador.verWishlist(10L, detallesUsuario, null, modelo);

		assertEquals("detalle-wishlist", vista);
		verify(modelo).addAttribute("wishlist", listaDeseos);
	}

	/**
	 * Prueba el acceso denegado a una wishlist que pertenece a otro usuario.
	 * Debe redirigir a "/buscar".
	 */
	@Test
	void ImposibleEntrarWishlist() {
		Usuario usuario = new Usuario();
		usuario.setId(1L);

		Usuario usuarioDueño = new Usuario();
		usuarioDueño.setId(6L);

		Wishlist listaDeseos = new Wishlist();
		listaDeseos.setId(10L);
		listaDeseos.setUsuario(usuarioDueño);

		when(detallesUsuario.getUsername()).thenReturn("UsuarioWishlist@gmail.com");
		when(usuarioRepositorio.findByEmail(any())).thenReturn(Optional.of(usuario));
		when(wishlistRepositorio.findById(10L)).thenReturn(Optional.of(listaDeseos));

		String vista = controlador.verWishlist(10L, detallesUsuario, null, modelo);

		assertEquals("redirect:/buscar", vista);
		verify(modelo, never()).addAttribute(eq("wishlist"), any());
	}
}