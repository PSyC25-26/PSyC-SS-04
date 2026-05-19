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

/**
 * @brief Servicio principal de búsqueda y gestión de juegos.
 *
 * Orquesta la búsqueda combinando tres fuentes de datos:
 * - {@link IgdbService}: metadatos del juego (nombre, portada, géneros, etc.)
 * - {@link CheapSharkService}: precios de Steam y Epic desde el agregador CheapShark.
 * - {@link SteamService}: fallback directo a Steam Store cuando CheapShark no tiene precio.
 *
 * También gestiona la lógica de wishlist: agregar, eliminar juegos y actualizar
 * el historial de precios en base de datos.
 */
@Service
public class BusquedaService {

    private final IgdbService igdbService;
    private final CheapSharkService cheapSharkService;
    private final SteamService steamService;
    private final JuegoRepositorio juegoRepositorio;
    private final WishlistRepositorio wishlistRepositorio;

    /**
     * @brief Constructor principal con inyección de dependencias.
     * @param igdbService        Servicio para búsqueda de metadatos de juegos.
     * @param cheapSharkService  Servicio para obtener precios desde CheapShark.
     * @param steamService       Servicio para consultar la API de Steam directamente.
     * @param juegoRepositorio   Repositorio JPA para la entidad Juego.
     * @param wishlistRepositorio Repositorio JPA para la entidad Wishlist.
     */
    public BusquedaService(IgdbService igdbService, CheapSharkService cheapSharkService,
            SteamService steamService,
            JuegoRepositorio juegoRepositorio, WishlistRepositorio wishlistRepositorio) {
        this.igdbService = igdbService;
        this.cheapSharkService = cheapSharkService;
        this.steamService = steamService;
        this.juegoRepositorio = juegoRepositorio;
        this.wishlistRepositorio = wishlistRepositorio;
    }

    /**
     * @brief Busca juegos por nombre y devuelve sus precios en Steam y Epic.
     *
     * Flujo de precios para Steam:
     * 1. Precio de CheapShark (storeID=1).
     * 2. Si no hay: Steam App ID de CheapShark o IGDB → API de Steam Store.
     * 3. Último recurso: búsqueda por nombre en Steam Store.
     *
     * Flujo de precios para Epic:
     * 1. Precio de CheapShark (storeID=25).
     * 2. Si no hay precio pero IGDB tiene epicSlug → se proporciona al menos el enlace a Epic Store.
     *
     * @param query Texto de búsqueda introducido por el usuario.
     * @return Lista de resultados con metadatos y precios. Nunca null.
     */
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

    /**
     * @brief Agrega un juego a la wishlist del usuario.
     *
     * Si el juego ya existe en la base de datos, actualiza sus precios y registra
     * el precio anterior en el historial. Si no existe, lo crea con sus precios actuales.
     * Solo agrega el juego a la wishlist si aún no está en ella.
     *
     * @param dto        DTO con los datos del juego a agregar.
     * @param wishlistId ID de la wishlist destino.
     * @return ID de la wishlist (para redirección post-acción).
     */
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

    /**
     * @brief Elimina un juego de la wishlist del usuario.
     *
     * @param wishlistId ID de la wishlist de la que se quiere eliminar el juego.
     * @param juegoId    ID del juego a eliminar.
     * @throws RuntimeException si el juego no existe en la base de datos.
     */
    @Transactional
    public void eliminarDeWishlist(Long wishlistId, Long juegoId) {
        Wishlist wishlist = wishlistRepositorio.findById(wishlistId).orElseThrow();
        Juego juegoElim = juegoRepositorio.findById(juegoId).orElseThrow(() -> new RuntimeException("Juego no encontrado, lo siento!"));

        if (wishlist.getJuegos().contains(juegoElim)) {
            wishlist.getJuegos().remove(juegoElim);
        }

        wishlistRepositorio.save(wishlist);
    }

    /**
     * @brief Añade un precio a un juego nuevo (sin historial previo).
     *
     * Si el precio es null (tienda no disponible), no crea ninguna entrada.
     *
     * @param juego       Entidad Juego a la que añadir el precio.
     * @param tienda      Tienda (STEAM o EPIC).
     * @param precioValor Precio en euros, o null si no está disponible.
     * @param url         URL de la oferta en la tienda.
     */
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

    /**
     * @brief Actualiza el precio de una tienda en un juego existente, registrando el historial.
     *
     * Si ya existe un precio para esa tienda, guarda el valor anterior en {@link HistorialPrecios}
     * antes de actualizarlo. Si no existe, crea la entrada de precio nueva.
     * Si el nuevo precio es null, no hace nada.
     *
     * @param juego    Entidad Juego a actualizar.
     * @param tienda   Tienda cuyo precio se actualiza (STEAM o EPIC).
     * @param newPrice Nuevo precio en euros, o null para no actualizar.
     * @param newUrl   Nueva URL de la oferta.
     */
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

    /**
     * @brief Obtiene una selección aleatoria de ofertas para mostrar en la página de inicio.
     *
     * Busca juegos con la query "ato" en IGDB para obtener un conjunto variado,
     * filtra los que tienen precio válido en Steam o Epic, y devuelve hasta 10
     * resultados en orden aleatorio.
     *
     * @return Lista de hasta 10 ofertas aleatorias con precio válido.
     */
    public List<OfertasHomeDTO> getOfertasHome() {

        List<IgdbJuegoDTO> juegos = igdbService.buscar("ato");

        List<OfertasHomeDTO> ofertas = new ArrayList<>();

        for (IgdbJuegoDTO igdb : juegos) {

            CheapSharkPrecioDTO price =
                    cheapSharkService.buscarPrecios(igdb.getName());

            if (price == null) {
                continue;
            }

            Double finalPrice = null;

            if (price.getSteamPrice() != null) {
                finalPrice = price.getSteamPrice();
            } else if (price.getEpicPrice() != null) {
                finalPrice = price.getEpicPrice();
            }

            if (finalPrice == null) {
                continue;
            }

            OfertasHomeDTO dto = new OfertasHomeDTO();
            dto.setName(igdb.getName());
            dto.setImage(igdb.getCoverUrl());
            dto.setSteamUrl(price.getSteamUrl());
            dto.setEpicUrl(price.getEpicUrl());
            dto.setPrice(finalPrice);

            ofertas.add(dto);
        }

        Collections.shuffle(ofertas);

        return ofertas.stream().limit(10).toList();
    }
}
