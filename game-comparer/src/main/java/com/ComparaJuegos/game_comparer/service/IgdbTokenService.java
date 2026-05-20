package com.ComparaJuegos.game_comparer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * @brief Servicio de gestión del token OAuth2 para la API de IGDB.
 *
 * Obtiene y cachea el token de acceso de Twitch necesario para autenticar
 * las peticiones a la API de IGDB. El token se renueva automáticamente
 * 60 segundos antes de su expiración para evitar peticiones fallidas.
 *
 * El método getToken() es {@code synchronized} para garantizar que bajo
 * carga concurrente solo un hilo renueva el token a la vez.
 */
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

    /**
     * @brief Devuelve un token de acceso válido para la API de IGDB.
     *
     * Si el token en caché existe y no ha expirado, se devuelve directamente
     * sin realizar ninguna llamada a la red. En caso contrario, solicita un
     * nuevo token al endpoint OAuth2 de Twitch y lo almacena en caché.
     *
     * @return Token de acceso Bearer válido para usar en las peticiones a IGDB.
     */
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

    /**
     * @brief Devuelve el Client ID de IGDB configurado en las propiedades de la aplicación.
     *
     * Usado por {@link IgdbService} para incluir el header {@code Client-ID}
     * en cada petición a la API de IGDB.
     *
     * @return El valor de la propiedad {@code igdb.client-id}.
     */
    public String getClientId() {
        return clientId;
    }
}
