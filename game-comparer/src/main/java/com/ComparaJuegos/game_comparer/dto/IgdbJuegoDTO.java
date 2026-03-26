package com.ComparaJuegos.game_comparer.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class IgdbJuegoDTO {
    private long igdbId;
    private String name;
    private String summary;
    private String coverUrl;
    private String genres;
    private LocalDate firstReleaseDate;
    private String developer;
    private String publisher;
}
