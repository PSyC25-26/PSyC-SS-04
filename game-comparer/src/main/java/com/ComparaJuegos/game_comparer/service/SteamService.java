package com.ComparaJuegos.game_comparer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class SteamService {

    private final RestClient restClient;

    public SteamService() {
        this.restClient = RestClient.create();
    }

    /**
     * Returns the current Steam price for a given appId.
     * Returns 0.0 if the game is free-to-play, the price in euros if paid, or null if unavailable.
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
            System.err.println("[Steam] Error fetching price for appId '" + appId + "': " + e.getMessage());
        }
        return null;
    }

    public String getSteamUrl(String appId) {
        return "https://store.steampowered.com/app/" + appId;
    }
}
