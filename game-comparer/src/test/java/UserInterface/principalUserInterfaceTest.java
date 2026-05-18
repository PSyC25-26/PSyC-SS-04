package UserInterface;

import com.ComparaJuegos.game_comparer.GameComparerApplication;
import com.ComparaJuegos.game_comparer.service.IgdbTokenService;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertTrue;
@SpringBootTest(
        classes = GameComparerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {

                "spring.main.allow-bean-definition-overriding=true",


                "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",




                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
public class principalUserInterfaceTest {
    @MockitoBean
    private IgdbTokenService igdbTokenService;
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
        contexto=navegador.newContext();
        pagina=contexto.newPage();
    }

    @Test
    @DisplayName("Testeando el botón del registro")
    void testBotonRegistro() {
        pagina.navigate("http://localhost:" + port + "/iniciar");
        pagina.click("text=REGISTRATE");
        assertTrue(pagina.url().contains("/registro"));
    }

    @Test
    @DisplayName("Testeando botón de inicio de sesión")
    void testBotonInicioSesion() {
        pagina.navigate("http://localhost:" + port + "/iniciar");
        pagina.click("text=INICIA SESIÓN");
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
