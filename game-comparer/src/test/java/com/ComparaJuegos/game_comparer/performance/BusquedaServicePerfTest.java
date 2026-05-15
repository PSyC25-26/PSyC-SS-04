package com.ComparaJuegos.game_comparer.performance;

import com.ComparaJuegos.game_comparer.JuegoRepositorio;
import com.ComparaJuegos.game_comparer.WishlistRepositorio;
import com.ComparaJuegos.game_comparer.dto.CheapSharkPrecioDTO;
import com.ComparaJuegos.game_comparer.dto.IgdbJuegoDTO;
import com.ComparaJuegos.game_comparer.service.BusquedaService;
import com.ComparaJuegos.game_comparer.service.CheapSharkService;
import com.ComparaJuegos.game_comparer.service.IgdbService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
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
    @Mock private JuegoRepositorio juegoRepositorio;
    @Mock private WishlistRepositorio wishlistRepositorio;

    private BusquedaService busquedaService;

    @BeforeEach
    void setUp() {
        busquedaService = new BusquedaService(igdbService, cheapSharkService, juegoRepositorio, wishlistRepositorio);

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

        when(igdbService.buscar(anyString())).thenReturn(List.of(game1, game2, game3));
        when(cheapSharkService.buscarPrecios(anyString())).thenReturn(price);

        logger.info("BusquedaServicePerfTest setUp complete — 3 mocked games, prices stubbed");
    }

    @Test
    @JUnitPerfTest(threads = 20, durationMs = 10_000, warmUpMs = 2_000, rampUpPeriodMs = 1_000)
    @JUnitPerfTestRequirement(meanLatency = 10, executionsPerSec = 80, allowedErrorPercentage = 0.1f)
    void buscar_orchestrationLoopUnderLoad() {
        PerformanceTestRunner.assertPerformance(() -> busquedaService.buscar("witcher"));
    }
}
