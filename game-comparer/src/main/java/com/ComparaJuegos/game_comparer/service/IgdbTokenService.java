package com.ComparaJuegos.game_comparer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IgdbTokenService {

    @Value("${igdb.client-id}")
    private String clientId;

    @Value("${igdb.client-secret}")
    private String clientSecret;

    private String cachedToken;
    private Instant tokenExpiresAt;

    private final RestClient restClient = RestClient.create();

    public synchronized String getToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            log.info("Uso de token IGDB desde la caché interna. Válido hasta: {}", tokenExpiresAt);
            return cachedToken;
        }

        log.info("El token ha expirado o no existe. Solicitando nuevo token a Twitch...");

        String url = "https://id.twitch.tv/oauth2/token"
                + "?client_id=" + clientId
                + "&client_secret=" + clientSecret
                + "&grant_type=client_credentials";

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(url)
                .retrieve()
                .body(Map.class);

        cachedToken = (String) response.get("access_token");
        int expiresIn = (int) response.get("expires_in");
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn - 60);

        return cachedToken;
    }

    public String getClientId() {
        return clientId;
    }
}
