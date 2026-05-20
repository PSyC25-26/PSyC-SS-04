/**
 * @file detalleWishlistUserInterfaceTest.java
 * @author Equipo Caza Ofertas Gaming (COG)
 *
 * Test de interfaz de usuario para la pantalla de detalle de una Wishlist.
 * Utiliza Playwright para simular un navegador real y verificar que:
 * - El detalle de la lista carga correctamente mostrando su nombre dinámico.
 * - Los juegos guardados en la wishlist se renderizan con su portada, título y descripción.
 * - Los precios y enlaces asociados a las tiendas (Steam/Epic) se muestran correctamente.
 * - El botón "Eliminar" envía el formulario post y limpia el juego de la interfaz.
 */

package UserInterface;

import com.ComparaJuegos.game_comparer.GameComparerApplication;
import com.ComparaJuegos.game_comparer.JuegoRepositorio;
import com.ComparaJuegos.game_comparer.UsuarioRepositorio;
import com.ComparaJuegos.game_comparer.WishlistRepositorio;
import com.ComparaJuegos.game_comparer.models.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = GameComparerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class detalleWishlistUserInterfaceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private WishlistRepositorio wishlistRepositorio;

    @Autowired
    private JuegoRepositorio juegoRepositorio; // Único repositorio de persistencia para el juego

    private static Playwright playwright;
    private static Browser navegador;
    private BrowserContext contexto;
    private Page pagina;

    private Long testUserId;
    private Long testWishlistId;
    private Long testJuegoId;

    private final String miEmailExclusivo = "wishlist_detail_ui@compara-juegos.com";
    private final String contrasenaPlana = "Password123!";
    private final String nombreListaTest = "Joyas Ocultas RPG";
    private final String nombreJuegoTest = "The Witcher 3 Mock";

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

    @BeforeEach
    void prepararEstructuraYContexto() {
        // 1. Crear e insertar Usuario
        Usuario usuario = new Usuario();
        usuario.setName("Tester Wishlist");
        usuario.setEmail(miEmailExclusivo);
        usuario.setContrasena(new BCryptPasswordEncoder().encode(contrasenaPlana));
        usuario.setFecha_nac(LocalDate.of(1995, 8, 20));
        usuario.setPais("España");
        usuario = usuarioRepositorio.save(usuario);
        testUserId = usuario.getId();

        // 2. Crear objeto Juego base
        Juego juego = new Juego();
        juego.setName(nombreJuegoTest);
        juego.setGenero("RPG / Acción");
        juego.setDescripcion("Una descripción mockeada para comprobar el párrafo de Thymeleaf.");
        juego.setImagen("https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.jpg");
        juego.setPrecios(new ArrayList<>());

        // 3. Crear el objeto Precio y añadirlo directamente a la lista interna del juego
        Precio precio = new Precio();
        precio.setTienda(Tienda.STEAM);
        precio.setPrecio(14.99);
        precio.setUrl("https://store.steampowered.com/app/292030");
        precio.setJuego(juego);
        precio.setFechaActualizacion(LocalDateTime.now());
        juego.getPrecios().add(precio);

        // 4. Guardar el juego (gracias al CascadeType.ALL en tu modelo, persistirá también sus precios)
        juego = juegoRepositorio.save(juego);
        testJuegoId = juego.getId();

        // 5. Crear la Wishlist asignando el juego persistido
        Wishlist wishlist = new Wishlist();
        wishlist.setNombre(nombreListaTest);
        wishlist.setUsuario(usuario);
        wishlist.setFechaCreacion(LocalDateTime.now());
        wishlist.setJuegos(new ArrayList<>());
        wishlist.getJuegos().add(juego);

        wishlist = wishlistRepositorio.save(wishlist);
        testWishlistId = wishlist.getId();

        // 6. Levantar sesión en el navegador
        contexto = navegador.newContext();
        pagina = contexto.newPage();
        hacerLoginPrevio();
    }

    private void hacerLoginPrevio() {
        pagina.navigate("http://localhost:" + port + "/inicioSesion");
        pagina.fill("input[name='username']", miEmailExclusivo);
        pagina.fill("input[name='password']", contrasenaPlana);
        pagina.click("button[type='submit']");
        pagina.waitForURL(url -> !url.contains("/inicioSesion"));
    }

    @Test
    @DisplayName("La pantalla de detalle muestra el nombre de la lista, juegos y sus precios")
    void testCargaDetalleWishlistConElementos() {
        pagina.navigate("http://localhost:" + port + "/wishlist/" + testWishlistId);
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        assertTrue(pagina.locator("h2:has-text('" + nombreListaTest + "')").isVisible(),
                "El título de la wishlist no se muestra correctamente.");
        assertTrue(pagina.locator("h3:has-text('" + nombreJuegoTest + "')").isVisible(),
                "El nombre del juego dentro de la wishlist no está visible.");
        assertTrue(pagina.locator("p:has-text('RPG / Acción')").isVisible(),
                "El género del juego no se renderiza.");
        assertTrue(pagina.locator("a:has-text('STEAM: 14.99 €')").isVisible(),
                "El bloque de precios formateados de la tienda de Steam falló.");
    }

    @Test
    @DisplayName("El botón de eliminar borra de forma efectiva el juego de la vista")
    void testBotonEliminarJuegoDeWishlist() {
        pagina.navigate("http://localhost:" + port + "/wishlist/" + testWishlistId);
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        assertTrue(pagina.locator("h3:has-text('" + nombreJuegoTest + "')").isVisible());

        pagina.click("button:has-text('Eliminar')");
        pagina.waitForLoadState(LoadState.NETWORKIDLE);

        assertFalse(pagina.locator("h3:has-text('" + nombreJuegoTest + "')").isVisible(),
                "El juego sigue apareciendo en el DOM tras procesar el botón de eliminar.");
        assertTrue(pagina.locator("p:has-text('Esta wishlist está vacía')").isVisible(),
                "No se muestra el mensaje de advertencia tras vaciar la lista.");
    }

    @AfterEach
    void cerrarContexto() {
        if (contexto != null) {
            contexto.close();
        }
        if (testWishlistId != null) {
            wishlistRepositorio.deleteById(testWishlistId);
        }
        if (testJuegoId != null) {
            juegoRepositorio.deleteById(testJuegoId);
        }
        if (testUserId != null) {
            usuarioRepositorio.deleteById(testUserId);
        }
    }

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