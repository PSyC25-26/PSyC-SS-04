package com.ComparaJuegos.game_comparer;

/**
 * @file GameComparerApplicationTests.java
 * @brief Clase de prueba para verificar que el contexto de la aplicación se carga correctamente.
 * @author Equipo Caza Ofertas Gaming (COG)
 * 
 * @details Este test comprueba que el contexto de Spring Boot se levante sin errores,
 *          asegurando que todas las dependencias y configuraciones están correctamente
 *          definidas. Es una prueba de integridad básica del proyecto.
 */

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class GameComparerApplicationTests {

	@Test
	void contextLoads() {
	}
}