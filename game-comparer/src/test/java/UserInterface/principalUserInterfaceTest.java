/**
 * @file principalUserInterfaceTest.java
 * @author Equipo Caza Ofertas Gaming (COG)
 * * Pruebas de interfaz de usuario (UI) para la pantalla principal o de inicio.
 * Se utiliza Playwright para automatizar el navegador y simular clicks.
 * Se cubren los flujos de redirección hacia el registro y el inicio de sesión.
 */

package UserInterface;

import com.ComparaJuegos.game_comparer.GameComparerApplication;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Clase de pruebas de interfaz para la pantalla inicial de la aplicación.
 * Configura el entorno de Spring Boot y el ciclo de vida de Playwright.
 */
@SpringBootTest(
        classes = GameComparerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class principalUserInterfaceTest {

    @LocalServerPort
    private int port;
    private static Playwright playwright;
    private static Browser navegador;
    private BrowserContext contexto;
    private Page pagina;

    /**
     * Define e inicializa el navegador de Playwright.
     * Si se ejecuta en GitHub Actions, se lanza en modo headless (sin interfaz gráfica).
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
     * Crea un nuevo contexto y una pestaña limpia en el navegador
     * antes de ejecutar cada test.
     */
    @BeforeEach
    void crearContexto() {
        contexto=navegador.newContext();
        pagina=contexto.newPage();
    }

    /**
     * Prueba el botón de registro de la pantalla inicial.
     * Verifica que al pulsar en "REGISTRATE" se redirige correctamente a /registro.
     */
    @Test
    @DisplayName("Testeando el botón del registro")
    void testBotonRegistro() {
        pagina.navigate("http://localhost:" + port + "/iniciar");
        pagina.click("text=REGISTRATE");
        assertTrue(pagina.url().contains("/registro"));
    }

    /**
     * Prueba el botón de inicio de sesión de la pantalla inicial.
     * Verifica que al pulsar en "INICIA SESIÓN" se redirige correctamente a /inicioSesion.
     */
    @Test
    @DisplayName("Testeando botón de inicio de sesión")
    void testBotonInicioSesion() {
        pagina.navigate("http://localhost:" + port + "/iniciar");
        pagina.click("text=INICIA SESIÓN");
        assertTrue(pagina.url().contains("/inicioSesion"));
    }

    /**
     * Cierra el contexto del navegador y limpia la sesión al finalizar cada test.
     */
    @AfterEach
    void cerrarContexto() {
        contexto.close();
    }

    /**
     * Cierra por completo el navegador y destruye la instancia de Playwright
     * al terminar todos los tests de la clase.
     */
    @AfterAll
    static void cerrarNavegador() {
        navegador.close();
        playwright.close();
    }
}