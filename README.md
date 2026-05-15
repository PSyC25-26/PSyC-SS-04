![Java CI](https://github.com/PSyC25-26/PSyC-SS-04/actions/workflows/ci-java.yml/badge.svg)

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
- **Docker** — contenerización de la aplicación y la base de datos

---

## Requisitos previos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y en ejecución
- Credenciales de la [IGDB API](https://api-docs.igdb.com/#getting-started) (Client ID y Client Secret de Twitch Developer)

---

## Configuración
Crea un archivo de variables de entorno copiando la plantilla de ejemplo:

```bash
cp .env.example .env
```

Edita el `.env` con tus datos:

```env
DB_NAME=comparajuegos
DB_ROOT_PASSWORD=tu_contraseña_root
DB_USERNAME=tu_usuario
DB_PASSWORD=tu_contraseña
 
IGDB_CLIENT_ID=tu_client_id
IGDB_CLIENT_SECRET=tu_client_secret
```

> El archivo `.env` está en `.gitignore` y nunca se sube al repositorio.

---

## Ejecutar el proyecto
Docker levanta la aplicación y la base de datos MySQL juntas, sin necesidad de instalar Java, Maven ni MySQL.

**1. Desde la carpeta `game-comparer/`, ejecuta:**

```bash
docker compose up --build
```

**2. Abre a la aplicación en:** [http://localhost:8080](http://localhost:8080)

> La base de datos MySQL usa el puerto `localhost:3307` si quieres usar MySQL Workbench.

### Detener los contenedores

```bash
docker compose down
```

Los datos de la base de datos se conservan entre reinicios gracias al volumen `mysql-data`. Para borrarlos usa:

```bash
docker compose down --volumes
```

---

## Ejecutar los tests

El proyecto tiene tres niveles de tests. Todos se ejecutan desde la carpeta `game-comparer/`.

### Tests unitarios y de controlador

No requieren base de datos ni Docker.

```bash
./mvnw test
```

### Tests de rendimiento

Miden la latencia y el throughput de los servicios bajo carga concurrente. No requieren base de datos ni Docker (usan mocks).

```bash
./mvnw test -Pperformance
```

### Tests de integración

Arrancan la aplicación completa contra la base de datos real. Requieren Docker en ejecución.

**1. Configura el perfil de test** (solo la primera vez):

```bash
cp src/test/resources/application-test.properties.example src/test/resources/application-test.properties
```

Edita el archivo y rellena los valores de tu `.env`:

```properties
spring.datasource.password=YOUR_DB_ROOT_PASSWORD
igdb.client-id=YOUR_IGDB_CLIENT_ID
igdb.client-secret=YOUR_IGDB_CLIENT_SECRET
```

> `application-test.properties` está en `.gitignore` y nunca se sube al repositorio.

**2. Levanta la base de datos:**

```bash
docker compose up -d
```

**3. Ejecuta el test:**

```bash
./mvnw test -Dtest=WishlistIntegrationTest
```

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
