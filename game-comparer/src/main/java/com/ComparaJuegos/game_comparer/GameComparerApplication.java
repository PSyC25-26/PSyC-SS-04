package com.ComparaJuegos.game_comparer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class GameComparerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GameComparerApplication.class, args);
	}

}
