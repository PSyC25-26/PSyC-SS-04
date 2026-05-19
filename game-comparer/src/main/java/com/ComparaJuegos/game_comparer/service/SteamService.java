package com.ComparaJuegos.game_comparer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * @brief Servicio para consultar precios directamente desde la API de Steam Store.
 *
 * Se usa como fallback cuando CheapShark no tiene precio de Steam para un juego,
 * lo que ocurre principalmente con juegos free-to-play (p.ej. Team Fortress 2)
 * o juegos que nunca han estado en oferta.
 *
 * Endpoints utilizados:
 * - appdetails: obtiene el precio dado un Steam App ID.
 * - storesearch: busca el App ID de un juego por nombre.
 */
@Service
@Slf4j
public class SteamService {

    private final RestClient restClient;

    /**
     * @brief Constructor por defecto. Crea un RestClient estándar.
     */
    public SteamService() {
        this.restClient = RestClient.create();
    }

    /**
     * @brief Obtiene el precio actual de un juego en Steam dado su App ID.
     *
     * Consulta la API de Steam Store (store.steampowered.com/api/appdetails)
     * con región europea (cc=eu) para obtener precios en euros.
     *
     * @param appId Steam App ID del juego (p.ej. "252490" para Rocket League).
     * @return 0.0 si el juego es free-to-play, el precio en euros si es de pago,
     *         o null si el juego no está disponible o la petición falla.
     */
    @SuppressWarnings("unchecked")
    public Double getSteamPrice(String appId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("https://store.steampowered.com/api/appdetails?appids={id}&cc=eu&l=en", appId)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return null;

            Map<String, Object> entry = (Map<String, Object>) response.get(appId);
            if (entry == null || !Boolean.TRUE.equals(entry.get("success"))) return null;

            Map<String, Object> data = (Map<String, Object>) entry.get("data");
            if (data == null) return null;

            if (Boolean.TRUE.equals(data.get("is_free"))) return 0.0;

            Map<String, Object> priceOverview = (Map<String, Object>) data.get("price_overview");
            if (priceOverview != null) {
                Number finalPrice = (Number) priceOverview.get("final");
                if (finalPrice != null) return finalPrice.doubleValue() / 100.0;
            }
        } catch (Exception e) {
            log.error("[Steam] Error fetching price for appId '" + appId + "': " + e.getMessage());
        }
        return null;
    }

    /**
     * @brief Construye la URL de la página de un juego en Steam Store.
     *
     * @param appId Steam App ID del juego.
     * @return URL directa a la ficha del juego en store.steampowered.com.
     */
    public String getSteamUrl(String appId) {
        return "https://store.steampowered.com/app/" + appId;
    }

    /**
     * @brief Busca el Steam App ID de un juego por nombre usando el buscador de Steam Store.
     *
     * Se usa como último recurso cuando ni CheapShark ni IGDB proporcionan el App ID.
     * Aplica dos pasadas de coincidencia: exacta y por contenido.
     *
     * @param gameName Nombre del juego a buscar.
     * @return Steam App ID como String si se encuentra, o null si no hay coincidencia.
     */
    @SuppressWarnings("unchecked")
    public String findAppIdByName(String gameName) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("https://store.steampowered.com/api/storesearch?term={name}&l=english&cc=eu", gameName)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return null;

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null || items.isEmpty()) return null;

            String targetLower = gameName.toLowerCase();

            // Pass 1: exact match
            for (Map<String, Object> item : items) {
                Object nameObj = item.get("name");
                if (nameObj != null && nameObj.toString().equalsIgnoreCase(gameName)) {
                    Object id = item.get("id");
                    return id != null ? id.toString() : null;
                }
            }
            // Pass 2: Steam title contains game name
            for (Map<String, Object> item : items) {
                Object nameObj = item.get("name");
                if (nameObj != null && nameObj.toString().toLowerCase().contains(targetLower)) {
                    Object id = item.get("id");
                    return id != null ? id.toString() : null;
                }
            }
        } catch (Exception e) {
            log.error("[Steam] Error searching '" + gameName + "': " + e.getMessage());
        }
        return null;
    }
}
