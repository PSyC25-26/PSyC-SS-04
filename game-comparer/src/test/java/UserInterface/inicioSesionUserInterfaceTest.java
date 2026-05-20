/**
 * @file inicioSesionUserInterfaceTest.java
 * @author Equipo Caza Ofertas Gaming (COG)
 *
 * Test de interfaz de usuario para la pantalla de inicio de sesión (Login).
 * Utiliza Playwright para simular un navegador real y verificar que:
 * - El formulario de login autentica correctamente a un usuario real de la BD.
 * - La sesión se establece y el sistema redirige correctamente a /buscar.
 * - El enlace de vuelta al inicio funciona y redirige a /iniciar.
 * - El botón "REGISTRATE" redirige de forma exitosa a /registro.
 *
 * Depende de TestSecurityConfig (perfil "test") que habilita el formulario
 * de login en /inicioSesion con loginProcessingUrl para procesar la sesión real.
 */

package UserInterface;

import com.ComparaJuegos.game_comparer.GameComparerApplication;
import com.ComparaJuegos.game_comparer.UsuarioRepositorio;
import com.ComparaJuegos.game_comparer.models.Usuario;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @class inicioSesionUserInterfaceTest
 * @brief Test de UI para el formulario de autenticación y navegación de login.
 *
 * Configura el entorno de Spring Boot en un puerto aleatorio, gestiona las
 * instancias del navegador e interactúa con la persistencia real de H2.
 */
@SpringBootTest(
        classes = GameComparerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class inicioSesionUserInterfaceTest {

    /** Puerto aleatorio asignado por Spring Boot al levantar el servidor. */
    @LocalServerPort
    private int port;

    /** Repositorio de usuarios para dar de alta las credenciales de prueba y limpiar la BD. */
    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    /** Instancia de Playwright compartida por todos los tests de la clase. */
    private static Playwright playwright;

    /** Navegador Chromium compartido por todos los tests de la clase. */
    private static Browser navegador;

    /** Contexto de navegación aislado para cada test (cookies, sesión, etc.). */
    private BrowserContext contexto;

    /** Página activa del navegador para cada test. */
    private Page pagina;

    /** ID del usuario de prueba guardado para asegurar su posterior eliminación. */
    private Long testUserId;

    /** Email exclusivo y controlado para el flujo de inicio de sesión. */
    private final String miEmailExclusivo = "login_ui_test@compara-juegos.com";

    /** Contraseña en texto plano que enviará Playwright. Se encripta en BD. */
    private final String contrasenaPlana = "Password123!";

    /**
     * @brief Inicializa Playwright y lanza el navegador Chromium una sola vez para toda la clase.
     *
     * Detecta automáticamente si se está ejecutando en GitHub Actions para adaptar el comportamiento:
     * - En CI (GitHub Actions): modo headless activado y sin retardo entre acciones.
     * - En local: navegador visible con un retardo de 300ms para facilitar la depuración visual.
     */
    @BeforeAll
    static void definirNavegador() {
        playwright = Playwright.create();
        boolean esGitHubActions = System.getenv("GITHUB_ACTIONS") != null;
        navegador = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(esGitHubActions)
                        .setSlowMo(esGitHubActions ? 0 : 300)
        );
    }

    /**
     * @brief Prepara un contexto web aislado e inserta el usuario de pruebas en la base de datos.
     *
     * Carga el usuario con la contraseña codificada en BCrypt para que los filtros de
     * Spring Security procesen la autenticación real del formulario.
     */
    @BeforeEach
    void crearContexto() {
        // Asegurar que el usuario de prueba existe de forma real en H2
        Usuario usuarioPrueba = new Usuario();
        usuarioPrueba.setName("Usuario Test Login");
        usuarioPrueba.setEmail(miEmailExclusivo);
        usuarioPrueba.setContrasena(new BCryptPasswordEncoder().encode(contrasenaPlana));
        usuarioPrueba.setFecha_nac(LocalDate.of(1990, 5, 10));
        usuarioPrueba.setPais("España");

        usuarioPrueba = usuarioRepositorio.save(usuarioPrueba);
        testUserId = usuarioPrueba.getId();

        contexto = navegador.newContext();
        pagina = contexto.newPage();
    }

    /**
     * @brief Prueba el envío del formulario de inicio de sesión con credenciales válidas.
     *
     * Introduce los datos en los inputs 'username' y 'password', procesa el submit
     * del formulario y verifica que Spring Security conceda el acceso redirigiendo a /buscar.
     */
    @Test
    @DisplayName("Testeando el envío del formulario de login")
    void testFormularioLogin() {
        pagina.navigate("http://localhost:" + port + "/inicioSesion");
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        pagina.fill("input[name='username']", miEmailExclusivo);
        pagina.fill("input[name='password']", contrasenaPlana);

        pagina.click("button[type='submit']");

        // Esperamos a que la red procese la sesión y nos eche de la vista de login
        pagina.waitForURL(url -> !url.contains("/inicioSesion"));

        // Verificamos que redirigió con éxito a la URL por defecto tras autenticarse con éxito (/buscar)
        assertTrue(pagina.url().contains("/buscar"),
                "El login falló o no redirigió a /buscar. URL actual: " + pagina.url());
    }

    /**
     * @brief Prueba el enlace de retorno al inicio desde la pantalla de login.
     *
     * Verifica que al pulsar en el texto "Vuelta al inicio" se redirige de manera correcta a /iniciar.
     */
    @Test
    @DisplayName("Testeando botón de vuelta al inicio desde el login")
    void testBotonVueltaInicioDesdeLogin() {
        pagina.navigate("http://localhost:" + port + "/inicioSesion");
        pagina.click("text=Vuelta al inicio");
        assertTrue(pagina.url().contains("/iniciar"),
                "El enlace no redirigió a /iniciar. URL actual: " + pagina.url());
    }

    /**
     * @brief Prueba el botón de acceso rápido al registro de cuentas desde el login.
     *
     * Verifica que al hacer click en el texto descriptivo "REGISTRATE" se redirige a /registro.
     */
    @Test
    @DisplayName("Testeando botón de ir a Registrarse desde el login")
    void testBotonIrARegistroDesdeLogin() {
        pagina.navigate("http://localhost:" + port + "/inicioSesion");
        pagina.click("text=REGISTRATE");
        assertTrue(pagina.url().contains("/registro"),
                "El botón no redirigió a /registro. URL actual: " + pagina.url());
    }

    /**
     * @brief Cierra el contexto del navegador y elimina el usuario de prueba para limpiar el entorno.
     *
     * Libera los recursos del navegador y destruye el registro creado en H2 para evitar
     * efectos colaterales colisionando con otras ejecuciones de pruebas.
     */
    @AfterEach
    void cerrarContexto() {
        if (contexto != null) {
            contexto.close();
        }
        if (testUserId != null) {
            usuarioRepositorio.deleteById(testUserId);
        }
    }

    /**
     * @brief Detiene por completo el navegador Chromium y destruye la instancia global de Playwright.
     *
     * Se ejecuta de forma única tras la conclusión total de la batería de pruebas de esta clase.
     */
    @AfterAll
    static void cerrarNavegador() {
        if (navegador != null) {
            navegador.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}