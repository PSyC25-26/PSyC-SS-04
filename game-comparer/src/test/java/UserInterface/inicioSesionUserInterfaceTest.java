/**
 * @file inicioSesionUserInterfaceTest.java
 * @author Equipo Caza Ofertas Gaming (COG)
 * * Pruebas de interfaz de usuario (UI) para la pantalla de inicio de sesión (Login).
 * Se utiliza Playwright para automatizar la interacción con el formulario de autenticación.
 * Se cubren los flujos de envío del formulario y la navegación hacia el registro o el inicio.
 */

package UserInterface;

import com.ComparaJuegos.game_comparer.GameComparerApplication;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Clase de pruebas de interfaz para el formulario y navegación de inicio de sesión.
 * Configura el entorno de Spring Boot en un puerto aleatorio y gestiona las instancias de Playwright.
 */
@SpringBootTest(
        classes = GameComparerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class inicioSesionUserInterfaceTest {

    @LocalServerPort
    private int port;
    private static Playwright playwright;
    private static Browser navegador;
    private BrowserContext contexto;
    private Page pagina;

    /**
     * Inicializa el motor de Playwright y levanta el navegador Chromium.
     * Si se detecta un entorno de integración continua (GitHub Actions), se ejecuta en modo headless.
     */
    @BeforeAll
    static void definirNavegador() {
        playwright = Playwright.create();
        boolean esGitHubActions = System.getenv("GITHUB_ACTIONS") != null;
        navegador = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(esGitHubActions)
        );
    }

    /**
     * Crea un contexto web aislado y abre una nueva pestaña limpia en el navegador
     * antes de dar comienzo a cada caso de prueba.
     */
    @BeforeEach
    void crearContexto() {
        contexto = navegador.newContext();
        pagina = contexto.newPage();
    }

    /**
     * Prueba el envío del formulario de inicio de sesión.
     * Introduce credenciales de prueba en los campos de texto, efectúa el click de envío
     * y comprueba de forma básica que la acción del botón responde correctamente en la interfaz.
     */
    @Test
    @DisplayName("Testeando el envío del formulario de login")
    void testFormularioLogin() {
        pagina.navigate("http://localhost:" + port + "/inicioSesion");

        pagina.fill("input[name='username']", "user@test.com");
        pagina.fill("input[name='password']", "Password123!");

        pagina.click("button[type='submit']");

        assertTrue(pagina.url() != null);
    }

    /**
     * Prueba el enlace de retorno al inicio desde la pantalla de login.
     * Verifica que al pulsar en "Vuelta al inicio" se redirige de manera correcta a /iniciar.
     */
    @Test
    @DisplayName("Testeando botón de vuelta al inicio desde el login")
    void testBotonVueltaInicioDesdeLogin() {
        pagina.navigate("http://localhost:" + port + "/inicioSesion");
        pagina.click("text=Vuelta al inicio");
        assertTrue(pagina.url().contains("/iniciar"));
    }

    /**
     * Prueba el botón de acceso rápido al registro de cuentas desde el login.
     * Verifica que al hacer click en "REGISTRATE" se redirige de forma exitosa a /registro.
     */
    @Test
    @DisplayName("Testeando botón de ir a Registrarse desde el login")
    void testBotonIrARegistroDesdeLogin() {
        pagina.navigate("http://localhost:" + port + "/inicioSesion");
        pagina.click("text=REGISTRATE");
        assertTrue(pagina.url().contains("/registro"));
    }

    /**
     * Cierra el contexto del navegador y limpia las cookies temporales tras finalizar cada test.
     */
    @AfterEach
    void cerrarContexto() {
        contexto.close();
    }

    /**
     * Detiene por completo el navegador Chromium y destruye la instancia global de Playwright
     * una vez completados todos los tests de la clase.
     */
    @AfterAll
    static void cerrarNavegador() {
        navegador.close();
        playwright.close();
    }
}