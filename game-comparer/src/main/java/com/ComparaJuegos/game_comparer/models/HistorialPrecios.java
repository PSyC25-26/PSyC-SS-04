package com.ComparaJuegos.game_comparer.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "HistorialPrecios")
public class HistorialPrecios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double precio;
    private LocalDateTime fecha;

    @ManyToOne
    @JoinColumn(name = "precio_id")
    private Precio precio_ref;
}
