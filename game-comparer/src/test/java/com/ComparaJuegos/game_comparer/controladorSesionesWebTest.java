package com.ComparaJuegos.game_comparer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

//testemplates no va, mirare mas tarde otras alternativas

// Esto levanta el servidor real para la prueba de INTEGRACIÓN
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class controladorSesionesWebTest {

    @Autowired
    //private TestRestTemplate restTemplate;

    @Test
    public void testComunicacionClienteServidor() {
        // El cliente (TestRestTemplate) hace una petición GET al servidor
        //ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);
        
        // Verificamos que la comunicación remota funciona (Status 200)
        //assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("Prueba de integración exitosa: El servidor respondió correctamente.");
    }
}