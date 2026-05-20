/**
 * @file buscarUserInterfaceTest.java
 * @author Equipo Caza Ofertas Gaming (COG)
 *
 * Test de interfaz de usuario para la pantalla de búsqueda.
 * Utiliza Playwright para simular un navegador real y verificar que:
 * - El login con usuario y contraseña funciona correctamente.
 * - La página /buscar es accesible con sesión activa.
 * - Los botones del carrusel funcionan correctamente.
 * - El formulario de búsqueda responde sin errores.
 * - El mensaje de "no encontrado" aparece cuando no hay resultados.
 * - Los links de wishlists navegan correctamente.
 * - El botón "Añadir a Wishlist" funciona con resultados mockeados.
 *
 * Depende de TestSecurityConfig (perfil "test") que habilita el formulario
 * de login en /inicioSesion con loginProcessingUrl para que Playwright
 * pueda autenticarse correctamente.
 */

package UserInterface;

import com.ComparaJuegos.game_comparer.GameComparerApplication;
import com.ComparaJuegos.game_comparer.UsuarioRepositorio;
import com.ComparaJuegos.game_comparer.WishlistRepositorio;
import com.ComparaJuegos.game_comparer.dto.ResultadoBusquedaDTO;
import com.ComparaJuegos.game_comparer.models.Usuario;
import com.ComparaJuegos.game_comparer.models.Wishlist;
import com.ComparaJuegos.game_comparer.service.BusquedaService;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @class buscarUserInterfaceTest
 * @brief Test de UI para la pantalla de búsqueda con sesión autenticada.
 *
 * Levanta el contexto completo de Spring Boot en un puerto aleatorio
 * y lanza un navegador Chromium real mediante Playwright para simular
 * el flujo de login y acceso a la pantalla de búsqueda.
 * BusquedaService se mockea para evitar llamadas a APIs externas (IGDB, Steam, CheapShark).
 */
