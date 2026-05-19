/**
 * @file registroUserInterfaceTest.java
 * @author Equipo Caza Ofertas Gaming (COG)
 * * Pruebas de interfaz de usuario (UI) para la pantalla de registro de usuarios.
 * Se utiliza Playwright para automatizar el navegador e interactuar con el formulario.
 * Se cubren los casos de envío con datos válidos y la navegación hacia otras vistas.
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
 * Clase de pruebas de interfaz para el formulario y navegación de registro.
 * Configura el entorno de Spring Boot bajo un puerto aleatorio y gestiona Playwright.
 */
@SpringBootTest(
        classes = GameComparerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class registroUserInterfaceTest {

    @LocalServerPort
    private int port;
    private static Playwright playwright;
    private static Browser navegador;
    private BrowserContext contexto;
    private Page pagina;

    /**
     * Instancia el motor de Playwright y configura el navegador Chromium.
     * En servidores de integración continua como GitHub Actions se activa el modo headless.
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
     * Inicializa un contexto web limpio aislado y abre una nueva pestaña
     * antes de dar comienzo a cada test.
     */
    @BeforeEach
    void crearContexto() {
        contexto = navegador.newContext();
        pagina = contexto.newPage();
    }

    /**
     * Prueba el flujo completo de registro introduciendo datos válidos.
     * Simula la cumplimentación del formulario HTML campo a campo, realiza el submit
     * y verifica que el sistema procesa el alta redirigiendo a la pantalla correspondiente.
     */
    @Test
    @DisplayName("Testeando el envío del formulario de registro con datos válidos")
    void testFormularioRegistroCompleto() {
        pagina.navigate("http://localhost:" + port + "/registro");

        pagina.fill("input[name='name']", "Usuario Test UI");
        pagina.fill("input[name='email']", "playwright_test@test.com");
        pagina.fill("input[name='contrasena']", "Password123!");
        pagina.fill("input[name='fecha_nac']", "2000-01-01");
        pagina.fill("input[name='pais']", "España");

        pagina.click("input[type='submit']");

        assertTrue(pagina.url().contains("/iniciar") || pagina.url().contains("/inicioSesion"));
    }

    /**
     * Prueba el enlace de retorno al inicio desde la pantalla de registro.
     * Verifica que al pulsar en "Vuelta al inicio" se redirige correctamente a /iniciar.
     */
    @Test
    @DisplayName("Testeando botón de vuelta al inicio desde el registro")
    void testBotonVueltaInicio() {
        pagina.navigate("http://localhost:" + port + "/registro");
        pagina.click("text=Vuelta al inicio");
        assertTrue(pagina.url().contains("/iniciar"));
    }

    /**
     * Prueba el botón de redirección rápida a la vista de login.
     * Verifica que al pulsar en "INICIAR SESION" se envía al usuario a /inicioSesion.
     */
    @Test
    @DisplayName("Testeando botón de ir a Iniciar Sesión desde el registro")
    void testBotonIrAInicioSesion() {
        pagina.navigate("http://localhost:" + port + "/registro");
        pagina.click("text=INICIAR SESION");
        assertTrue(pagina.url().contains("/inicioSesion"));
    }

    /**
     * Limpia el contexto de navegación y cierra las sesiones temporales al acabar cada test.
     */
    @AfterEach
    void cerrarContexto() {
        contexto.close();
    }

    /**
     * Detiene por completo la ejecución del navegador y libera los recursos del sistema
     * asignados a Playwright tras terminar todos los tests de la clase.
     */
    @AfterAll
    static void cerrarNavegador() {
        navegador.close();
        playwright.close();
    }
}