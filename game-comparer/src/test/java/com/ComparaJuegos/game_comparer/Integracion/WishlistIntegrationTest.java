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
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.ComparaJuegos.game_comparer.UsuarioRepositorio;
import com.ComparaJuegos.game_comparer.models.Usuario;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WishlistIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio; // Inyectado correctamente

    private RestTemplate restTemplate; // Se creará en setUp con manejo de cookies
    private String baseUrl;
    private String userEmail;
    private String userPassword;
    private Long userId;
    private Long wishlistId;
    private Long gameId;

    private static final String MOCK_GAME_NAME = "Minecraft";
    private static final String MOCK_GAME_DESC = "Sandbox game where players explore a blocky world.";
    private static final String MOCK_GAME_IMAGE = "https://images.igdb.com/igdb/image/upload/t_cover_big/co49x5.jpg";
    private static final String MOCK_GAME_GENRE = "Sandbox, Survival";
    private static final String MOCK_GAME_DEVELOPER = "Mojang Studios";
    private static final String MOCK_GAME_PUBLISHER = "Microsoft Studios";
    private static final double MOCK_GAME_STEAM_PRICE = 26.99;
    private static final String MOCK_GAME_STEAM_URL = "https://store.steampowered.com/app/1672970/Minecraft/";

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        // Email único con timestamp
        userEmail = "testuser_" + System.currentTimeMillis() + "@test.com";
        userPassword = "TestPassword123!";

        // RestTemplate con manejador de errores que no lance excepción (para poder
        // inspeccionar cuerpos)
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }
        });
    }

    @Test
    void testWishlistUserStory() {
        registerUser();
        loginUser(); // <-- AHORA EL LOGIN ES REAL
        fetchUserIdFromDatabase(); // Obtenemos el ID del usuario desde la BD (ya que estamos autenticados)
        visitProfileAndExtractWishlistId(); // Accedemos a /perfil autenticado y extraemos wishlistId
        if (wishlistId == null) {
            createWishlist();
            wishlistId = extractWishlistIdFromProfile(); // Volvemos a extraer
        }
        addGameToWishlist();
        verifyGameAdded();
        removeGameFromWishlist();
        verifyGameRemoved();
    }

    // 1. Registrar
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
                "Registro fallido: " + response.getStatusCode());
    }

    // 2. Login real (form login)
    private void loginUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String payload = "username=" + encode(userEmail) + "&password=" + encode(userPassword);
        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/inicioSesion", request, String.class);

        // Spring Security debe redirigir (302) a /perfil (por defaultSuccessUrl)
        assertTrue(response.getStatusCode().is3xxRedirection(),
                "Login fallido. Status: " + response.getStatusCode() + " Body: " + response.getBody());

        // El RestTemplate automáticamente guarda la cookie JSESSIONID de la respuesta
        // y la enviará en siguientes peticiones.
    }

    // 3. Obtener userId desde la base de datos (ya que estamos autenticados, pero
    // el controlador /perfil no nos devuelve el ID fácilmente)
    private void fetchUserIdFromDatabase() {
        Usuario usuario = usuarioRepositorio.findByEmail(userEmail).orElseThrow();
        userId = usuario.getId();
        System.out.println("Usuario ID obtenido de BD: " + userId);
    }

    // 4. Visitar /perfil (autenticado) para extraer la wishlistId (si existe)
    private void visitProfileAndExtractWishlistId() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/perfil", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "No se pudo acceder a /perfil después del login");
        wishlistId = extractFirstWishlistId(response.getBody());
    }

    // 5. Crear wishlist (si no existía)
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

    // Extraer wishlistId después de crear (recargando perfil)
    private Long extractWishlistIdFromProfile() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/perfil", String.class);
        return extractFirstWishlistId(response.getBody());
    }

    // 6. Añadir juego
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

        // Cargar la wishlist para extraer el gameId
        ResponseEntity<String> wishlistPage = restTemplate.getForEntity(baseUrl + "/wishlist/" + wishlistId,
                String.class);
        assertEquals(HttpStatus.OK, wishlistPage.getStatusCode());
        gameId = extractGameId(wishlistPage.getBody());
        assertNotNull(gameId);
        System.out.println("Juego añadido con ID: " + gameId);
    }

    // 7. Verificar que aparece
    private void verifyGameAdded() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/wishlist/" + wishlistId, String.class);
        assertTrue(response.getBody().contains(MOCK_GAME_NAME), "El juego debería aparecer");
    }

    // 8. Eliminar
    private void removeGameFromWishlist() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String payload = "wishlistId=" + wishlistId + "&juegoId=" + gameId;
        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/wishlist/eliminar", request, String.class);
        assertTrue(response.getStatusCode().is3xxRedirection() || response.getStatusCode().is2xxSuccessful());
    }

    // 9. Verificar que desaparece
    private void verifyGameRemoved() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/wishlist/" + wishlistId, String.class);
        assertFalse(response.getBody().contains(MOCK_GAME_NAME), "El juego no debería aparecer tras eliminar");
    }

    // Helpers de extracción (igual que tenías)
    private Long extractFirstWishlistId(String html) {
        if (html == null)
            return null;
        Matcher m = Pattern.compile("/wishlist/(\\d+)").matcher(html);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }

    private Long extractGameId(String html) {
        if (html == null)
            return null;
        Matcher m = Pattern.compile("name=\"juegoId\"\\s+value=\"(\\d+)\"").matcher(html);
        if (m.find())
            return Long.parseLong(m.group(1));
        m = Pattern.compile("juegoId=(\\d+)").matcher(html);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}