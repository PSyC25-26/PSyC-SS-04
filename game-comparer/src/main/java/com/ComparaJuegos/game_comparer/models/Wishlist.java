package com.ComparaJuegos.game_comparer.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "Wishlist")
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToMany
    @JoinTable(
      name = "wishlist_juegos",
      joinColumns = @JoinColumn(name = "wishlist_id"),
      inverseJoinColumns = @JoinColumn(name = "juego_id")
    )
    private List<Juego> juegos = new ArrayList<>();
}
