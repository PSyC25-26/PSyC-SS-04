package com.ComparaJuegos.game_comparer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.EntityExchangeResult;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class controladorSesionesWebTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restClient;

    @Test
    public void testComunicacionClienteServidor() {
        // Construir RestClient apuntando al puerto aleatorio
        restClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();

        EntityExchangeResult<String> result = restClient
                .get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

        assertEquals(200, result.getStatus().value());

        String body = result.getResponseBody();
        assertNotNull(body);
        assertTrue(body.contains("<html") || body.contains("<!DOCTYPE html"));

    }

    @Test
    public void testPaginaPublicaAccesible() {
        restClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();

        EntityExchangeResult<String> result = restClient
                .get()
                .uri("/inicioSesion")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class) // Especificamos que queremos String
                .returnResult();

        assertEquals(200, result.getStatus().value());
        String body = result.getResponseBody();
        assertNotNull(body);
        assertTrue(body.contains("inicioSesion") || body.contains("Usuario"));
    }
}