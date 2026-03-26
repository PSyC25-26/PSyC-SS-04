package com.ComparaJuegos.game_comparer.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ResultadoBusquedaDTO {
    private String name;
    private String descripcion;
    private String imagen;
    private String genero;
    private LocalDate releaseDate;
    private String developer;
    private String publisher;
    private Double steamPrice;
    private String steamUrl;
    private Double epicPrice;
    private String epicUrl;
}
