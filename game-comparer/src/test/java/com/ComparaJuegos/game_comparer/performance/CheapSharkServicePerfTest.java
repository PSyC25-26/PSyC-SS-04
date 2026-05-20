package com.ComparaJuegos.game_comparer.performance;

import com.ComparaJuegos.game_comparer.dto.CheapSharkPrecioDTO;
import com.ComparaJuegos.game_comparer.service.CheapSharkService;
import com.github.noconnor.junitperf.JUnitPerfTest;
import com.github.noconnor.junitperf.JUnitPerfTestRequirement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Collections;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @defgroup tests_rendimiento Tests de Rendimiento
 * Unit-level performance test for CheapSharkService.buscarPrecios().
 *
 * Level: unit — RestClient is mocked via deep stubs; no real HTTP calls.
 * Hot spot: two sequential HTTP calls per invocation (game search + deals fetch)
 * plus in-memory string matching in findBestMatch() and filtering by storeID.
 *
 * Fixture note: both restClient.get()...body() calls in buscarPrecios() return the
 * same stubbed list, crafted to satisfy both the games-parsing path ("gameID",
 * "info"/"title") and the deals-parsing path ("storeID", "salePrice", "dealID").
 *
 * Performance specification:
 *   Scope          : CheapSharkService.buscarPrecios() with mocked RestClient
 *   Concurrent users: 20 threads
 *   Workload mix   : game name match with 1 result, 1 Steam deal returned
 *   Time requirement: meanLatency <= 10 ms, >= 80 executions/sec
 */
class CheapSharkServicePerfTest {

    private static final Logger logger = LogManager.getLogger(CheapSharkServicePerfTest.class);

    private CheapSharkService cheapSharkService;
    private RestClient mockRestClient;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        // Fixture por defecto manteniendo a Witcher 3 para el test de rendimiento
        Map<String, Object> fixture = Map.of(
                "gameID", "12345",
                "external", "Witcher 3",
                "info", Map.of("title", "Witcher 3"),
                "storeID", "1",
                "price", "29.99",
                "salePrice", "29.99",
                "dealID", "perf-deal-abc",
                "steamAppID", "292030"
        );
        List<Map<String, Object>> stubList = List.of(fixture);

        mockRestClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        
        // Respuesta por defecto para el paso 1 (lista de juegos)
        when(mockRestClient.get()
                .uri(org.mockito.ArgumentMatchers.contains("/games?title="), anyString())
                .retrieve()
                .body(List.class))
                .thenReturn((List) stubList);

        // Respuesta por defecto para el paso 2 (detalles del juego)
        Map<String, Object> detailsFixture = Map.of(
                "info", Map.of("title", "Witcher 3"),
                "deals", List.of(
                        Map.of("storeID", "1", "price", "29.99", "dealID", "deal-steam-123"),
                        Map.of("storeID", "25", "price", "19.99", "dealID", "deal-epic-456")
                )
        );
        when(mockRestClient.get()
                .uri(org.mockito.ArgumentMatchers.contains("/games?id="), anyString())
                .retrieve()
                .body(Map.class))
                .thenReturn(detailsFixture);

        cheapSharkService = new CheapSharkService(mockRestClient);
        logger.info("CheapSharkServicePerfTest setUp complete — deep-stub RestClient configured");
    }

    @Test
    @JUnitPerfTest(threads = 20, durationMs = 10_000, warmUpMs = 2_000, rampUpPeriodMs = 1_000)
    @JUnitPerfTestRequirement(meanLatency = 10, executionsPerSec = 80, allowedErrorPercentage = 0.1f)
    void buscarPrecios_parsingUnderLoad() {
        PerformanceTestRunner.assertPerformance(() -> {
            var result = cheapSharkService.buscarPrecios("Witcher 3");
            if (result == null) throw new AssertionError("result must not be null");
        });
    }


    @Test
    @SuppressWarnings("unchecked")
    void buscarPrecios_NoGamesFound_ReturnsEmptyDto() {
        // Simulamos que la API no encuentra el juego devolviendo lista vacía
        when(mockRestClient.get()
                .uri(org.mockito.ArgumentMatchers.contains("/games?title="), anyString())
                .retrieve()
                .body(List.class))
                .thenReturn(Collections.emptyList());

        CheapSharkPrecioDTO result = cheapSharkService.buscarPrecios("JuegoFantasma");
        assertNotNull(result);
        assertNull(result.getSteamPrice());
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscarPrecios_NoBestMatchFound_ReturnsEmptyDto() {
        // La API devuelve un juego pero con un título que no tiene nada que ver para que falle findBestMatch
        Map<String, Object> badGame = Map.of("gameID", "999", "external", "Tetris");
        when(mockRestClient.get()
                .uri(org.mockito.ArgumentMatchers.contains("/games?title="), anyString())
                .retrieve()
                .body(List.class))
                .thenReturn(List.of(badGame));

        CheapSharkPrecioDTO result = cheapSharkService.buscarPrecios("Witcher 3");
        assertNotNull(result);
        assertNull(result.getSteamPrice());
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscarPrecios_PassesOfMatch_AndUnparseablePrice() {
        // Creamos una lista de juegos para forzar las Pasadas 2, 3 y 4 de normalización de cadenas (¡pisa los if!)
        Map<String, Object> gamePass2 = Map.of("gameID", "1", "external", "The Witcher 3: Wild Hunt");
        Map<String, Object> gamePass3 = Map.of("gameID", "2", "external", "Witcher");
        Map<String, Object> gamePass4 = Map.of("gameID", "3", "external", "Witcher 3™");

        when(mockRestClient.get()
                .uri(org.mockito.ArgumentMatchers.contains("/games?title="), anyString())
                .retrieve()
                .body(List.class))
                .thenReturn(List.of(gamePass2, gamePass3, gamePass4));

        // Forzamos también que el detalle del juego devuelva un precio corrupto ("GRATIS") para pisar el NumberFormatException
        Map<String, Object> corruptDetails = Map.of(
                "info", Map.of("title", "Witcher 3"),
                "deals", List.of(
                        Map.of("storeID", "1", "price", "GRATIS_TXT", "dealID", "error-deal")
                )
        );
        when(mockRestClient.get()
                .uri(org.mockito.ArgumentMatchers.contains("/games?id="), anyString())
                .retrieve()
                .body(Map.class))
                .thenReturn(corruptDetails);

        CheapSharkPrecioDTO result = cheapSharkService.buscarPrecios("Witcher 3");
        assertNotNull(result);
        assertNull(result.getSteamPrice()); // El precio saltó por el catch numérico e ignoró el registro
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscarPrecios_DetailsNull_ReturnsDto() {
        // Simulamos que el paso 2 devuelve un null completo
        when(mockRestClient.get()
                .uri(org.mockito.ArgumentMatchers.contains("/games?id="), anyString())
                .retrieve()
                .body(Map.class))
                .thenReturn(null);

        CheapSharkPrecioDTO result = cheapSharkService.buscarPrecios("Witcher 3");
        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscarPrecios_GlobalExceptionCatch_PaintsCatchGreen() {
        // Forzamos un desplome absoluto lanzando una excepción al invocar al RestClient
        when(mockRestClient.get()
                .uri(anyString(), any(Object[].class)))
                .thenThrow(new RuntimeException("API fuera de servicio de prueba"));

        // Al saltar el catch general de buscarPrecios, guardará el log y devolverá el DTO vacío en verde
        CheapSharkPrecioDTO result = cheapSharkService.buscarPrecios("Witcher 3");
        assertNotNull(result);
        assertNull(result.getSteamPrice());
    }
}
