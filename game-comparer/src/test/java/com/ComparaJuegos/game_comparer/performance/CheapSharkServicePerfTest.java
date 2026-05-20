package com.ComparaJuegos.game_comparer.performance;

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

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        Map<String, Object> fixture = Map.of(
                "gameID", "12345",
                "info", Map.of("title", "Witcher 3"),
                "storeID", "1",
                "salePrice", "29.99",
                "dealID", "perf-deal-abc"
        );
        List<Map<String, Object>> stubList = List.of(fixture);

        RestClient mockRestClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(mockRestClient.get()
                .uri(anyString(), any(Object[].class))
                .retrieve()
                .body(List.class))
                .thenReturn((List) stubList);

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
}
