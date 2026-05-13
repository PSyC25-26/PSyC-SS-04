package com.ComparaJuegos.game_comparer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.ComparaJuegos.game_comparer.controladores.BusquedaControlador;
import com.ComparaJuegos.game_comparer.dto.ResultadoBusquedaDTO;
import com.ComparaJuegos.game_comparer.models.Usuario;
import com.ComparaJuegos.game_comparer.models.Wishlist;
import com.ComparaJuegos.game_comparer.service.BusquedaService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.EntityExchangeResult;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class LibraryIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restClient;

    // Steps to follow:
    // 1 Se crea un usuario
    // 2 Se crea una wishlist
    // 3 Se añade un juego aleatorio a la wishlist
    // 4 Se comprueba que el juego se ha añadido a la wishlist
    // 5 Se elimina el juego de la wishlist
    // 6 Se comprueba que el juego se ha eliminado de la wishlist
    // 7 Se elimina la wishlist y el usuario creado

    @Test
    void testUserAddsAndRemovesFromWishlist() {
        restClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();

        EntityExchangeResult<String> result = restClient
                .get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

        assertEquals(200, result.getStatus().value());

        // Step 1: Create a user
        Usuario usuario = new Usuario();
        ResponseEntity<User> userResponse = restTemplate.postForEntity("/api/library/users/register", user, User.class);
        assertEquals(HttpStatus.CREATED, userResponse.getStatusCode());
        User createdUser = userResponse.getBody();
        assertNotNull(createdUser);
        Long userId = createdUser.getId();

        // 2 Se crea una wishlist
        Book book = new Book("Spring Boot in Action", "anonymous");
        ResponseEntity<Book> bookResponse = restTemplate.postForEntity("/api/library/books", book, Book.class);
        assertEquals(HttpStatus.OK, bookResponse.getStatusCode());
        Book createdBook = bookResponse.getBody();
        assertNotNull(createdBook);
        Long bookId = createdBook.getId();

        // 3 Se añade un juego aleatorio a la wishlist
        ResponseEntity<Void> borrowResponse = restTemplate.exchange(
                "/api/library/books/borrow/" + bookId + "/users/" + userId, HttpMethod.POST, null, Void.class);
        assertEquals(HttpStatus.OK, borrowResponse.getStatusCode());

        // 4 Se comprueba que el juego se ha añadido a la wishlist
        ResponseEntity<List<Book>> borrowedBookResponse = restTemplate.exchange(
                "/api/library/books",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Book>>() {
                });

        // Ensure the response is OK
        assertEquals(HttpStatus.OK, borrowedBookResponse.getStatusCode());

        // Get the list of books
        List<Book> books = borrowedBookResponse.getBody();
        assertNotNull(books);
        assertFalse(books.isEmpty());

        // Find the borrowed book
        // Find the borrowed book using a traditional loop
        Book borrowedBook = null;
        for (Book bookTemp : books) {
            if (bookTemp.getId().equals(bookId)) {
                borrowedBook = bookTemp;
                break; // Exit loop once the book is found
            }
        }

        // Ensure the book is found and is borrowed
        assertNotNull(borrowedBook);
        assertNotNull(borrowedBook.getBorrower());

        // 5 Se elimina el juego de la wishlist
        ResponseEntity<Void> returnResponse = restTemplate.exchange(
                "/api/library/books/return/" + bookId, HttpMethod.POST, null, Void.class);
        assertEquals(HttpStatus.OK, returnResponse.getStatusCode());

        // 6 Se comprueba que el juego se ha eliminado de la wishlist
        ResponseEntity<List<Book>> returnedBookResponse = restTemplate.exchange(
                "/api/library/books",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Book>>() {
                });
        // Ensure the response is OK
        assertEquals(HttpStatus.OK, returnedBookResponse.getStatusCode());

        // Get the list of books
        books = returnedBookResponse.getBody();
        assertNotNull(books);
        assertFalse(books.isEmpty());

        // Find the borrowed book
        // Find the borrowed book using a traditional loop
        Book returnedBook = null;
        for (Book bookTemp : books) {
            if (bookTemp.getId().equals(bookId)) {
                returnedBook = bookTemp;
                break; // Exit loop once the book is found
            }
        }

        assertNotNull(returnedBook);
        assertTrue(returnedBook.getBorrower() == null);

        // 7 Se elimina la wishlist y el usuario creado
        ResponseEntity<Void> borrowingDeleteResponse = restTemplate.exchange(
                "/api/library/books/" + bookId + "/users/" + userId, HttpMethod.DELETE, null, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, borrowingDeleteResponse.getStatusCode());
    }
}