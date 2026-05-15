package com.ComparaJuegos.game_comparer.performance;

import com.ComparaJuegos.game_comparer.service.IgdbTokenService;
import com.github.noconnor.junitperf.JUnitPerfTest;
import com.github.noconnor.junitperf.JUnitPerfTestRequirement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

/**
 * Unit-level performance test for IgdbTokenService.getToken() — cache-hit path.
 *
 * Level: unit — no HTTP calls; the token cache is pre-populated via ReflectionTestUtils.
 * Hot spot: the synchronized block. Under concurrent load every thread acquires
 * the intrinsic lock; this test measures contention cost on the cache-hit fast path.
 *
 * Performance specification:
 *   Scope          : IgdbTokenService.getToken() cache-hit (no network I/O)
 *   Concurrent users: 50 threads (simulates burst of concurrent API requests)
 *   Workload mix   : 100% cache-hit reads
 *   Time requirement: meanLatency <= 5 ms, >= 200 executions/sec
 */
class IgdbTokenServicePerfTest {

    private static final Logger logger = LogManager.getLogger(IgdbTokenServicePerfTest.class);

    private IgdbTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new IgdbTokenService();
        ReflectionTestUtils.setField(tokenService, "cachedToken", "perf-test-token");
        ReflectionTestUtils.setField(tokenService, "tokenExpiresAt", Instant.now().plusSeconds(3600));
        logger.info("IgdbTokenServicePerfTest setUp complete — cache pre-populated");
    }

    @Test
    @JUnitPerfTest(threads = 50, durationMs = 10_000, warmUpMs = 2_000, rampUpPeriodMs = 1_000)
    @JUnitPerfTestRequirement(meanLatency = 5, executionsPerSec = 200, allowedErrorPercentage = 0.1f)
    void getToken_cacheHitUnderConcurrentLoad() {
        PerformanceTestRunner.assertPerformance(() -> {
            String token = tokenService.getToken();
            if (!"perf-test-token".equals(token)) {
                throw new AssertionError("Unexpected token: " + token);
            }
        });
    }
}
