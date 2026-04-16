package com.ComparaJuegos.game_comparer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ComparaJuegos.game_comparer.controladores.BusquedaControlador;
import com.ComparaJuegos.game_comparer.dto.ResultadoBusquedaDTO;
import com.ComparaJuegos.game_comparer.service.BusquedaService;


    @ExtendWith(MockitoExtension.class)
	class TestBusquedaContr{

		@Mock
		private BusquedaService busquedaService;

		@Mock
		private UsuarioRepositorio usuarioRepositorio;

		@Mock
		private WishlistRepositorio wishlistRepositorio;

		@InjectMocks
		private BusquedaControlador controlador;

		@Test
		void agreagarAWishlist(){

			ResultadoBusquedaDTO dto_resul = new ResultadoBusquedaDTO();
			long id_wishlist = 1L;

			when(busquedaService.agregarAWishlist(dto_resul, id_wishlist)).thenReturn(99L);

			String resultado = controlador.agregar(dto_resul, id_wishlist);

			assertEquals("redirect:/wishlist/99", resultado);

			verify(busquedaService).agregarAWishlist(dto_resul, id_wishlist);
		}
	}
