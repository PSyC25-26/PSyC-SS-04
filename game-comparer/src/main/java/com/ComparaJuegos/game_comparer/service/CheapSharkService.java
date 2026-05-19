package com.ComparaJuegos.game_comparer.service;

import com.ComparaJuegos.game_comparer.dto.CheapSharkPrecioDTO;
import com.ComparaJuegos.game_comparer.dto.OfertasHomeDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class CheapSharkService {

    private final RestClient restClient;

    public CheapSharkService() {
        this.restClient = RestClient.create();
    }

    /** Secondary constructor — allows injecting a RestClient in tests. */
    public CheapSharkService(RestClient restClient) {
        this.restClient = restClient;
    }

    private static final String STEAM_STORE_ID = "1";
    private static final String EPIC_STORE_ID  = "25";

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
