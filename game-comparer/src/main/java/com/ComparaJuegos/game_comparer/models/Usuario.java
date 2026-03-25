package com.ComparaJuegos.game_comparer.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "Usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha_nac;
    private String name;

    @Column(unique = true)
    private String email;

    private String contrasena;
    private String pais;

    @Enumerated(EnumType.STRING)
    private Rol rol = Rol.USER;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Wishlist> wishlists = new ArrayList<>();
}
