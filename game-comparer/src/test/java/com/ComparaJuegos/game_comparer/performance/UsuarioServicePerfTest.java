package com.ComparaJuegos.game_comparer.performance;

import com.ComparaJuegos.game_comparer.UsuarioRepositorio;
import com.ComparaJuegos.game_comparer.models.Rol;
import com.ComparaJuegos.game_comparer.models.Usuario;
import com.ComparaJuegos.game_comparer.service.UsuarioService;
import com.github.noconnor.junitperf.JUnitPerfTest;
import com.github.noconnor.junitperf.JUnitPerfTestRequirement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.Mockito.when;

/**
 * Unit-level performance test for UsuarioService.loadUserByUsername().
 *
 * Level: unit — UsuarioRepositorio is mocked; no DB.
 * Hot spot: called on every authenticated HTTP request by Spring Security.
 * Measures the cost of a repository lookup (mocked) + User.builder() chain
 * + role assignment under concurrent load.
 *
 * Performance specification:
 *   Scope          : UsuarioService.loadUserByUsername() with mocked repository
 *   Concurrent users: 30 threads (simulates simultaneous authenticated requests)
 *   Workload mix   : 100% successful lookups (user always found)
 *   Time requirement: meanLatency <= 5 ms, >= 150 executions/sec
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServicePerfTest {

    private static final Logger logger = LogManager.getLogger(UsuarioServicePerfTest.class);

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService();
        ReflectionTestUtils.setField(usuarioService, "repositorio", usuarioRepositorio);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("perf@test.com");
        usuario.setContrasena("$2a$10$hashedpassword");
        usuario.setRol(Rol.USER);

        when(usuarioRepositorio.findByEmail("perf@test.com")).thenReturn(Optional.of(usuario));
        logger.info("UsuarioServicePerfTest setUp complete — user stub configured");
    }

    @Test
    @JUnitPerfTest(threads = 30, durationMs = 10_000, warmUpMs = 2_000, rampUpPeriodMs = 1_000)
    @JUnitPerfTestRequirement(meanLatency = 5, executionsPerSec = 150, allowedErrorPercentage = 0.1f)
    void loadUserByUsername_authPathUnderLoad() {
        PerformanceTestRunner.assertPerformance(() -> {
            var details = usuarioService.loadUserByUsername("perf@test.com");
            if (!"perf@test.com".equals(details.getUsername())) {
                throw new AssertionError("Unexpected username: " + details.getUsername());
            }
        });
    }
}
