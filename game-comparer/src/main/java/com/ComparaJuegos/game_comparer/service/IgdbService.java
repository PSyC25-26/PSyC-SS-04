package com.ComparaJuegos.game_comparer.service;

import com.ComparaJuegos.game_comparer.dto.IgdbJuegoDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IgdbService {

    private final IgdbTokenService tokenService;
    private final RestClient restClient = RestClient.create();

    public IgdbService(IgdbTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @SuppressWarnings("unchecked")
    public List<IgdbJuegoDTO> buscar(String query) {
        String body = "fields name, summary, cover.image_id, genres.name, first_release_date, " +
                "involved_companies.company.name, involved_companies.developer, involved_companies.publisher; " +
                "search \"" + query + "\"; limit 5;";

        List<Map<String, Object>> response = restClient.post()
                .uri("https://api.igdb.com/v4/games")
                .header("Client-ID", tokenService.getClientId())
                .header("Authorization", "Bearer " + tokenService.getToken())
                .header("Content-Type", "text/plain")
                .body(body)
                .retrieve()
                .body(List.class);

        if (response == null) return List.of();

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

            results.add(dto);
        }
        return results;
    }
}
