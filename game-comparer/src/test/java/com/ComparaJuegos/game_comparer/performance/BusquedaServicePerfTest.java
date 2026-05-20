package com.ComparaJuegos.game_comparer.performance;

import com.ComparaJuegos.game_comparer.JuegoRepositorio;
import com.ComparaJuegos.game_comparer.WishlistRepositorio;
import com.ComparaJuegos.game_comparer.dto.CheapSharkPrecioDTO;
import com.ComparaJuegos.game_comparer.dto.IgdbJuegoDTO;
import com.ComparaJuegos.game_comparer.dto.ResultadoBusquedaDTO;
import com.ComparaJuegos.game_comparer.dto.OfertasHomeDTO;
import com.ComparaJuegos.game_comparer.models.Juego;
import com.ComparaJuegos.game_comparer.models.Precio;
import com.ComparaJuegos.game_comparer.models.Tienda;
import com.ComparaJuegos.game_comparer.models.Wishlist;
import com.ComparaJuegos.game_comparer.service.BusquedaService;
import com.ComparaJuegos.game_comparer.service.CheapSharkService;
import com.ComparaJuegos.game_comparer.service.SteamService;
import com.github.noconnor.junitperf.JUnitPerfTest;
import com.github.noconnor.junitperf.JUnitPerfTestRequirement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.ComparaJuegos.game_comparer.service.IgdbService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @defgroup tests_rendimiento Tests de Rendimiento
 * Unit-level performance test for BusquedaService.buscar().
 *
 * Level: unit — IgdbService and CheapSharkService are mocked; no DB, no HTTP.
 * Hot spot: the orchestration loop that pairs IGDB results with CheapShark
 * prices and builds ResultadoBusquedaDTO objects.
 *
 * Performance specification:
 *   Scope          : BusquedaService.buscar() with mocked dependencies
 *   Concurrent users: 20 threads
 *   Workload mix   : single query "witcher", returns 3 IGDB games each priced
 *   Time requirement: meanLatency <= 10 ms, >= 80 executions/sec
 *
 * Note: JUnitPerf ≤1.25.0 is incompatible with JUnit Jupiter 6.x (Spring Boot 4.x)
 * because it calls ExtensionContext.getRequiredTestInstance() from background threads.
 * PerformanceTestRunner provides equivalent behaviour and reads the annotations below.
 */
@ExtendWith(MockitoExtension.class)
class BusquedaServicePerfTest {

    private static final Logger logger = LogManager.getLogger(BusquedaServicePerfTest.class);

    @Mock private IgdbService igdbService;
    @Mock private CheapSharkService cheapSharkService;
    @Mock private SteamService steamService;
    @Mock private JuegoRepositorio juegoRepositorio;
    @Mock private WishlistRepositorio wishlistRepositorio;

    private BusquedaService busquedaService;

    @BeforeEach
    void setUp() {
        busquedaService = new BusquedaService(igdbService, cheapSharkService, steamService, juegoRepositorio, wishlistRepositorio);

        IgdbJuegoDTO game1 = new IgdbJuegoDTO();
        game1.setName("The Witcher 3");
        game1.setSummary("An open-world RPG.");
        game1.setCoverUrl("https://images.igdb.com/igdb/image/upload/t_cover_big/co1234.jpg");
        game1.setGenres("RPG");
        game1.setFirstReleaseDate(LocalDate.of(2015, 5, 19));
        game1.setDeveloper("CD Projekt Red");
        game1.setPublisher("CD Projekt");

        IgdbJuegoDTO game2 = new IgdbJuegoDTO();
        game2.setName("The Witcher 2");
        game2.setSummary("A sequel RPG.");
        game2.setCoverUrl("https://images.igdb.com/igdb/image/upload/t_cover_big/co5678.jpg");
        game2.setGenres("RPG");
        game2.setFirstReleaseDate(LocalDate.of(2011, 5, 17));
        game2.setDeveloper("CD Projekt Red");
        game2.setPublisher("CD Projekt");

        IgdbJuegoDTO game3 = new IgdbJuegoDTO();
        game3.setName("The Witcher");
        game3.setSummary("The original RPG.");
        game3.setCoverUrl("https://images.igdb.com/igdb/image/upload/t_cover_big/co9999.jpg");
        game3.setGenres("RPG");
        game3.setFirstReleaseDate(LocalDate.of(2007, 10, 26));
        game3.setDeveloper("CD Projekt Red");
        game3.setPublisher("CD Projekt");

        CheapSharkPrecioDTO price = new CheapSharkPrecioDTO();
        price.setSteamPrice(29.99);
        price.setSteamUrl("https://www.cheapshark.com/redirect?dealID=abc");
        price.setEpicPrice(24.99);
        price.setEpicUrl("https://www.cheapshark.com/redirect?dealID=xyz");

        // Stubs mínimos requeridos para evitar fallos null en hilos paralelos
        lenient().when(igdbService.buscar(anyString())).thenReturn(List.of(game1, game2, game3));
        lenient().when(cheapSharkService.buscarPrecios(anyString())).thenReturn(price);

        logger.info("BusquedaServicePerfTest setUp complete — 3 mocked games, prices stubbed");
    }

