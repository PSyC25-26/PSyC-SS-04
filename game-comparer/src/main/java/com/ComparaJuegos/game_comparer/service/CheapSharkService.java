package com.ComparaJuegos.game_comparer.service;

import com.ComparaJuegos.game_comparer.dto.CheapSharkPrecioDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class CheapSharkService {

    private final RestClient restClient = RestClient.create();

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

            // Step 2: fetch ALL deals for this game in one call, filter by store in memory
            List<Map<String, Object>> deals = restClient.get()
                    .uri("https://www.cheapshark.com/api/1.0/deals?gameID={gid}", gameId)
                    .retrieve()
                    .body(List.class);

            if (deals == null) return dto;

            for (Map<String, Object> deal : deals) {
                String storeId = String.valueOf(deal.get("storeID"));
                String salePriceStr = (String) deal.get("salePrice");
                String dealId = (String) deal.get("dealID");
                if (salePriceStr == null) continue;

                double price = Double.parseDouble(salePriceStr);
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
            // Rate limited or unavailable — return empty prices rather than crashing
        }
        return dto;
    }

    private String findBestMatch(List<Map<String, Object>> games, String targetName) {
        String lower = targetName.toLowerCase();
        for (Map<String, Object> g : games) {
            @SuppressWarnings("unchecked")
            Map<String, Object> info = (Map<String, Object>) g.get("info");
            if (info != null) {
                String title = (String) info.get("title");
                if (title != null && title.equalsIgnoreCase(targetName)) {
                    return String.valueOf(g.get("gameID"));
                }
            }
        }
        for (Map<String, Object> g : games) {
            @SuppressWarnings("unchecked")
            Map<String, Object> info = (Map<String, Object>) g.get("info");
            if (info != null) {
                String title = (String) info.get("title");
                if (title != null && title.toLowerCase().contains(lower)) {
                    return String.valueOf(g.get("gameID"));
                }
            }
        }
        return String.valueOf(games.get(0).get("gameID"));
    }

}
