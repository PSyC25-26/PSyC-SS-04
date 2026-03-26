package com.ComparaJuegos.game_comparer.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "Precio")
public class Precio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Tienda tienda;

    private double precio;
    private String url;
    private LocalDateTime fechaActualizacion;

    @ManyToOne
    @JoinColumn(name = "juego_id")
    private Juego juego;

    @OneToMany(mappedBy = "precio", cascade = CascadeType.ALL)
    private List<HistorialPrecios> historial = new ArrayList<>();
}
