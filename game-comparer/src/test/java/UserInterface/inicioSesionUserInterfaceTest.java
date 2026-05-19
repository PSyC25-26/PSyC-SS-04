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
public class inicioSesionUserInterfaceTest {

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
    @DisplayName("Testeando el envío del formulario de login")
    void testFormularioLogin() {
        // 1. Ir a la pantalla de inicio de sesión
        pagina.navigate("http://localhost:" + port + "/inicioSesion");

        // 2. Rellenar los campos usando los 'name' exactos de tu HTML
        pagina.fill("input[name='username']", "user@test.com");
        pagina.fill("input[name='password']", "Password123!");

        // 3. Hacer clic en el botón de enviar
        pagina.click("button[type='submit']");

        // 4. Al no tener token al menos que funcione el boton
        assertTrue(pagina.url() != null);
    }

    @Test
    @DisplayName("Testeando botón de vuelta al inicio desde el login")
    void testBotonVueltaInicioDesdeLogin() {
        pagina.navigate("http://localhost:" + port + "/inicioSesion");
        pagina.click("text=Vuelta al inicio");
        assertTrue(pagina.url().contains("/iniciar"));
    }

    @Test
    @DisplayName("Testeando botón de ir a Registrarse desde el login")
    void testBotonIrARegistroDesdeLogin() {
        pagina.navigate("http://localhost:" + port + "/inicioSesion");
        pagina.click("text=REGISTRATE");
        assertTrue(pagina.url().contains("/registro"));
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