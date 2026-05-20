package com.ComparaJuegos.game_comparer.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ComparaJuegos.game_comparer.service.SteamService;

import static org.junit.jupiter.api.Assertions.*;

public class SteamServicePerfTest {
    private SteamService steamService;

    @BeforeEach
    void setUp() {
        // Inicializamos el servicio de forma limpia antes de cada test
        steamService = new SteamService();
    }

    @Test
    void testGetSteamUrl() {
        // Cubre el método más simple que genera la URL de la ficha
        String appId = "252490";
        String expectedUrl = "https://store.steampowered.com/app/252490";
        assertEquals(expectedUrl, steamService.getSteamUrl(appId));
    }

    @Test
    void testGetSteamPrice_Success_PaidGame() {
        // Probamos con un juego de pago ultra conocido (Portal 2 -> AppID 620)
        // Esto pasará por las líneas de validación y de mapeo de precios
        Double price = steamService.getSteamPrice("620");
        
        // No validamos el valor exacto porque el precio cambia con las rebajas,
        // pero sí que la estructura del JSON responde y se parsea correctamente.
        assertNotNull(price, "El precio de Portal 2 no debería ser nulo");
        assertTrue(price >= 0.0, "El precio debería ser un valor positivo");
    }

    @Test
    void testGetSteamPrice_Success_FreeGame() {
        // Probamos con un juego Free-To-Play eterno (Team Fortress 2 -> AppID 440)
        // Esto obliga a JaCoCo a pisar la línea: if (Boolean.TRUE.equals(data.get("is_free"))) return 0.0;
        Double price = steamService.getSteamPrice("440");
        
        assertNotNull(price);
        assertEquals(0.0, price, "Un juego free-to-play debe retornar 0.0");
    }

    @Test
    void testGetSteamPrice_InvalidAppId() {
        // Forzamos que la API responda success:false usando un ID que no existe
        Double price = steamService.getSteamPrice("999999999");
        assertNull(price, "Un ID inválido debe retornar null");
    }

    @Test
    void testGetSteamPrice_ExceptionCatch() {
        // Forzamos que salte el bloque catch enviando un null
        // Al intentar concatenar o procesar el null, restClient o la lógica lanzarán Exception
        // ¡Esto pintará de verde tu bloque 'catch' de logs!
        Double price = steamService.getSteamPrice(null);
        assertNull(price);
    }

    @Test
    void testFindAppIdByName_ExactAndPartialMatch() {
        // Probamos el buscador de nombres de Steam con un juego clásico
        String appId = steamService.findAppIdByName("Portal 2");
        
        assertNotNull(appId, "Debería encontrar el AppID de Portal 2");
        assertEquals("620", appId, "El ID de Portal 2 debe ser 620");
    }

    @Test
    void testFindAppIdByName_NotFound() {
        // Buscamos un juego inventado para que la lista de items venga vacía o nula
        String appId = steamService.findAppIdByName("JuegoInexistenteQueNadieConoce12345");
        assertNull(appId);
    }

    @Test
    void testFindAppIdByName_ExceptionCatch() {
        // Forzamos el bloque catch del buscador enviando un null
        // ¡Esto pintará de verde el catch de logs de este método!
        String appId = steamService.findAppIdByName(null);
        assertNull(appId);
    }
}
