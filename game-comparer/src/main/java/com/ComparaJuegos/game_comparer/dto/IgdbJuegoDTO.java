package com.ComparaJuegos.game_comparer.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * @brief DTO con los metadatos de un juego obtenidos de la API de IGDB.
 *
 * Contiene la información descriptiva del juego (nombre, descripción, portada, etc.)
 * y los identificadores en plataformas externas necesarios para obtener precios:
 * Steam App ID y Epic Games slug.
 */
@Data
public class IgdbJuegoDTO {

    /** @brief ID interno del juego en IGDB. */
    private long igdbId;

    /** @brief Nombre oficial del juego. */
    private String name;

    /** @brief Descripción o sinopsis del juego. */
    private String summary;

    /** @brief URL de la imagen de portada en el CDN de IGDB. */
    private String coverUrl;

    /** @brief Géneros del juego separados por comas (p.ej. "RPG, Adventure"). */
    private String genres;

    /** @brief Fecha de lanzamiento del juego. */
    private LocalDate firstReleaseDate;

    /** @brief Nombre del estudio desarrollador. */
    private String developer;

    /** @brief Nombre del publisher. */
    private String publisher;

    /**
     * @brief Steam App ID del juego (external_games category=1 en IGDB).
     *
     * Se usa como fallback para consultar precios directamente en la API
     * de Steam Store cuando CheapShark no tiene datos.
     */
    private String steamAppId;

    /**
     * @brief Identificador del juego en Epic Games Store (external_games category=26 en IGDB).
     *
     * Se usa para construir el enlace directo a la página del juego en Epic Store
     * cuando CheapShark no tiene precio para esa plataforma.
     */
    private String epicSlug;
}
