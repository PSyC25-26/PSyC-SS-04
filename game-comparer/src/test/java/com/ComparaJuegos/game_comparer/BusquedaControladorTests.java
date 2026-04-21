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

		@Test//Revisar
		void testAgreagarAWishlist(){

			ResultadoBusquedaDTO dto_resul = new ResultadoBusquedaDTO();
			long id_wishlist = 1L;//Para que conozca al 1 como Long, le ponemos una L despues

			when(busquedaService.agregarAWishlist(dto_resul, id_wishlist)).thenReturn(99L);

			String resultado = controlador.agregar(dto_resul, id_wishlist);

			assertEquals("redirect:/wishlist/99", resultado);

			verify(busquedaService).agregarAWishlist(dto_resul, id_wishlist);
		}

		@Test//Cuando acierta lo que se busca NO es null
		void testBuscarJuegos(){
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

		@Test //Ahora cuando lo que se busca ES null
		void busquedaNull(){
			//Si te tiene que pasar algo
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

		@Test //No existe el juego
		void BusquedaDeJuegoNoExistente(){
			String q = "heldorse";//Juego que buscamos y que se que o existe

			when(detallesUsuario.getUsername()).thenReturn("userTestNull@gmail.com");

			Usuario usuario = new Usuario();

			when(usuarioRepositorio.findByEmail("userTestNull@gmail.com")).thenReturn(Optional.empty());
			
			assertThrows(NoSuchElementException.class, () -> {
				controlador.buscar(q, detallesUsuario, modelo);
			});
		};

		@Test//Test de, la wishlist existe
		void EntrarWishlist(){

			Usuario usuario = new Usuario();
			usuario.setId(1L);

			Wishlist listaDeseos = new Wishlist();
			listaDeseos.setId(10L);
			//Hay que crear 2 usuarios separados para que funcione bien
			Usuario usuarioDos = new Usuario();
			usuarioDos.setId(1L);

			listaDeseos.setUsuario(usuarioDos);

			when(detallesUsuario.getUsername()).thenReturn("UsuarioWishlist@gmail.com");

			when(usuarioRepositorio.findByEmail("UsuarioWishlist@gmail.com")).thenReturn(Optional.of(usuario));
			when(wishlistRepositorio.findById(10L)).thenReturn(Optional.of(listaDeseos));

			String vista = controlador.verWishlist(10L, detallesUsuario, modelo);

			assertEquals("detalle-wishlist", vista);
			verify(modelo).addAttribute("wishlist", listaDeseos);

		};

		//Si no existe error
		@Test//Acceso denegado
		void ImposibleEntrarWishlist(){

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

			String vista = controlador.verWishlist(10L, detallesUsuario, modelo);

			assertEquals("redirect:/buscar", vista);
			verify(modelo, never()).addAttribute(eq("wishlist"), any());//Si devuelve algo es que tienes acceso

		};
	}
