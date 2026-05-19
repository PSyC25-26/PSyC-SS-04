package com.ComparaJuegos.game_comparer.dto;

import lombok.Data;

/**
 * @brief DTO con los precios obtenidos de la API de CheapShark.
 *
 * Los campos de precio son null cuando la tienda correspondiente no tiene
 * datos disponibles para el juego (no lo vende o nunca ha estado en oferta).
 * Un precio de 0.0 indica que el juego es gratuito (free-to-play).
 */
@Data
public class CheapSharkPrecioDTO {

    /** @brief Precio actual en Steam en euros, o null si no disponible. */
    private Double steamPrice;

    /** @brief URL de la oferta en Steam (redireccionada a través de CheapShark). */
    private String steamUrl;

    /** @brief Precio actual en Epic Games Store en euros, o null si no disponible. */
    private Double epicPrice;

    /** @brief URL de la oferta en Epic (redireccionada a través de CheapShark). */
    private String epicUrl;

    /**
     * @brief Steam App ID extraído de los resultados de búsqueda de CheapShark.
     *
     * Se usa como fuente alternativa al App ID de IGDB para el fallback
     * a la API directa de Steam Store.
     */
    private String steamAppId;
}