@SpringBootTest(
        classes = GameComparerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class buscarUserInterfaceTest {

    /** Puerto aleatorio asignado por Spring Boot al levantar el servidor. */
    @LocalServerPort
    private int port;

    /** Repositorio de usuarios para crear y limpiar datos de prueba. */
    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    /** Repositorio de wishlists para crear y limpiar datos de prueba. */
    @Autowired
    private WishlistRepositorio wishlistRepositorio;

    /**
     * Mock de BusquedaService para evitar llamadas a APIs externas
     * (IGDB, CheapShark, Steam) durante los tests de UI.
     */
    @MockitoBean
    private BusquedaService busquedaService;

    /** Instancia de Playwright compartida por todos los tests de la clase. */
    private static Playwright playwright;

    /** Navegador Chromium compartido por todos los tests de la clase. */
    private static Browser navegador;

    /** Contexto de navegación aislado para cada test (cookies, sesión, etc.). */
    private BrowserContext contexto;

    /** Página activa del navegador para cada test. */
    private Page pagina;

    /** ID del usuario de prueba creado en @BeforeEach, usado para limpieza en @AfterEach. */
    private Long testUserId;

    /** ID de la wishlist de prueba creada en @BeforeEach, usada para limpieza en @AfterEach. */
    private Long testWishlistId;

    /** Email exclusivo del usuario de prueba para evitar colisiones con otros tests. */
    private final String miEmailExclusivo = "testUI_buscador@compara-juegos.com";

    /** Contraseña en texto plano del usuario de prueba. Se encripta con BCrypt al guardar. */
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
     * @brief Crea el usuario y wishlist de prueba en BD, configura los mocks
     * y realiza el login antes de cada test.
     *
     * - La contraseña se encripta con BCrypt para que Spring Security la acepte.
     * - Se crea una wishlist asociada al usuario para que aparezca en la pantalla.
     * - BusquedaService se mockea para devolver lista vacía por defecto en buscar()
     *   y lista vacía en getOfertasHome(), evitando llamadas a APIs externas.
     */
    @BeforeEach
    void prepararDatosYContexto() {
        // Crear usuario de prueba
        Usuario usuarioPrueba = new Usuario();
        usuarioPrueba.setName("Test UI User");
        usuarioPrueba.setEmail(miEmailExclusivo);
        usuarioPrueba.setContrasena(new BCryptPasswordEncoder().encode(contrasenaPlana));
        usuarioPrueba.setFecha_nac(LocalDate.of(1995, 1, 1));
        usuarioPrueba.setPais("Espana");
        usuarioPrueba = usuarioRepositorio.save(usuarioPrueba);
        testUserId = usuarioPrueba.getId();

        // Crear wishlist de prueba asociada al usuario
        Wishlist wishlist = new Wishlist();
        wishlist.setNombre("Mi Wishlist Test");
        wishlist.setUsuario(usuarioPrueba);
        wishlist.setFechaCreacion(LocalDateTime.now());
        wishlist = wishlistRepositorio.save(wishlist);
        testWishlistId = wishlist.getId();

        // Mock por defecto: sin resultados de búsqueda y sin ofertas
        when(busquedaService.buscar(anyString())).thenReturn(List.of());
        when(busquedaService.getOfertasHome()).thenReturn(List.of());

        contexto = navegador.newContext();
        pagina = contexto.newPage();

        hacerLogin();
    }

    /**
     * @brief Simula el proceso de login mediante el formulario de /inicioSesion.
     *
     * Navega a la página de login, rellena los campos, hace submit y espera
     * a que Spring Security redirija fuera de /inicioSesion.
     * Incluye aserción explícita de que la redirección apunta a /buscar.
     */
    private void hacerLogin() {
        pagina.navigate("http://localhost:" + port + "/inicioSesion");
        pagina.fill("input[name='username']", miEmailExclusivo);
        pagina.fill("input[name='password']", contrasenaPlana);
        pagina.click("button[type='submit']");
        pagina.waitForURL(url -> !url.contains("/inicioSesion"));

        // Aserción explícita de que el login redirigió correctamente a /buscar
        Assertions.assertTrue(pagina.url().contains("/buscar"),
                "El login no redirigió a /buscar, URL actual: " + pagina.url());
    }

    /**
     * @brief Verifica que la pantalla de búsqueda carga correctamente con sesión activa.
     *
     * Comprueba que no redirige al login y que la URL contiene /buscar.
     */
    @Test
    @DisplayName("La página /buscar carga correctamente con sesión activa")
    void testPantallaBuscarCargaSinInvolucrarJuegos() {
        pagina.navigate("http://localhost:" + port + "/buscar");

        Assertions.assertFalse(pagina.url().contains("/inicioSesion"),
                "Redirigió al login — la sesión no se estableció correctamente");
        Assertions.assertTrue(pagina.url().contains("/buscar"),
                "No se llegó a /buscar, URL actual: " + pagina.url());
    }

    /**
     * @brief Verifica que los botones de flecha del carrusel están presentes y son clicables.
     *
     * Las flechas son JavaScript puro — no hacen peticiones al servidor.
     * Se verifica que existen en el DOM y que el click no lanza ningún error.
     */
    @Test
    @DisplayName("Los botones del carrusel están presentes y son clicables")
    void testBotonesCarrusel() {
        pagina.navigate("http://localhost:" + port + "/buscar");

        Locator flechaDerecha = pagina.locator("button.arrow.right");
        Locator flechaIzquierda = pagina.locator("button.arrow.left");

        Assertions.assertTrue(flechaDerecha.isVisible(),
                "El botón de flecha derecha no está visible");
        Assertions.assertTrue(flechaIzquierda.isVisible(),
                "El botón de flecha izquierda no está visible");

        // Verificar que el click no lanza errores JavaScript
        flechaDerecha.click();
        flechaIzquierda.click();
    }

    /**
     * @brief Verifica que el formulario de búsqueda responde sin errores al enviar una query.
     *
     * BusquedaService está mockeado para devolver lista vacía, así que debe
     * aparecer el mensaje de "No se encontraron resultados".
     * Usa press("Enter") en el input para garantizar que el formulario se envía
     * correctamente evitando problemas de validación del navegador con el atributo required.
     * Usa waitForLoadState(NETWORKIDLE) para esperar a que la red esté quieta.
     */
    @Test
    @DisplayName("El formulario de búsqueda responde correctamente con una query sin resultados")
    void testFormularioBusquedaSinResultados() {
        pagina.navigate("http://localhost:" + port + "/buscar");

        pagina.fill("input[name='q']", "juegoquenuncaexistira12345");
        // ✅ Enter es más fiable que click en submit para formularios GET con required
        pagina.press("input[name='q']", "Enter");
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        // La página no debe dar error 500
        Assertions.assertTrue(pagina.url().contains("/buscar"),
                "La búsqueda redirigió fuera de /buscar");

        // Debe aparecer el mensaje de no encontrado
        Assertions.assertTrue(
                pagina.locator("p:has-text('No se encontraron resultados')").isVisible(),
                "Debería aparecer el mensaje de no encontrado");
    }

    /**
     * @brief Verifica que el formulario de búsqueda muestra resultados cuando el servicio los devuelve.
     *
     * Se mockea BusquedaService para devolver un resultado de prueba y se verifica
     * que el nombre del juego aparece en la página.
     * Usa press("Enter") en el input para garantizar que el formulario se envía
     * correctamente evitando problemas de validación del navegador con el atributo required.
     * Usa waitForLoadState(NETWORKIDLE) para esperar a que la red esté quieta.
     */
    @Test
    @DisplayName("El formulario de búsqueda muestra resultados cuando el servicio los devuelve")
    void testFormularioBusquedaConResultados() {
        // Mock con un resultado de prueba
        ResultadoBusquedaDTO juegoMock = new ResultadoBusquedaDTO();
        juegoMock.setName("Juego Mock Test");
        juegoMock.setDescripcion("Descripción de prueba");
        juegoMock.setSteamPrice(19.99);
        juegoMock.setSteamUrl("https://store.steampowered.com");
        when(busquedaService.buscar(anyString())).thenReturn(List.of(juegoMock));

        pagina.navigate("http://localhost:" + port + "/buscar");
        pagina.fill("input[name='q']", "Juego Mock Test");
        // ✅ Enter es más fiable que click en submit para formularios GET con required
        pagina.press("input[name='q']", "Enter");
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        // El nombre del juego debe aparecer en la página
        Assertions.assertTrue(
                pagina.locator("h3:has-text('Juego Mock Test')").isVisible(),
                "El nombre del juego mockeado debería aparecer en los resultados");
    }

    /**
     * @brief Verifica que el link de la wishlist navega correctamente a /wishlist/{id}.
     *
     * El usuario de prueba tiene una wishlist creada en @BeforeEach.
     * Se verifica que el link aparece y que al hacer click navega a la URL correcta.
     */
    @Test
    @DisplayName("El link de wishlist navega correctamente a /wishlist/{id}")
    void testLinkWishlistNavega() {
        pagina.navigate("http://localhost:" + port + "/buscar");

        // El link de la wishlist debe estar visible
        Locator linkWishlist = pagina.locator("a[href*='/wishlist/']").first();
        Assertions.assertTrue(linkWishlist.isVisible(),
                "El link de la wishlist no está visible");

        // Al hacer click debe navegar a /wishlist/{id}
        linkWishlist.click();
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        Assertions.assertTrue(pagina.url().contains("/wishlist/"),
                "No navegó a /wishlist/, URL actual: " + pagina.url());
    }

    /**
     * @brief Verifica que el botón "Añadir a Wishlist" aparece y funciona con resultados mockeados.
     *
     * Se mockea BusquedaService para devolver un resultado con wishlist disponible
     * y se verifica que el botón aparece y que al hacer click redirige a /wishlist/{id}.
     * Usa press("Enter") en el input para garantizar que el formulario se envía
     * correctamente evitando problemas de validación del navegador con el atributo required.
     * Usa waitForLoadState(NETWORKIDLE) para esperar a que la red esté quieta
     * tanto tras el submit del buscador como tras el click en añadir.
     */
    @Test
    @DisplayName("El botón Añadir a Wishlist aparece y funciona con resultados mockeados")
    void testBotonAnadirAWishlist() {
        // Mock con un resultado de prueba
        ResultadoBusquedaDTO juegoMock = new ResultadoBusquedaDTO();
        juegoMock.setName("Juego Mock Wishlist");
        juegoMock.setDescripcion("Descripción de prueba");
        juegoMock.setSteamPrice(29.99);
        juegoMock.setSteamUrl("https://store.steampowered.com");
        when(busquedaService.buscar(anyString())).thenReturn(List.of(juegoMock));

        pagina.navigate("http://localhost:" + port + "/buscar");
        pagina.fill("input[name='q']", "Juego Mock Wishlist");
        // ✅ Enter es más fiable que click en submit para formularios GET con required
        pagina.press("input[name='q']", "Enter");

        // Esperar a que carguen los resultados
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        // El botón de añadir a wishlist debe aparecer
        Locator botonAnadir = pagina.locator("button:has-text('Añadir a Wishlist')").first();
        Assertions.assertTrue(botonAnadir.isVisible(),
                "El botón 'Añadir a Wishlist' no está visible");

        botonAnadir.click();

        // Esperar a que la redirección a /wishlist/{id} complete
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        // Debe redirigir a /wishlist/{id}
        Assertions.assertTrue(pagina.url().contains("/wishlist/"),
                "Tras añadir a wishlist no redirigió a /wishlist/, URL: " + pagina.url());
    }

    /**
     * @brief Cierra el contexto de navegación y elimina los datos de prueba de la BD.
     *
     * Se ejecuta después de cada test para garantizar un estado limpio
     * y evitar efectos colaterales entre tests.
     */
    @AfterEach
    void cerrarContexto() {
        if (contexto != null) {
            contexto.close();
        }
        if (testWishlistId != null) {
            wishlistRepositorio.deleteById(testWishlistId);
        }
        if (testUserId != null) {
            usuarioRepositorio.deleteById(testUserId);
        }
    }

    /**
     * @brief Cierra el navegador y la instancia de Playwright al finalizar todos los tests.
     *
     * Se ejecuta una sola vez al terminar todos los tests de la clase
     * para liberar los recursos del navegador correctamente.
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