package com.ComparaJuegos.game_comparer.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "Juego")
public class Juego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String publisher;
    private String developer;
    private String genero;
    private String descripcion;
    private String imagen;
    private LocalDate releaseDate;

    @OneToMany(mappedBy = "juego", cascade = CascadeType.ALL)
    private List<Precio> precios = new ArrayList<>();

    @ManyToMany(mappedBy = "juegos")
    private List<Wishlist> listasDondeAparece = new ArrayList<>();
}
