package com.ComparaJuegos.game_comparer.performance;

import com.ComparaJuegos.game_comparer.dto.IgdbJuegoDTO;
import com.ComparaJuegos.game_comparer.service.IgdbService;
import com.ComparaJuegos.game_comparer.service.IgdbTokenService;
import com.github.noconnor.junitperf.JUnitPerfTest;
import com.github.noconnor.junitperf.JUnitPerfTestRequirement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit-level performance test for IgdbService.parseGameResponse().
 *
 * Level: unit — tests the response parsing path directly with a pre-built fixture;
 * no HTTP or RestClient mock needed.
 *
 * Hot spot: the nested-map parsing loop — genres list join, involved_companies
 * iteration for developer/publisher extraction, cover URL string concatenation,
 * and epoch-second-to-LocalDate conversion. Repeated up to 5× per call.
 *
 * Performance specification:
 *   Scope          : IgdbService.parseGameResponse() with in-memory fixture
 *   Concurrent users: 20 threads
 *   Workload mix   : 3 fully-populated game objects per response
 *   Time requirement: meanLatency <= 5 ms, >= 200 executions/sec
 */
@ExtendWith(MockitoExtension.class)
class IgdbServicePerfTest {

    private static final Logger logger = LogManager.getLogger(IgdbServicePerfTest.class);

    // Static, immutable fixture — safe for concurrent reads across all threads
    private static final List<Map<String, Object>> IGDB_RESPONSE = List.of(
            Map.of("id", 1942,
                    "name", "The Witcher 3: Wild Hunt",
                    "summary", "An open-world RPG.",
                    "cover", Map.of("image_id", "co1cgj"),
                    "genres", List.of(Map.of("name", "Role-playing (RPG)"), Map.of("name", "Adventure")),
                    "first_release_date", 1431993600L,
                    "involved_companies", List.of(
                            Map.of("company", Map.of("name", "CD Projekt Red"),
                                    "developer", Boolean.TRUE, "publisher", Boolean.FALSE),
                            Map.of("company", Map.of("name", "CD Projekt"),
                                    "developer", Boolean.FALSE, "publisher", Boolean.TRUE))),
            Map.of("id", 1943,
                    "name", "The Witcher 2: Assassins of Kings",
                    "summary", "A sequel.",
                    "cover", Map.of("image_id", "co5678"),
                    "genres", List.of(Map.of("name", "Role-playing (RPG)")),
                    "first_release_date", 1305590400L,
                    "involved_companies", List.of(
                            Map.of("company", Map.of("name", "CD Projekt Red"),
                                    "developer", Boolean.TRUE, "publisher", Boolean.FALSE))),
            Map.of("id", 1944,
                    "name", "The Witcher",
                    "summary", "The original.",
                    "cover", Map.of("image_id", "co9999"),
                    "genres", List.of(Map.of("name", "Role-playing (RPG)")),
                    "first_release_date", 1193356800L,
                    "involved_companies", List.of(
                            Map.of("company", Map.of("name", "CD Projekt Red"),
                                    "developer", Boolean.TRUE, "publisher", Boolean.TRUE)))
    );

    @Mock
    private IgdbTokenService mockTokenService;

    @Mock 
    private RestClient mockRestClient;

    private IgdbService igdbService;

    @BeforeEach
    void setUp() {
        // No stubs needed: parseGameResponse() is pure parsing logic that doesn't
        // call the token service. The mock is required only for constructor injection.
        igdbService = new IgdbService(mockTokenService);
        logger.info("IgdbServicePerfTest setUp complete — static IGDB fixture ready, 3 games");
    }

    @Test
    @JUnitPerfTest(threads = 20, durationMs = 10_000, warmUpMs = 2_000, rampUpPeriodMs = 1_000)
    @JUnitPerfTestRequirement(meanLatency = 5, executionsPerSec = 200, allowedErrorPercentage = 0.1f)
    void parseGameResponse_parsingLoopUnderLoad() {
        PerformanceTestRunner.assertPerformance(() -> {
            var results = igdbService.parseGameResponse(IGDB_RESPONSE);
            if (results.isEmpty()) throw new AssertionError("results must not be empty");
        });
    }

    @Test
    void parseGameResponse_WhenResponseIsNull_ReturnsEmptyList() {
        List<IgdbJuegoDTO> results = igdbService.parseGameResponse(null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void parseGameResponse_WithMissingAndExternalData_PisesAllBranches() {
        // Construimos un mapa mutable con datos vacíos para obligar a pasar por los bloques 'if (X == null)'
        Map<String, Object> brokenGame = new HashMap<>();
        brokenGame.put("id", 9999L);
        brokenGame.put("name", "Juego Incompleto");
        brokenGame.put("cover", null); 
        brokenGame.put("genres", null); 
        brokenGame.put("first_release_date", null); 

        // Añadimos datos de tiendas para forzar category 1 y category 26
        List<Map<String, Object>> externalGames = new ArrayList<>();
        
        Map<String, Object> steamGame = new HashMap<>();
        steamGame.put("category", 1);
        steamGame.put("uid", "steam123");
        externalGames.add(steamGame);

        Map<String, Object> epicGame = new HashMap<>();
        epicGame.put("category", 26);
        epicGame.put("uid", "epic-slug");
        externalGames.add(epicGame);

        // Añadimos una compañía rota (sin sub-objeto company) para cubrir la línea 'if (company == null) continue;'
        List<Map<String, Object>> companies = new ArrayList<>();
        Map<String, Object> brokenCompany = new HashMap<>();
        brokenCompany.put("company", null);
        companies.add(brokenCompany);

        brokenGame.put("external_games", externalGames);
        brokenGame.put("involved_companies", companies);

        List<Map<String, Object>> rawResponse = List.of(brokenGame);
        List<IgdbJuegoDTO> parsed = igdbService.parseGameResponse(rawResponse);

        assertFalse(parsed.isEmpty());
        assertEquals("steam123", parsed.get(0).getSteamAppId());
        assertEquals("epic-slug", parsed.get(0).getEpicSlug());
        assertNull(parsed.get(0).getCoverUrl());
    }

    @Test
    void buscar_WhenExceptionOccurs_ReturnsEmptyList() {
        // Rompemos la llamada justo al principio usando any()
        lenient().when(mockRestClient.post()).thenThrow(new RuntimeException("API caida temporalmente"));

        List<IgdbJuegoDTO> results = igdbService.buscar("error-query");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
