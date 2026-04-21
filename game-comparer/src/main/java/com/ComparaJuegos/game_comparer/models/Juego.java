package com.ComparaJuegos.game_comparer.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "juego")
public class Juego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String publisher;
    private String developer;
    private String genero;

    @Column(name = "price")
    private Double price = 0.0;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private String imagen;
    private LocalDate releaseDate;


    @OneToMany(mappedBy = "juego", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Precio> precios = new ArrayList<>();

    @ManyToMany(mappedBy = "juegos")
    private List<Wishlist> listasDondeAparece = new ArrayList<>();
}
