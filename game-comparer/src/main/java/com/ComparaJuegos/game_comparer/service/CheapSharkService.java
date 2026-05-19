package com.ComparaJuegos.game_comparer.service;

import com.ComparaJuegos.game_comparer.dto.CheapSharkPrecioDTO;
import com.ComparaJuegos.game_comparer.dto.OfertasHomeDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @brief Servicio para obtener precios de juegos desde la API de CheapShark.
 *
 * Consulta la API pública de CheapShark (cheapshark.com/api/1.0) para buscar
 * deals de Steam (storeID=1) y Epic Games Store (storeID=25).
 * Para juegos no disponibles en CheapShark (p.ej. juegos free-to-play),
 * {@link BusquedaService} aplica fallbacks adicionales.
 */
@Service
public class CheapSharkService {

    private final RestClient restClient;

    /**
     * @brief Constructor por defecto. Crea un RestClient estándar.
     */
    public CheapSharkService() {
        this.restClient = RestClient.create();
    }

    /**
     * @brief Constructor secundario para inyección en tests.
     * @param restClient RestClient a usar (normalmente un mock en tests unitarios).
     */
    public CheapSharkService(RestClient restClient) {
        this.restClient = restClient;
    }

    private static final String STEAM_STORE_ID = "1";
    private static final String EPIC_STORE_ID  = "25";

    /**
     * @brief Busca los precios actuales de un juego en Steam y Epic Games Store.
     *
     * Realiza dos llamadas a la API de CheapShark:
     * 1. Búsqueda por título para obtener el gameID.
     * 2. Detalle del juego para obtener sus deals por tienda.
     *
     * También extrae el steamAppID del resultado de búsqueda para que
     * {@link BusquedaService} pueda usarlo como fallback hacia la API de Steam.
     *
     * @param gameName Nombre del juego a buscar (proveniente de IGDB).
     * @return DTO con los precios y URLs de Steam y Epic. Los campos son null si
     *         no se encontró precio para esa tienda.
     */
    @SuppressWarnings("unchecked")
    public CheapSharkPrecioDTO buscarPrecios(String gameName) {
        CheapSharkPrecioDTO dto = new CheapSharkPrecioDTO();
        try {
            // Step 1: search for the game
            List<Map<String, Object>> games = restClient.get()
                    .uri("https://www.cheapshark.com/api/1.0/games?title={title}&limit=5", gameName)
                    .retrieve()
                    .body(List.class);

            if (games == null || games.isEmpty()) return dto;

            String gameId = findBestMatch(games, gameName);
            if (gameId == null) return dto;

            // Extract steamAppID from search results so BusquedaService can use it as fallback
            for (Map<String, Object> g : games) {
                if (gameId.equals(String.valueOf(g.get("gameID")))) {
                    Object appId = g.get("steamAppID");
                    if (appId != null && !appId.toString().isBlank()) {
                        dto.setSteamAppId(appId.toString());
                    }
                    break;
                }
            }

            // Step 2: fetch game details (includes deals per store)
            @SuppressWarnings("unchecked")
            Map<String, Object> gameDetails = restClient.get()
                    .uri("https://www.cheapshark.com/api/1.0/games?id={id}", gameId)
                    .retrieve()
                    .body(Map.class);

            if (gameDetails == null) return dto;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> deals = (List<Map<String, Object>>) gameDetails.get("deals");
            if (deals == null) return dto;

            for (Map<String, Object> deal : deals) {
                String storeId = String.valueOf(deal.get("storeID"));
                Object priceObj = deal.get("price");
                if (priceObj == null) continue;
                double price;
                try {
                    price = Double.parseDouble(priceObj.toString());
                } catch (NumberFormatException nfe) {
                    continue;
                }
                String dealId = deal.get("dealID") != null ? deal.get("dealID").toString() : null;
                String url = dealId != null ? "https://www.cheapshark.com/redirect?dealID=" + dealId : null;

                if (STEAM_STORE_ID.equals(storeId) && dto.getSteamPrice() == null) {
                    dto.setSteamPrice(price);
                    dto.setSteamUrl(url);
                } else if (EPIC_STORE_ID.equals(storeId) && dto.getEpicPrice() == null) {
                    dto.setEpicPrice(price);
                    dto.setEpicUrl(url);
                }
            }
        } catch (Exception e) {
            System.err.println("[CheapShark] Error fetching prices for '" + gameName + "': " + e.getMessage());
        }
        return dto;
    }

    /**
     * @brief Selecciona el gameID de CheapShark que mejor coincide con el nombre buscado.
     *
     * Aplica cuatro pasadas en orden de precisión decreciente:
     * 1. Coincidencia exacta (sin distinguir mayúsculas).
     * 2. El título de CheapShark contiene el nombre buscado.
     * 3. El nombre buscado contiene el título de CheapShark (para títulos IGDB más largos).
     * 4. Coincidencia normalizada (elimina símbolos como ™ y ®).
     *
     * @param games      Lista de resultados devueltos por CheapShark.
     * @param targetName Nombre del juego a buscar.
     * @return El gameID del mejor resultado, o null si ninguno coincide.
     */
    private String findBestMatch(List<Map<String, Object>> games, String targetName) {
        String lower = targetName.toLowerCase();
        // Pass 1: exact match (case-insensitive)
        for (Map<String, Object> g : games) {
            String title = extractTitle(g);
            if (title != null && title.equalsIgnoreCase(targetName)) {
                return String.valueOf(g.get("gameID"));
            }
        }
        // Pass 2: CheapShark title contains target
        for (Map<String, Object> g : games) {
            String title = extractTitle(g);
            if (title != null && title.toLowerCase().contains(lower)) {
                return String.valueOf(g.get("gameID"));
            }
        }
        // Pass 3: target contains CheapShark title (IGDB title is longer, e.g. has edition/subtitle)
        for (Map<String, Object> g : games) {
            String title = extractTitle(g);
            if (title != null && lower.contains(title.toLowerCase())) {
                return String.valueOf(g.get("gameID"));
            }
        }
        // Pass 4: normalized — strip ™ ® punctuation differences
        String normalizedTarget = lower.replaceAll("[^a-z0-9 ]", "").trim();
        for (Map<String, Object> g : games) {
            String title = extractTitle(g);
            if (title != null) {
                String normalizedTitle = title.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim();
                if (normalizedTitle.equals(normalizedTarget)
                        || normalizedTitle.contains(normalizedTarget)
                        || normalizedTarget.contains(normalizedTitle)) {
                    return String.valueOf(g.get("gameID"));
                }
            }
        }
        return null;
    }

    /**
     * @brief Extrae el título de un resultado de búsqueda de CheapShark.
     *
     * El endpoint /games?title= devuelve el título en el campo "external" (nivel raíz).
     * El endpoint /games?id= lo devuelve dentro de "info.title".
     * Este método prueba ambas ubicaciones para mayor robustez.
     *
     * @param game Mapa con los datos de un juego devueltos por CheapShark.
     * @return El título del juego, o null si no se encuentra en ningún campo.
     */
    private String extractTitle(Map<String, Object> game) {
        Object external = game.get("external");
        if (external != null) return external.toString();
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) game.get("info");
        if (info != null) {
            Object title = info.get("title");
            if (title != null) return title.toString();
        }
        return null;
    }



}
