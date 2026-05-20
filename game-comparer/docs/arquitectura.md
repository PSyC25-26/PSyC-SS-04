# Arquitectura

## Tecnologías

| Capa                  | Tecnología                  |
|-----------------------|-----------------------------|
| Lenguaje              | Java 21                     |
| Framework             | Spring Boot 4.0.4           |
| Seguridad             | Spring Security             |
| Persistencia          | Spring Data JPA + Hibernate |
| Base de datos         | MySQL                       |
| Plantillas            | Thymeleaf                   |
| Reducción boilerplate | Lombok                      |
| Contenerización       | Docker                      |
| Documentación         | Sphinx + Doxygen            |

---

## Estructura del proyecto

```
game-comparer/
├── src/main/java/com/ComparaJuegos/game_comparer/
│   ├── Config/
│   │   └── SeguridadConfig.java          # Configuración Spring Security
│   ├── controladores/
│   │   ├── controladorSesiones.java      # Rutas de auth, perfil y wishlists
│   │   └── BusquedaControlador.java      # Búsqueda y detalle de wishlist
│   ├── models/
│   │   ├── Usuario.java
│   │   ├── Wishlist.java
│   │   ├── Juego.java
│   │   ├── Precio.java
│   │   ├── Tienda.java
│   │   ├── Rol.java
│   │   └── HistorialPrecios.java
│   ├── service/
│   │   ├── BusquedaService.java          # Orquesta IGDB + CheapShark
│   │   ├── IgdbService.java              # Integración IGDB API
│   │   ├── CheapSharkService.java        # Integración CheapShark API
│   │   ├── IgdbTokenService.java         # Gestión del token OAuth de IGDB
│   │   └── UsuarioService.java           # Lógica de usuario (UserDetailsService)
│   ├── dto/
│   │   ├── ResultadoBusquedaDTO.java     # DTO de resultado unificado
│   │   ├── IgdbJuegoDTO.java
│   │   └── CheapSharkPrecioDTO.java
│   ├── *Repositorio.java                 # Interfaces JPA
│   └── GameComparerApplication.java
│
└── src/main/resources/
    ├── templates/
    │   ├── plantilla.html                # Fragmentos reutilizables
    │   ├── principal.html
    │   ├── inicioSesion.html
    │   ├── registro.html
    │   ├── buscar.html
    │   ├── perfil.html
    │   └── detalle-wishlist.html
    ├── static/
    │   └── estilos.css
    └── application.properties
```

---

## Flujo de búsqueda de precios

### Steam

1. Precio de CheapShark (storeID=1)
2. Si no hay: Steam App ID de CheapShark o IGDB → API de Steam Store
3. Último recurso: búsqueda por nombre en Steam Store

### Epic Games Store

1. Precio de CheapShark (storeID=25)
2. Si no hay precio pero IGDB tiene `epicSlug` → se proporciona el enlace a Epic Store

---

## Contenedores Docker

El proyecto usa `docker compose` con dos servicios:

- **mysql** — base de datos MySQL 8.4, con healthcheck y volumen persistente
- **app** — aplicación Spring Boot, espera a que MySQL esté sano antes de arrancar
