package com.ComparaJuegos.game_comparer.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "Juego")
public class Juego {

    @Id //Esto debe estar siempre, pues la BD necesita un ID para funcionar
    @GeneratedValue(strategy = GenerationType.IDENTITY)//Ahora el ID se genera eutomaticamente
    private long id;

    private String name;
    private String publisher;
    private String developer;
    private String distributionPlatform;
    private double price;
    private java.time.LocalDate releaseDate;
    //additional content es un dato no especificable, al no ser necesaria se queda asi de momento

    //varios juegos a varios wishlist y viceversa
    @ManyToMany(mappedBy = "juegos")
    private java.util.List<Wishlist> listasDondeAparece;
    
    
}
