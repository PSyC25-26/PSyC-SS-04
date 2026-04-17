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
import com.ComparaJuegos.game_comparer.service.BusquedaService;


    @ExtendWith(MockitoExtension.class)
	class TestBusquedaContr{

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
			long id_wishlist = 1L;

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
		}
	}
