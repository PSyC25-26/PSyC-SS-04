/**
 * @file WishlistIntegrationTest.java
 * @author Equipo Caza Ofertas Gaming (COG)
 * 
 * Test de integración para el flujo completo de wishlist.
 * 
 * IMPORTANTE: Para evitar problemas con sesiones y cookies, se utiliza el
 * perfil "test" que desactiva la seguridad (ver TestSecurityConfig). 
 * El controlador /perfil acepta un parámetro opcional "testUserId" que permite 
 * acceder al perfil de un usuario sin necesidad de autenticación.
 * 
 * Flujo:
 * 1. Registrar un nuevo usuario.
 * 2. Obtener su ID desde la base de datos.
 * 3. Acceder a /perfil?testUserId=... para extraer la wishlistId (si existe).
 * 4. Si no existe, crear una wishlist y volver a extraer el ID.
 * 5. Añadir un juego mock a la wishlist.
 * 6. Verificar que el juego aparece por su nombre.
 * 7. Eliminar el juego de la wishlist.
 * 8. Verificar que ya no aparece.
 * 
 * No se utiliza login, cookies ni configuraciones complejas de RestTemplate.
 */

package com.ComparaJuegos.game_comparer.Integracion;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.ComparaJuegos.game_comparer.UsuarioRepositorio;
import com.ComparaJuegos.game_comparer.models.Usuario;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Clase de test de integración para el flujo de wishlists.
 * Configura un entorno de pruebas con perfil "test", desactiva la seguridad
 * y utiliza un RestTemplate simple para realizar peticiones HTTP.
 * Se apoya en una base de datos H2 en memoria (por defecto) y limpia
 * los datos después de cada test mediante @AfterEach.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = { "spring.main.allow-bean-definition-overriding=true" })