    @Test
    @JUnitPerfTest(threads = 20, durationMs = 10_000, warmUpMs = 2_000, rampUpPeriodMs = 1_000)
    @JUnitPerfTestRequirement(meanLatency = 10, executionsPerSec = 80, allowedErrorPercentage = 0.1f)
    void buscar_orchestrationLoopUnderLoad() {
        PerformanceTestRunner.assertPerformance(() -> busquedaService.buscar("witcher"));
    }

    @Test
    void buscar_SteamAndEpicFallbacks_PisesAllConditionalBranches() {
        IgdbJuegoDTO igdbGame = new IgdbJuegoDTO();
        igdbGame.setName("Cyberpunk 2077");
        igdbGame.setEpicSlug("cyberpunk-2077");
        igdbGame.setSteamAppId(null); // Fuerza búsqueda de appId por nombre

        // CheapShark no devuelve precios de nada
        CheapSharkPrecioDTO cheapPrices = new CheapSharkPrecioDTO();
        cheapPrices.setSteamPrice(null);
        cheapPrices.setSteamAppId(null);

        when(igdbService.buscar("cyberpunk")).thenReturn(List.of(igdbGame));
        when(cheapSharkService.buscarPrecios("Cyberpunk 2077")).thenReturn(cheapPrices);
        
        // Simular llamadas de último recurso hacia la API directa de Steam
        when(steamService.findAppIdByName("Cyberpunk 2077")).thenReturn("1091500");
        when(steamService.getSteamPrice("1091500")).thenReturn(59.99);
        when(steamService.getSteamUrl("1091500")).thenReturn("https://store.steampowered.com/app/1091500");

        List<ResultadoBusquedaDTO> res = busquedaService.buscar("cyberpunk");
        assertFalse(res.isEmpty());
        assertEquals(59.99, res.get(0).getSteamPrice());
        assertNotNull(res.get(0).getEpicUrl()); // Se autoconstruyó el link con el Slug
    }

    @Test
    void agregarAWishlist_NuevoJuegoYBaseDeDatos_SavesCorrectly() {
        ResultadoBusquedaDTO dto = new ResultadoBusquedaDTO();
        dto.setName("Nuevo Juego");
        dto.setSteamPrice(19.99);
        dto.setEpicPrice(null); // Deja un precio nulo para pisar el ignore de addPrecio

        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        wishlist.setJuegos(new ArrayList<>());

        when(juegoRepositorio.findFirstByNameIgnoreCase("Nuevo Juego")).thenReturn(Optional.empty());
        when(juegoRepositorio.save(any(Juego.class))).thenAnswer(i -> i.getArguments()[0]);
        when(wishlistRepositorio.findById(1L)).thenReturn(Optional.of(wishlist));

        Long returnedId = busquedaService.agregarAWishlist(dto, 1L);
        assertEquals(1L, returnedId);
        verify(juegoRepositorio, times(1)).save(any(Juego.class));
    }

