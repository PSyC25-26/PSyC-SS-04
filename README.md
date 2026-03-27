# Caza Ofertas Gaming

Comparador de precios de videojuegos con sistema de wishlists. Permite buscar juegos, ver su precio en Steam y Epic Games Store en tiempo real, y guardarlos en listas personalizadas.

---

## Tecnologías

- **Java 21** + **Spring Boot 4.0.4**
- **Spring Security** — autenticación y control de acceso
- **Spring Data JPA** + **Hibernate** — persistencia
- **MySQL** — base de datos
- **Thymeleaf** — motor de plantillas HTML
- **Lombok** — reducción de boilerplate
- **IGDB API** — metadatos de juegos (género, descripción, fecha de lanzamiento, portada)
- **CheapShark API** — precios de tiendas (Steam, Epic Games Store)

---

## Requisitos previos

- Java 21 o superior
- Maven (o usar el wrapper incluido `./mvnw`)
- MySQL 8 corriendo en `localhost:3306`
- Credenciales de la [IGDB API](https://api-docs.igdb.com/#getting-started) (Client ID y Client Secret de Twitch Developer)

---

## Configuración

Editar `game-comparer/src/main/resources/application.properties`:

```properties
# Base de datos
spring.datasource.url=jdbc:mysql://localhost:3306/mi_app_db
spring.datasource.username=root
spring.datasource.password=TU_CONTRASEÑA

# IGDB (Twitch Developer)
igdb.client-id=TU_CLIENT_ID
igdb.client-secret=TU_CLIENT_SECRET
```

Crear la base de datos en MySQL antes de arrancar:

```sql
CREATE DATABASE mi_app_db;
```

Las tablas se crean automáticamente al iniciar la aplicación (`ddl-auto=update`).

---

## Ejecución

Desde la carpeta `game-comparer/`:

```bash
# Con el wrapper de Maven (recomendado, no necesita Maven instalado)
./mvnw spring-boot:run

# Con Maven instalado globalmente
mvn spring-boot:run
```

La aplicación arranca en: **http://localhost:8080**

---

## Páginas y navegación

| Ruta | Vista | Acceso |
|------|-------|--------|
| `/` o `/iniciar` | Página principal con botones de registro/login | Público |
| `/registro` | Formulario de registro | Público |
| `/inicioSesion` | Formulario de login | Público |
| `/buscar` | Buscador de juegos con precios | Autenticado |
| `/perfil` | Panel de usuario y gestión de wishlists | Autenticado |
| `/wishlist/{id}` | Detalle de una wishlist con precios actualizados | Autenticado |

Tras hacer login, el usuario es redirigido a `/buscar`. Todas las vistas autenticadas incluyen una barra de navegación con acceso a Buscar, Mi Perfil y Cerrar Sesión.

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
    │   ├── plantilla.html                # Fragmentos reutilizables (header, nav, footer)
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

## APIs externas

### IGDB
Proporciona metadatos de juegos: nombre, género, descripción, fecha de lanzamiento y portada. Requiere autenticación OAuth2 con Twitch. El token se gestiona automáticamente mediante `IgdbTokenService`.

### CheapShark
Proporciona precios actuales de juegos en Steam y Epic Games Store. No requiere API key.
