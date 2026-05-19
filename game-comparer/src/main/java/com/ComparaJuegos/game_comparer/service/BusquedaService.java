package com.ComparaJuegos.game_comparer.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class BusquedaService {

    private final IgdbService igdbService;
    private final CheapSharkService cheapSharkService;
    private final SteamService steamService;
    private final JuegoRepositorio juegoRepositorio;
    private final WishlistRepositorio wishlistRepositorio;

    public BusquedaService(IgdbService igdbService, CheapSharkService cheapSharkService,
            SteamService steamService,
            JuegoRepositorio juegoRepositorio, WishlistRepositorio wishlistRepositorio) {
        this.igdbService = igdbService;
        this.cheapSharkService = cheapSharkService;
        this.steamService = steamService;
        this.juegoRepositorio = juegoRepositorio;
        this.wishlistRepositorio = wishlistRepositorio;
    }

    public List<ResultadoBusquedaDTO> buscar(String query) {
        List<IgdbJuegoDTO> igdbResults = igdbService.buscar(query);
        List<ResultadoBusquedaDTO> results = new ArrayList<>();

        for (IgdbJuegoDTO igdb : igdbResults) {
            CheapSharkPrecioDTO prices = cheapSharkService.buscarPrecios(igdb.getName());

            // Fallback: if CheapShark has no Steam price, try Steam API directly
            if (prices.getSteamPrice() == null) {
                String appId = prices.getSteamAppId() != null ? prices.getSteamAppId() : igdb.getSteamAppId();
                // Last resort: search Steam store by game name
                if (appId == null) {
                    appId = steamService.findAppIdByName(igdb.getName());
                }
                if (appId != null) {
                    Double steamPrice = steamService.getSteamPrice(appId);
                    if (steamPrice != null) {
                        prices.setSteamPrice(steamPrice);
                        prices.setSteamUrl(steamService.getSteamUrl(appId));
                    }
                }
            }

            // Fallback: if CheapShark has no Epic URL but IGDB gave us an Epic slug, provide a store link
            if (prices.getEpicUrl() == null && igdb.getEpicSlug() != null) {
                prices.setEpicUrl("https://store.epicgames.com/en-US/p/" + igdb.getEpicSlug());
            }

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
        if (existente.isEmpty()) {//Comprueba si existe en la BD, si no existe lo crea
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
        } else {//Si existe lo consigue
            juego = existente.get();
            updateOrCreatePrecio(juego, Tienda.STEAM, dto.getSteamPrice(), dto.getSteamUrl());
            updateOrCreatePrecio(juego, Tienda.EPIC, dto.getEpicPrice(), dto.getEpicUrl());
            juego = juegoRepositorio.save(juego);
        }

        Wishlist wishlist = wishlistRepositorio.findById(wishlistId).orElseThrow();//Consigue la wishlist deseada
        if (!wishlist.getJuegos().contains(juego)) {
            wishlist.getJuegos().add(juego);//Le metemos el juego en cuestion
            wishlistRepositorio.save(wishlist);
        }

        return wishlistId;
    }

    @Transactional
    public void eliminarDeWishlist(Long wishlistId, Long juegoId){//Tengo que coger mi wishlist y el juego, de ahí comprobar si el juego existe, y despues eliminarlo
        Wishlist wishlist = wishlistRepositorio.findById(wishlistId).orElseThrow();
        Juego juegoElim = juegoRepositorio.findById(juegoId).orElseThrow(() -> new RuntimeException("Juego no encontrado, lo siento!"));

        if (wishlist.getJuegos().contains(juegoElim)){
            wishlist.getJuegos().remove(juegoElim);//Eliminamos el juego de la wishlist
        };

        wishlistRepositorio.save(wishlist);//Actualizamos y guardamos la wishlist

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

        List<IgdbJuegoDTO> juegos = igdbService.buscar("ato");

        List<OfertasHomeDTO> ofertas = new ArrayList<>();

        for (IgdbJuegoDTO igdb : juegos) {

            CheapSharkPrecioDTO price =
                    cheapSharkService.buscarPrecios(igdb.getName());

            OfertasHomeDTO dto = new OfertasHomeDTO();

            dto.setName(igdb.getName());
            dto.setImage(igdb.getCoverUrl());
            dto.setSteamUrl(price.getSteamUrl());
            dto.setEpicUrl(price.getEpicUrl());

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
