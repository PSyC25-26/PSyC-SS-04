package com.ComparaJuegos.game_comparer.models;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


@Entity
@Data
@Table(name = "Usuario")
public class Usuario {

    @Id //Esto debe estar siempre, pues la BD necesita un ID para funcionar
    @GeneratedValue(strategy = GenerationType.IDENTITY)//Ahora el ID se genera eutomaticamente
    private long id;

    private LocalDate fecha_nac;
    private String name, email, contrasena, pais;

}