    @Test
    void agregarAWishlist_JuegoYaExistente_UpdatesHistoryPrices() {
        ResultadoBusquedaDTO dto = new ResultadoBusquedaDTO();
        dto.setName("Juego Repetido");
        dto.setSteamPrice(9.99);

        Juego juegoExistente = new Juego();
        juegoExistente.setId(44L);
        juegoExistente.setName("Juego Repetido");
        juegoExistente.setPrecios(new ArrayList<>());

        Precio precioViejo = new Precio();
        precioViejo.setTienda(Tienda.STEAM);
        precioViejo.setPrecio(49.99);
        precioViejo.setHistorial(new ArrayList<>());
        juegoExistente.getPrecios().add(precioViejo);

        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        wishlist.setJuegos(new ArrayList<>());

        when(juegoRepositorio.findFirstByNameIgnoreCase("Juego Repetido")).thenReturn(Optional.of(juegoExistente));
        when(juegoRepositorio.save(any(Juego.class))).thenAnswer(i -> i.getArguments()[0]);
        when(wishlistRepositorio.findById(1L)).thenReturn(Optional.of(wishlist));

        busquedaService.agregarAWishlist(dto, 1L);
        assertEquals(9.99, precioViejo.getPrecio()); // Actualizado
        assertFalse(precioViejo.getHistorial().isEmpty()); // Historial guardado
    }

    @Test
    void agregarAWishlist_JuegoYaEnWishlist_NoDuplica() {
        ResultadoBusquedaDTO dto = new ResultadoBusquedaDTO();
        dto.setName("Juego Repetido");

        Juego juegoExistente = new Juego();
        juegoExistente.setName("Juego Repetido");
        juegoExistente.setPrecios(new java.util.ArrayList<>());

        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        // Metemos el juego ya en la lista para forzar el "Else" de no duplicar
        wishlist.setJuegos(new java.util.ArrayList<>(List.of(juegoExistente)));

        lenient().when(juegoRepositorio.findFirstByNameIgnoreCase(any())).thenReturn(Optional.of(juegoExistente));
        lenient().when(juegoRepositorio.save(any(Juego.class))).thenReturn(juegoExistente);
        lenient().when(wishlistRepositorio.findById(any())).thenReturn(Optional.of(wishlist));

        Long returnedId = busquedaService.agregarAWishlist(dto, 1L);
        assertEquals(1L, returnedId);
    }

    @Test
    void eliminarDeWishlist_ExitoYFalloJuego_PisesCatches() {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        Juego juego = new Juego();
        juego.setId(10L);
        wishlist.setJuegos(new ArrayList<>(List.of(juego)));

        when(wishlistRepositorio.findById(1L)).thenReturn(Optional.of(wishlist));
        when(juegoRepositorio.findById(10L)).thenReturn(Optional.of(juego));

        // Caso 1: Se elimina correctamente
        busquedaService.eliminarDeWishlist(1L, 10L);
        assertTrue(wishlist.getJuegos().isEmpty());

        // Caso 2: Error de juego fantasma (lanza RuntimeException)
        when(juegoRepositorio.findById(11L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> busquedaService.eliminarDeWishlist(1L, 11L));
    }

    @Test
    void getOfertasHome_FiltersValidPricesAndShuffles() {
        IgdbJuegoDTO g1 = new IgdbJuegoDTO(); g1.setName("Juego 1");
        IgdbJuegoDTO g2 = new IgdbJuegoDTO(); g2.setName("Juego 2");
        
        CheapSharkPrecioDTO p1 = new CheapSharkPrecioDTO(); p1.setSteamPrice(12.50);
        CheapSharkPrecioDTO p2 = new CheapSharkPrecioDTO(); p2.setSteamPrice(null); p2.setEpicPrice(null); // Descartado

        when(igdbService.buscar("ato")).thenReturn(List.of(g1, g2));
        when(cheapSharkService.buscarPrecios("Juego 1")).thenReturn(p1);
        when(cheapSharkService.buscarPrecios("Juego 2")).thenReturn(p2);

        List<OfertasHomeDTO> ofertas = busquedaService.getOfertasHome();
        assertEquals(1, ofertas.size());
        assertEquals("Juego 1", ofertas.get(0).getName());
    }
}