public class WishlistIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    private final RestTemplate restTemplate = new RestTemplate();

    private String baseUrl;
    private String userEmail;
    private String userPassword;
    private Long userId;
    private Long wishlistId;
    private Long gameId;

    // Datos mock del juego (equivalente a lo que devolvería IGDB para "Minecraft")
    private static final String MOCK_GAME_NAME = "Minecraft";
    private static final String MOCK_GAME_DESC = "Sandbox game where players explore a blocky world.";
    private static final String MOCK_GAME_IMAGE = "https://images.igdb.com/igdb/image/upload/t_cover_big/co49x5.jpg";
    private static final String MOCK_GAME_GENRE = "Sandbox, Survival";
    private static final String MOCK_GAME_DEVELOPER = "Mojang Studios";
    private static final String MOCK_GAME_PUBLISHER = "Microsoft Studios";
    private static final double MOCK_GAME_STEAM_PRICE = 26.99;
    private static final String MOCK_GAME_STEAM_URL = "https://store.steampowered.com/app/1672970/Minecraft/";

    /**
     * Configuración inicial antes de cada test.
     * Construye la URL base, genera un email único y establece la contraseña.
     */
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        userEmail = "testuser_" + System.currentTimeMillis() + "@test.com";
        userPassword = "TestPassword123!";
    }

    /**
     * Limpieza posterior a cada test.
     * Elimina el usuario de prueba de la base de datos para evitar
     * efectos colaterales entre tests.
     */
    @org.junit.jupiter.api.AfterEach
    void cleanup() {
        if (userId != null) {
            usuarioRepositorio.deleteById(userId);
            System.out.println("Usuario de test eliminado de la BD: " + userId);
        }
    }

    /**
     * Test principal que ejecuta el flujo completo de wishlist.
     */
    @Test
    void testWishlistUserStory() {
        registerUser();
        fetchUserIdFromDatabase();
        visitProfileWithTestUserId();
        if (wishlistId == null) {
            createWishlist();
            wishlistId = extractWishlistIdFromProfile();
        }
        addGameToWishlist();
        verifyGameAdded();
        removeGameFromWishlist();
        verifyGameRemoved();
    }

    /**
     * Registra un nuevo usuario mediante POST /registro.
     * Los datos se envían como application/x-www-form-urlencoded.
     */
    private void registerUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String payload = "name=Test+User"
                + "&email=" + encode(userEmail)
                + "&contrasena=" + encode(userPassword)
                + "&fecha_nac=1990-01-01"
                + "&pais=Espana";

        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/registro", request, String.class);

        assertTrue(response.getStatusCode().is3xxRedirection() || response.getStatusCode().is2xxSuccessful(),
                "Registro fallido. Status: " + response.getStatusCode());
    }

    /**
     * Obtiene el userId del usuario recién registrado consultando directamente la
     * base de datos.
     */
    private void fetchUserIdFromDatabase() {
        Usuario usuario = usuarioRepositorio.findByEmail(userEmail).orElseThrow();
        userId = usuario.getId();
        System.out.println("Usuario ID obtenido de BD: " + userId);
    }

    /**
     * Visita la página /perfil pasando testUserId como parámetro (sin
     * autenticación)
     * y extrae el primer wishlistId encontrado en el HTML.
     */
    private void visitProfileWithTestUserId() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/perfil?testUserId=" + userId, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "No se pudo acceder a /perfil con testUserId=" + userId);
        wishlistId = extractFirstWishlistId(response.getBody());
    }

    /**
     * Crea una nueva wishlist para el usuario mediante POST /crear-wishlist.
     */
    private void createWishlist() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String payload = "id=" + userId + "&nombre=Mi+Wishlist+Test";
        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/crear-wishlist", request, String.class);
        assertTrue(response.getStatusCode().is3xxRedirection() || response.getStatusCode().is2xxSuccessful(),
                "Crear wishlist fallido: " + response.getStatusCode());
    }

    /**
     * Vuelve a cargar el perfil (con testUserId) y extrae la wishlistId recién
     * creada.
     * 
     * @return ID de la wishlist, o null si no se encuentra.
     */
    private Long extractWishlistIdFromProfile() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/perfil?testUserId=" + userId, String.class);
        return extractFirstWishlistId(response.getBody());
    }

    /**
     * Añade un juego mock a la wishlist mediante POST /wishlist/agregar.
     * También carga la página de la wishlist para extraer el gameId.
     */
    private void addGameToWishlist() {
        assertNotNull(wishlistId, "No hay wishlistId para añadir juego");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String payload = "name=" + encode(MOCK_GAME_NAME)
                + "&descripcion=" + encode(MOCK_GAME_DESC)
                + "&imagen=" + encode(MOCK_GAME_IMAGE)
                + "&genero=" + encode(MOCK_GAME_GENRE)
                + "&developer=" + encode(MOCK_GAME_DEVELOPER)
                + "&publisher=" + encode(MOCK_GAME_PUBLISHER)
                + "&steamPrice=" + MOCK_GAME_STEAM_PRICE
                + "&steamUrl=" + encode(MOCK_GAME_STEAM_URL)
                + "&epicPrice="
                + "&epicUrl=";

        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/wishlist/agregar?wishlistId=" + wishlistId, request, String.class);
        assertTrue(response.getStatusCode().is3xxRedirection() || response.getStatusCode().is2xxSuccessful(),
                "Añadir juego fallido: " + response.getStatusCode());

        // Cargar la página de la wishlist para extraer el gameId (necesario para
        // eliminar)
        ResponseEntity<String> wishlistPage = restTemplate.getForEntity(
                baseUrl + "/wishlist/" + wishlistId + "?testUserId=" + userId, String.class);
        assertEquals(HttpStatus.OK, wishlistPage.getStatusCode());
        gameId = extractGameId(wishlistPage.getBody());
        assertNotNull(gameId, "No se pudo extraer el ID del juego recién añadido");
        System.out.println("Juego añadido con ID: " + gameId);
    }

    /**
     * Verifica que el juego añadido aparece en la wishlist (buscando su nombre).
     */
    private void verifyGameAdded() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/wishlist/" + wishlistId + "?testUserId=" + userId, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains(MOCK_GAME_NAME),
                "La wishlist debería contener el nombre '" + MOCK_GAME_NAME + "'");
    }

    /**
     * Elimina el juego de la wishlist mediante POST /wishlist/eliminar.
     */
    private void removeGameFromWishlist() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String payload = "wishlistId=" + wishlistId + "&juegoId=" + gameId;
        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/wishlist/eliminar", request, String.class);
        assertTrue(response.getStatusCode().is3xxRedirection() || response.getStatusCode().is2xxSuccessful(),
                "Eliminar juego fallido: " + response.getStatusCode());
    }

    /**
     * Verifica que el juego ya no aparece en la wishlist tras ser eliminado.
     */
    private void verifyGameRemoved() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/wishlist/" + wishlistId + "?testUserId=" + userId, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().contains(MOCK_GAME_NAME),
                "La wishlist NO debería contener '" + MOCK_GAME_NAME + "' después de eliminar");
    }

    // =========================================================================
    // Métodos auxiliares de extracción desde HTML
    // =========================================================================

    /**
     * Extrae el primer /wishlist/{id} del HTML del perfil.
     * 
     * @param html Código HTML de la página.
     * @return ID de la wishlist, o null si no se encuentra.
     */
    private Long extractFirstWishlistId(String html) {
        if (html == null)
            return null;
        Matcher m = Pattern.compile("/wishlist/(\\d+)").matcher(html);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }

    /**
     * Extrae el ID del juego del HTML de detalle de la wishlist.
     * Busca el campo oculto: <input name="juegoId" value="123">.
     * 
     * @param html Código HTML de la página.
     * @return ID del juego, o null si no se encuentra.
     */
    private Long extractGameId(String html) {
        if (html == null)
            return null;
        Matcher m = Pattern.compile("name=\"juegoId\"\\s+value=\"(\\d+)\"").matcher(html);
        if (m.find())
            return Long.parseLong(m.group(1));
        // Fallback: búsqueda más genérica
        m = Pattern.compile("juegoId=(\\d+)").matcher(html);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }

    /**
     * Codifica una cadena para ser usada en application/x-www-form-urlencoded.
     * 
     * @param value Cadena a codificar.
     * @return Cadena codificada en UTF-8.
     */
    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}