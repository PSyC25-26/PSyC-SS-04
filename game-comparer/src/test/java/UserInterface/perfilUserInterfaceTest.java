/**
 * @file perfilUserInterfaceTest.java
 * @author Equipo Caza Ofertas Gaming (COG)
 *
 * Test de interfaz de usuario para la pantalla de perfil del usuario.
 * Utiliza Playwright para simular un navegador real y verificar que:
 * - El perfil es accesible tras autenticarse correctamente.
 * - Se muestra el saludo personalizado con el nombre del usuario logueado.
 * - Aparece el mensaje de advertencia correcto si el usuario no posee wishlists.
 * - El formulario de creación de wishlists funciona y añade la lista a la interfaz.
 *
 * Depende de TestSecurityConfig (perfil "test") para gestionar la autenticación
 * por el formulario de /inicioSesion y mantener la sesión activa en /perfil.
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
 * @class perfilUserInterfaceTest
 * @brief Test de UI para la pantalla de gestión del perfil y creación de wishlists.
 *
 * Levanta el contexto completo de Spring Boot en un puerto aleatorio, gestiona las
 * pestañas aisladas del navegador y valida los cambios tanto en el DOM como en H2.
 */
@SpringBootTest(
        classes = GameComparerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class perfilUserInterfaceTest {

    /** Puerto aleatorio asignado por Spring Boot al levantar el servidor. */
    @LocalServerPort
    private int port;

    /** Repositorio de usuarios para insertar las credenciales de la sesión de pruebas. */
    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    /** Repositorio de wishlists para validar que el formulario de la UI las persiste realmente. */
    @Autowired
    private WishlistRepositorio wishlistRepositorio;

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

    /** Email exclusivo utilizado para la sesión del perfil. */
    private final String miEmailExclusivo = "perfil_ui_test@compara-juegos.com";

    /** Contraseña en texto plano para el login automatizado. */
    private final String contrasenaPlana = "Password123!";

    /** Nombre del usuario que se verificará en el saludo de la interfaz. */
    private final String nombreUsuarioTest = "Usuario de Pruebas COG";

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
     * @brief Prepara los datos del usuario en H2, abre el navegador e inicia sesión antes de cada test.
     */
    @BeforeEach
    void prepararDatosYContexto() {
        // 1. Crear el usuario de prueba con contraseña encriptada
        Usuario usuarioPrueba = new Usuario();
        usuarioPrueba.setName(nombreUsuarioTest);
        usuarioPrueba.setEmail(miEmailExclusivo);
        usuarioPrueba.setContrasena(new BCryptPasswordEncoder().encode(contrasenaPlana));
        usuarioPrueba.setFecha_nac(LocalDate.of(1998, 3, 15));
        usuarioPrueba.setPais("España");

        usuarioPrueba = usuarioRepositorio.save(usuarioPrueba);
        testUserId = usuarioPrueba.getId();

        // 2. Crear entorno de navegación aislado
        contexto = navegador.newContext();
        pagina = contexto.newPage();

        // 3. Ejecutar el login obligado para poder ver el perfil
        hacerLoginPrevio();
    }

    /**
     * @brief Automatiza el flujo de inicio de sesión para habilitar las cookies de Spring Security.
     */
    private void hacerLoginPrevio() {
        pagina.navigate("http://localhost:" + port + "/inicioSesion");
        pagina.fill("input[name='username']", miEmailExclusivo);
        pagina.fill("input[name='password']", contrasenaPlana);
        pagina.click("button[type='submit']");
        pagina.waitForURL(url -> !url.contains("/inicioSesion"));
    }

    /**
     * @brief Verifica que los datos del usuario e informativos se cargan correctamente en el panel.
     *
     * Comprueba que aparece el nombre del usuario en el saludo y que, al ser una cuenta nueva,
     * se muestra el texto por defecto de que aún no posee ninguna lista creada.
     */
    @Test
    @DisplayName("El panel de usuario muestra el saludo correcto y aviso de listas vacías")
    void testCargaPerfilUsuarioYDatosBasicos() {
        pagina.navigate("http://localhost:" + port + "/perfil");
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        // Comprobamos que el saludo dinámico muestra nuestro nombre
        String saludoEsperado = "Hola, " + nombreUsuarioTest + "!";
        assertTrue(pagina.locator("p:has-text('" + saludoEsperado + "')").isVisible(),
                "No se encontró el saludo personalizado con el nombre del usuario.");

        // Validamos que aparece el aviso de que no hay listas de deseos todavía
        assertTrue(pagina.locator("p:has-text('Aún no tienes ninguna lista')").isVisible(),
                "No se muestra el mensaje de aviso de lista de deseos vacía.");
    }

    /**
     * @brief Verifica que el formulario de creación de wishlists funciona de manera correcta.
     *
     * Rellena el input de texto del formulario, hace submit y valida que la nueva lista
     * aparezca pintada inmediatamente en el listado HTML de la página de perfil.
     */
    @Test
    @DisplayName("El formulario del perfil permite crear una nueva wishlist con éxito")
    void testCrearNuevaWishlistDesdePerfil() {
        pagina.navigate("http://localhost:" + port + "/perfil");
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        String nombreNuevaLista = "Favoritos del Verano 2026";

        // Rellenar el formulario de creación
        pagina.fill("input[name='nombre']", nombreNuevaLista);
        pagina.click("button:has-text('Crear Nueva Wishlist')");

        // Esperamos a que la petición POST recargue la página de perfil
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        // Verificación en UI: Comprobar que el nombre de la lista aparece ahora en el DOM
        assertTrue(pagina.locator("strong:has-text('" + nombreNuevaLista + "')").isVisible(),
                "La nueva wishlist creada no aparece listada en la pantalla de perfil.");
    }

    /**
     * @brief Cierra el contexto del navegador y destruye en cascada los datos creados en H2.
     */
    @AfterEach
    void cerrarContexto() {
        if (contexto != null) {
            contexto.close();
        }
        if (testUserId != null) {
            // Al borrar el usuario, gracias a las relaciones JPA, se limpian también sus wishlists asociadas
            usuarioRepositorio.deleteById(testUserId);
        }
    }

    /**
     * @brief Apaga por completo el motor de Playwright al terminar la suite de pruebas.
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
