/**
 * @file perfilUserInterfaceTest.java
 * @author Equipo Caza Ofertas Gaming (COG)
 * * Pruebas de interfaz de usuario (UI) para la pantalla de perfil del usuario.
 * Se utiliza Playwright para automatizar el navegador y simular el inicio de sesión.
 * Se cubren los flujos de visualización de datos y la creación de nuevas wishlists.
 */

package UserInterface;

import com.ComparaJuegos.game_comparer.GameComparerApplication;
import com.ComparaJuegos.game_comparer.UsuarioRepositorio;
import com.ComparaJuegos.game_comparer.WishlistRepositorio;
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
 * Clase de pruebas de interfaz para la pantalla de perfil de usuario.
 * Configura el entorno de Spring Boot, los repositorios de datos y el ciclo de vida de Playwright.
 */
@SpringBootTest(
        classes = GameComparerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class perfilUserInterfaceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private WishlistRepositorio wishlistRepositorio;

    private static Playwright playwright;
    private static Browser navegador;
    private BrowserContext contexto;
    private Page pagina;

    private Long testUserId;
    private final String miEmailExclusivo = "perfil_ui_test@compara-juegos.com";
    private final String contrasenaPlana = "Password123!";
    private final String nombreUsuarioTest = "Usuario de Pruebas COG";

    /**
     * Define e inicializa el navegador de Playwright.
     * Si se ejecuta en GitHub Actions, se lanza en modo headless (sin interfaz gráfica).
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
     * Crea un usuario de prueba en la base de datos, inicializa un nuevo contexto
     * en el navegador y realiza el inicio de sesión previo.
     */
    @BeforeEach
    void prepararDatosYContexto() {
        Usuario usuarioPrueba = new Usuario();
        usuarioPrueba.setName(nombreUsuarioTest);
        usuarioPrueba.setEmail(miEmailExclusivo);
        usuarioPrueba.setContrasena(new BCryptPasswordEncoder().encode(contrasenaPlana));
        usuarioPrueba.setFecha_nac(LocalDate.of(1998, 3, 15));
        usuarioPrueba.setPais("España");

        usuarioPrueba = usuarioRepositorio.save(usuarioPrueba);
        testUserId = usuarioPrueba.getId();

        contexto = navegador.newContext();
        pagina = contexto.newPage();

        hacerLoginPrevio();
    }

    /**
     * Automatiza el inicio de sesión rellenando el formulario.
     * Segunda estrategia: Hace click y espera a que la red esté completamente inactiva (NETWORKIDLE),
     * asegurando que la redirección a /perfil haya finalizado antes de proceder.
     */
    private void hacerLoginPrevio() {
        pagina.navigate("http://localhost:" + port + "/inicioSesion");
        pagina.waitForLoadState(LoadState.DOMCONTENTLOADED);

        pagina.fill("input[name='username']", miEmailExclusivo);
        pagina.fill("input[name='password']", contrasenaPlana);

        // Hacemos el click directamente
        pagina.click("button[type='submit']");

        // En lugar de interceptar la URL, esperamos a que la red deje de enviar peticiones (Login completado)
        pagina.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * Prueba la carga básica del perfil de usuario.
     * Verifica que se muestre el saludo personalizado y el mensaje de listas vacías.
     */
    @Test
    @DisplayName("El panel de usuario muestra el saludo correcto y aviso de listas vacías")
    void testCargaPerfilUsuarioYDatosBasicos() {
        pagina.navigate("http://localhost:" + port + "/perfil");
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        String saludoEsperado = "Hola, " + nombreUsuarioTest + "!";

        // Esperamos explícitamente a que el elemento sea visible antes de lanzar el assert
        pagina.locator("p:has-text('" + saludoEsperado + "')").waitFor();
        assertTrue(pagina.locator("p:has-text('" + saludoEsperado + "')").isVisible(),
                "No se encontró el saludo personalizado con el nombre del usuario.");

        assertTrue(pagina.locator("p:has-text('Aún no tienes ninguna lista')").isVisible(),
                "No se muestra el mensaje de aviso de lista de deseos vacía.");
    }

    /**
     * Prueba el formulario de creación de listas de deseos desde el perfil.
     * Verifica que al rellenar el nombre y enviar, la nueva wishlist aparece en la pantalla.
     */
    @Test
    @DisplayName("El formulario del perfil permite crear una nueva wishlist con éxito")
    void testCrearNuevaWishlistDesdePerfil() {
        pagina.navigate("http://localhost:" + port + "/perfil");
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        String nombreNuevaLista = "Favoritos del Verano 2026";

        pagina.fill("input[name='nombre']", nombreNuevaLista);

        // Hacemos click en el botón de crear y esperamos a que el estado de la red se estabilice
        pagina.click("button:has-text('Crear Nueva Wishlist')");
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        // Esperamos a que aparezca la etiqueta strong en el DOM antes del assert
        pagina.locator("strong:has-text('" + nombreNuevaLista + "')").waitFor();
        assertTrue(pagina.locator("strong:has-text('" + nombreNuevaLista + "')").isVisible(),
                "La nueva wishlist creada no aparece listada en la pantalla de perfil.");
    }

    /**
     * Cierra el contexto del navegador y limpia el usuario de prueba de la base de datos.
     */
    @AfterEach
    void cerrarContexto() {
        if (contexto != null) {
            contexto.close();
        }
        if (testUserId != null) {
            try {
                usuarioRepositorio.deleteById(testUserId);
            } catch (Exception e) {
                // Previene caídas en el AfterEach si la base de datos está bloqueada temporalmente
            }
        }
    }

    /**
     * Cierra por completo el navegador y destruye la instancia de Playwright.
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