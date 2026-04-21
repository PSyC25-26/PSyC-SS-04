package com.ComparaJuegos.game_comparer.service;

import com.ComparaJuegos.game_comparer.JuegoRepositorio;
import com.ComparaJuegos.game_comparer.WishlistRepositorio;
import com.ComparaJuegos.game_comparer.dto.CheapSharkPrecioDTO;
import com.ComparaJuegos.game_comparer.dto.IgdbJuegoDTO;
import com.ComparaJuegos.game_comparer.dto.OfertasHomeDTO;
import com.ComparaJuegos.game_comparer.dto.ResultadoBusquedaDTO;
import com.ComparaJuegos.game_comparer.models.HistorialPrecios;
import com.ComparaJuegos.game_comparer.models.Juego;
import com.ComparaJuegos.game_comparer.models.Precio;
import com.ComparaJuegos.game_comparer.models.Tienda;
import com.ComparaJuegos.game_comparer.models.Wishlist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class BusquedaService {

    private final IgdbService igdbService;
    private final CheapSharkService cheapSharkService;
    private final JuegoRepositorio juegoRepositorio;
    private final WishlistRepositorio wishlistRepositorio;

    public BusquedaService(IgdbService igdbService, CheapSharkService cheapSharkService,
            JuegoRepositorio juegoRepositorio, WishlistRepositorio wishlistRepositorio) {
        this.igdbService = igdbService;
        this.cheapSharkService = cheapSharkService;
        this.juegoRepositorio = juegoRepositorio;
        this.wishlistRepositorio = wishlistRepositorio;
    }

    public List<ResultadoBusquedaDTO> buscar(String query) {
        List<IgdbJuegoDTO> igdbResults = igdbService.buscar(query);
        List<ResultadoBusquedaDTO> results = new ArrayList<>();

        for (IgdbJuegoDTO igdb : igdbResults) {
            CheapSharkPrecioDTO prices = cheapSharkService.buscarPrecios(igdb.getName());
            ResultadoBusquedaDTO dto = new ResultadoBusquedaDTO();
            dto.setName(igdb.getName());
            dto.setDescripcion(igdb.getSummary());
            dto.setImagen(igdb.getCoverUrl());
            dto.setGenero(igdb.getGenres());
            dto.setReleaseDate(igdb.getFirstReleaseDate());
            dto.setDeveloper(igdb.getDeveloper());
            dto.setPublisher(igdb.getPublisher());
            dto.setSteamPrice(prices.getSteamPrice());
            dto.setSteamUrl(prices.getSteamUrl());
            dto.setEpicPrice(prices.getEpicPrice());
            dto.setEpicUrl(prices.getEpicUrl());
            results.add(dto);
        }
        return results;
    }

    @Transactional
    public Long agregarAWishlist(ResultadoBusquedaDTO dto, Long wishlistId) {
        Optional<Juego> existente = juegoRepositorio.findFirstByNameIgnoreCase(dto.getName());

        Juego juego;
        if (existente.isEmpty()) {
            juego = new Juego();
            juego.setName(dto.getName());
            juego.setDescripcion(dto.getDescripcion());
            juego.setImagen(dto.getImagen());
            juego.setGenero(dto.getGenero());
            juego.setReleaseDate(dto.getReleaseDate());
            juego.setDeveloper(dto.getDeveloper());
            juego.setPublisher(dto.getPublisher());

            addPrecio(juego, Tienda.STEAM, dto.getSteamPrice(), dto.getSteamUrl());
            addPrecio(juego, Tienda.EPIC, dto.getEpicPrice(), dto.getEpicUrl());
            juego = juegoRepositorio.save(juego);
        } else {
            juego = existente.get();
            updateOrCreatePrecio(juego, Tienda.STEAM, dto.getSteamPrice(), dto.getSteamUrl());
            updateOrCreatePrecio(juego, Tienda.EPIC, dto.getEpicPrice(), dto.getEpicUrl());
            juego = juegoRepositorio.save(juego);
        }

        Wishlist wishlist = wishlistRepositorio.findById(wishlistId).orElseThrow();
        if (!wishlist.getJuegos().contains(juego)) {
            wishlist.getJuegos().add(juego);
            wishlistRepositorio.save(wishlist);
        }

        return wishlistId;
    }

    private void addPrecio(Juego juego, Tienda tienda, Double precioValor, String url) {
        if (precioValor == null) {
            return;
        }

        Precio precio = new Precio();
        precio.setTienda(tienda);
        precio.setPrecio(precioValor);
        precio.setUrl(url);
        precio.setFechaActualizacion(LocalDateTime.now());
        precio.setJuego(juego);
        juego.getPrecios().add(precio);
    }

    private void updateOrCreatePrecio(Juego juego, Tienda tienda, Double newPrice, String newUrl) {
        if (newPrice == null)
            return;

        Optional<Precio> existing = juego.getPrecios().stream()
                .filter(p -> p.getTienda() == tienda)
                .findFirst();

        if (existing.isPresent()) {
            Precio precio = existing.get();
            // Record history before updating
            HistorialPrecios historial = new HistorialPrecios();
            historial.setPrecio(precio.getPrecio());
            historial.setFecha(LocalDateTime.now());
            historial.setPrecio_ref(precio);
            precio.getHistorial().add(historial);
            // Update current price
            precio.setPrecio(newPrice);
            precio.setUrl(newUrl);
            precio.setFechaActualizacion(LocalDateTime.now());
        } else {
            Precio precio = new Precio();
            precio.setTienda(tienda);
            precio.setPrecio(newPrice);
            precio.setUrl(newUrl);
            precio.setFechaActualizacion(LocalDateTime.now());
            precio.setJuego(juego);
            juego.getPrecios().add(precio);
        }
    }
    public List<OfertasHomeDTO> getOfertasHome() {

        List<IgdbJuegoDTO> juegos = igdbService.buscar("popular");

        List<OfertasHomeDTO> ofertas = new ArrayList<>();

        for (IgdbJuegoDTO igdb : juegos) {

            CheapSharkPrecioDTO price =
                    cheapSharkService.buscarPrecios(igdb.getName());

            OfertasHomeDTO dto = new OfertasHomeDTO();

            dto.setName(igdb.getName());
            dto.setImage(igdb.getCoverUrl());
            Double finalPrice = null;

            if (price != null && price.getSteamPrice() != null) {
                finalPrice = price.getSteamPrice();
            } else if (price != null && price.getEpicPrice() != null) {
                finalPrice = price.getEpicPrice();
            }

            if (finalPrice == null) {
                continue; // no hay oferta válida
            }

            dto.setPrice(finalPrice);



            ofertas.add(dto);
        }

        Collections.shuffle(ofertas);

        return ofertas.stream().limit(10).toList();
    }
}
