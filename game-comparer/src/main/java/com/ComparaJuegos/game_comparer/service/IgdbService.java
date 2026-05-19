package com.ComparaJuegos.game_comparer.service;

import com.ComparaJuegos.game_comparer.dto.IgdbJuegoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * @brief Servicio para buscar información de juegos en la API de IGDB.
 *
 * Consulta la API de IGDB (api.igdb.com/v4) para obtener metadatos de juegos:
 * nombre, descripción, portada, géneros, fecha de lanzamiento, desarrollador,
 * publisher y enlaces a tiendas externas (Steam App ID, Epic slug).
 *
 * El token OAuth necesario para IGDB se gestiona en {@link IgdbTokenService}.
 */
@Service
@Slf4j
public class IgdbService {

    //private static final Logger log = LoggerFactory.getLogger(IgdbService.class);

    private final IgdbTokenService tokenService;
    private final RestClient restClient;

    /**
     * @brief Constructor principal. Spring inyecta el IgdbTokenService automáticamente.
     * @param tokenService Servicio que gestiona el token OAuth de IGDB.
     */
    @Autowired
    public IgdbService(IgdbTokenService tokenService) {
        this.tokenService = tokenService;
        this.restClient = RestClient.create();
    }

    /**
     * @brief Constructor secundario para inyección en tests.
     * @param tokenService Servicio de token (puede ser un mock).
     * @param restClient   RestClient a usar (normalmente un mock en tests unitarios).
     */
    public IgdbService(IgdbTokenService tokenService, RestClient restClient) {
        this.tokenService = tokenService;
        this.restClient = restClient;
    }

    /**
     * @brief Busca juegos en IGDB por nombre y devuelve hasta 5 resultados.
     *
     * Incluye en la consulta los campos de external_games para poder extraer
     * los identificadores de Steam (category=1) y Epic Games (category=26).
     *
     * @param query Texto de búsqueda introducido por el usuario.
     * @return Lista de hasta 5 juegos encontrados, o lista vacía si falla la petición.
     */
    @SuppressWarnings("unchecked")
    public List<IgdbJuegoDTO> buscar(String query) {

        log.info("Iniciando consulta en la API de IGDB para el término: '{}'", query);

        String body = "fields name, summary, cover.image_id, genres.name, first_release_date, " +
                "involved_companies.company.name, involved_companies.developer, involved_companies.publisher, " +
                "external_games.uid, external_games.category; " +
                "search \"" + query + "\"; limit 5;";

        try {
            List<Map<String, Object>> response = restClient.post()
                    .uri("https://api.igdb.com/v4/games")
                    .header("Client-ID", tokenService.getClientId())
                    .header("Authorization", "Bearer " + tokenService.getToken())
                    .header("Content-Type", "text/plain")
                    .body(body)
                    .retrieve()
                    .body(List.class);

            List<IgdbJuegoDTO> resultados = parseGameResponse(response);
            log.info("Consulta de IGDB completada. Se encontraron {} resultados para '{}'", resultados.size(), query);
            return resultados;
        } catch (Exception e) {
            log.error("IGDB API call failed for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * @brief Convierte la respuesta raw de IGDB en una lista de {@link IgdbJuegoDTO}.
     *
     * Método separado del HTTP para permitir tests unitarios y de rendimiento
     * que prueben el parseo sin necesidad de mockear la capa de red.
     *
     * Extrae los identificadores de plataformas externas de external_games:
     * - category 1  → Steam App ID (se guarda en steamAppId)
     * - category 26 → Epic Games slug (se guarda en epicSlug)
     *
     * @param response Lista de mapas con la respuesta JSON de IGDB, o null.
     * @return Lista de DTOs parseados, o lista vacía si la respuesta es null.
     */
    @SuppressWarnings("unchecked")
    public List<IgdbJuegoDTO> parseGameResponse(List<Map<String, Object>> response) {
        if (response == null) {
            log.warn("La respuesta recibida de la API de IGDB fue null.");
            return List.of();
        }

        List<IgdbJuegoDTO> results = new ArrayList<>();
        for (Map<String, Object> game : response) {
            IgdbJuegoDTO dto = new IgdbJuegoDTO();
            dto.setIgdbId(((Number) game.get("id")).longValue());
            dto.setName((String) game.get("name"));
            dto.setSummary((String) game.get("summary"));

            // Cover image
            Map<String, Object> cover = (Map<String, Object>) game.get("cover");
            if (cover != null) {
                dto.setCoverUrl("https://images.igdb.com/igdb/image/upload/t_cover_big/" + cover.get("image_id") + ".jpg");
            }

            // Genres
            List<Map<String, Object>> genres = (List<Map<String, Object>>) game.get("genres");
            if (genres != null) {
                dto.setGenres(genres.stream()
                        .map(g -> (String) g.get("name"))
                        .collect(Collectors.joining(", ")));
            }

            // Release date
            Number releaseTimestamp = (Number) game.get("first_release_date");
            if (releaseTimestamp != null) {
                dto.setFirstReleaseDate(Instant.ofEpochSecond(releaseTimestamp.longValue())
                        .atZone(ZoneOffset.UTC).toLocalDate());
            }

            // Developer / Publisher
            List<Map<String, Object>> companies = (List<Map<String, Object>>) game.get("involved_companies");
            if (companies != null) {
                for (Map<String, Object> ic : companies) {
                    Map<String, Object> company = (Map<String, Object>) ic.get("company");
                    if (company == null) continue;
                    String companyName = (String) company.get("name");
                    if (Boolean.TRUE.equals(ic.get("developer")) && dto.getDeveloper() == null) {
                        dto.setDeveloper(companyName);
                    }
                    if (Boolean.TRUE.equals(ic.get("publisher")) && dto.getPublisher() == null) {
                        dto.setPublisher(companyName);
                    }
                }
            }

            // External platform IDs (category 1 = Steam, category 26 = Epic Games)
            List<Map<String, Object>> externalGames = (List<Map<String, Object>>) game.get("external_games");
            if (externalGames != null) {
                for (Map<String, Object> eg : externalGames) {
                    Object category = eg.get("category");
                    if (category == null) continue;
                    int cat = ((Number) category).intValue();
                    Object uid = eg.get("uid");
                    if (uid == null) continue;
                    if (cat == 1 && dto.getSteamAppId() == null) {
                        dto.setSteamAppId(uid.toString());
                    } else if (cat == 26 && dto.getEpicSlug() == null) {
                        dto.setEpicSlug(uid.toString());
                    }
                }
            }

            results.add(dto);
        }
        return results;
    }

}
