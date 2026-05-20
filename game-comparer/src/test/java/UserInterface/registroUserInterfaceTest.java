/**
 * @file registroUserInterfaceTest.java
 * @author Equipo Caza Ofertas Gaming (COG)
 *
 * Test de interfaz de usuario para la pantalla de registro de usuarios.
 * Utiliza Playwright para simular un navegador real y verificar que:
 * - El formulario de registro procesa y da de alta cuentas con datos válidos.
 * - El usuario creado se persiste correctamente en la base de datos (H2).
 * - El enlace de vuelta al inicio redirige correctamente.
 * - El botón de redirección rápida a inicio de sesión funciona correctamente.
 *
 * Depende de TestSecurityConfig (perfil "test") que permite la libre navegación
 * por los endpoints de alta y habilita el formulario de login para las vistas posteriores.
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
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @class registroUserInterfaceTest
 * @brief Test de UI para la pantalla de registro de nuevos usuarios.
 *
 * Levanta el contexto completo de Spring Boot en un puerto aleatorio
 * y lanza un navegador Chromium real mediante Playwright para simular
 * la cumplimentación de formularios y flujos de navegación en el registro.
 */
@SpringBootTest(
        classes = GameComparerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class registroUserInterfaceTest {

    /** Puerto aleatorio asignado por Spring Boot al levantar el servidor. */
    @LocalServerPort
    private int port;

    /** Repositorio de usuarios para verificar la persistencia y realizar la limpieza. */
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

    /** Email dinámico generado por cada test para evitar colisiones por restricciones unique en BD. */
    private String emailRegistroTest;

    /** Nombre de prueba utilizado para el alta del formulario. */
    private final String nombreTest = "Usuario Test UI";

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
     * @brief Prepara un contexto de navegación limpio y genera un email aleatorio antes de cada test.
     *
     * Utiliza un sufijo basado en milisegundos para garantizar que ejecuciones consecutivas
     * locales no causen excepciones de clave duplicada en la base de datos.
     */
    @BeforeEach
    void crearContexto() {
        emailRegistroTest = "registro_ui_" + System.currentTimeMillis() + "@test.com";
        contexto = navegador.newContext();
        pagina = contexto.newPage();
    }

    /**
     * @brief Verifica que el formulario de registro responde correctamente al introducir datos válidos.
     *
     * Simula la cumplimentación del formulario HTML campo a campo, realiza el submit y verifica
     * que la información se halla guardada con éxito en el UsuarioRepositorio (H2) antes de
     * comprobar la redirección final a la pantalla de login.
     */
    @Test
    @DisplayName("Testeando el envío del formulario de registro con datos válidos")
    void testFormularioRegistroCompleto() {
        pagina.navigate("http://localhost:" + port + "/registro");
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        pagina.fill("input[name='name']", nombreTest);
        pagina.fill("input[name='email']", emailRegistroTest);
        pagina.fill("input[name='contrasena']", "Password123!");
        pagina.fill("input[name='fecha_nac']", "2000-01-01");
        pagina.fill("input[name='pais']", "España");

        // Identifica de forma elástica el botón de submit o recurre a la tecla Enter
        Locator botonRegistro = pagina.locator("button[type='submit'], input[type='submit']").first();
        if (botonRegistro.isVisible()) {
            botonRegistro.click();
        } else {
            pagina.press("input[name='email']", "Enter");
        }

        // Espera a que el servidor procese la lógica interna del controlador y redirija
        pagina.waitForURL(url -> url.contains("/iniciar") || url.contains("/inicioSesion"));

        // Verificación de UI: Comprobar el destino de la redirección post-registro
        assertTrue(pagina.url().contains("/iniciar") || pagina.url().contains("/inicioSesion"),
                "El registro no redirigió a la vista de login esperada. URL actual: " + pagina.url());

        // Verificación de persistencia: Comprobar que el registro existe físicamente en el repositorio
        Optional<Usuario> usuarioGuardado = usuarioRepositorio.findByEmail(emailRegistroTest);
        assertTrue(usuarioGuardado.isPresent(), "El usuario registrado mediante la UI no se persistió en la BD.");
    }

    /**
     * @brief Verifica que el enlace de retorno al inicio desde la pantalla de registro funciona correctamente.
     *
     * Pulsa el elemento de texto correspondiente y valida que el navegador apunte a la ruta base /iniciar.
     */
    @Test
    @DisplayName("Testeando botón de vuelta al inicio desde el registro")
    void testBotonVueltaInicio() {
        pagina.navigate("http://localhost:" + port + "/registro");
        pagina.click("text=Vuelta al inicio");
        assertTrue(pagina.url().contains("/iniciar"),
                "El enlace no redirigió a /iniciar. URL actual: " + pagina.url());
    }

    /**
     * @brief Verifica que el botón de acceso rápido a inicio de sesión redirige correctamente.
     *
     * Acciona el enlace "INICIAR SESION" y valida el cambio de flujo hacia /inicioSesion.
     */
    @Test
    @DisplayName("Testeando botón de ir a Iniciar Sesión desde el registro")
    void testBotonIrAInicioSesion() {
        pagina.navigate("http://localhost:" + port + "/registro");
        pagina.click("text=INICIAR SESION");
        assertTrue(pagina.url().contains("/inicioSesion"),
                "El botón no redirigió a /inicioSesion. URL actual: " + pagina.url());
    }

    /**
     * @brief Cierra el contexto de navegación y elimina de forma segura el usuario temporal de la BD.
     *
     * Se ejecuta tras cada test de forma asíncrona para impedir la acumulación de datos basura
     * y salvaguardar la pureza de subsiguientes pruebas unitarias o de integración.
     */
    @AfterEach
    void cerrarContexto() {
        if (contexto != null) {
            contexto.close();
        }
        if (emailRegistroTest != null) {
            usuarioRepositorio.findByEmail(emailRegistroTest).ifPresent(usuario -> {
                usuarioRepositorio.deleteById(usuario.getId());
            });
        }
    }

    /**
     * @brief Cierra el navegador y la instancia de Playwright al finalizar todos los tests.
     *
     * Clausura de manera estricta los subprocesos abiertos de Chromium para liberar
     * la memoria del sistema operativo del servidor.
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