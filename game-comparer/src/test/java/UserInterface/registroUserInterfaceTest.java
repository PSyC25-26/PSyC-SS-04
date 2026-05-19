package UserInterface;

import com.ComparaJuegos.game_comparer.GameComparerApplication;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @BeforeAll
    static void definirNavegador() {
        playwright = Playwright.create();
        boolean esGitHubActions = System.getenv("GITHUB_ACTIONS") != null;
        navegador = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(esGitHubActions)
        );
    }

    @BeforeEach
    void crearContexto() {
        contexto = navegador.newContext();
        pagina = contexto.newPage();
    }

    @Test
    @DisplayName("Testeando el envío del formulario de registro con datos válidos")
    void testFormularioRegistroCompleto() {
        // 1. Ir a la pantalla de registro
        pagina.navigate("http://localhost:" + port + "/registro");

        // 2. Rellenar los inputs usando los names exactos de tu HTML
        pagina.fill("input[name='name']", "Usuario Test UI");
        pagina.fill("input[name='email']", "playwright_test@test.com");
        pagina.fill("input[name='contrasena']", "Password123!");
        pagina.fill("input[name='fecha_nac']", "2000-01-01"); // Formato estándar de fecha
        pagina.fill("input[name='pais']", "España");

        // 3. Hacer clic en el botón submit "Registrarse"
        pagina.click("input[type='submit']");

        // 4. Verificar que redirige (Spring suele redirigir tras un registro con éxito, por ejemplo al login o inicio)
        // Modifica el "/iniciar" si vuestro controlador redirige a otra ruta tras registrarse
        assertTrue(pagina.url().contains("/iniciar") || pagina.url().contains("/inicioSesion"));
    }

    @Test
    @DisplayName("Testeando botón de vuelta al inicio desde el registro")
    void testBotonVueltaInicio() {
        pagina.navigate("http://localhost:" + port + "/registro");
        pagina.click("text=Vuelta al inicio");
        assertTrue(pagina.url().contains("/iniciar"));
    }

    @Test
    @DisplayName("Testeando botón de ir a Iniciar Sesión desde el registro")
    void testBotonIrAInicioSesion() {
        pagina.navigate("http://localhost:" + port + "/registro");
        pagina.click("text=INICIAR SESION");
        assertTrue(pagina.url().contains("/inicioSesion"));
    }

    @AfterEach
    void cerrarContexto() {
        contexto.close();
    }

    @AfterAll
    static void cerrarNavegador() {
        navegador.close();
        playwright.close();
    }
}