# Guía de uso

## Páginas y navegación

| Ruta | Vista | Acceso |
|------|-------|--------|
| `/` o `/iniciar` | Página principal con botones de registro/login | Público |
| `/registro` | Formulario de registro | Público |
| `/inicioSesion` | Formulario de login | Público |
| `/buscar` | Buscador de juegos con precios | Autenticado |
| `/perfil` | Panel de usuario y gestión de wishlists | Autenticado |
| `/wishlist/{id}` | Detalle de una wishlist con precios actualizados | Autenticado |

---

## Flujo de usuario

Tras hacer login, el usuario es redirigido a `/buscar`.
Todas las vistas autenticadas incluyen una barra de navegación con acceso a:

- **Buscar** — buscador de juegos
- **Mi Perfil** — gestión de wishlists
- **Cerrar Sesión**

---

## Buscar juegos

En la vista `/buscar` el usuario puede introducir el nombre de un juego y ver:

- Metadatos del juego (portada, género, descripción, fecha de lanzamiento) desde **IGDB**
- Precio actual en **Steam**
- Precio actual en **Epic Games Store**

---

## Wishlists

Desde `/perfil` el usuario puede:

- Crear wishlists personalizadas
- Añadir juegos a una wishlist desde la búsqueda
- Ver el detalle de cada wishlist en `/wishlist/{id}` con los precios actualizados en tiempo real
- Eliminar juegos de una wishlist
