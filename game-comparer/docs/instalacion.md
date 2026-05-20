# Instalación y puesta en marcha

## Requisitos previos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y en ejecución
- Credenciales de la [IGDB API](https://api-docs.igdb.com/#getting-started) (Client ID y Client Secret de Twitch Developer)

---

## Configuración del entorno

Crea el archivo de variables de entorno copiando la plantilla:

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

```{warning}
El archivo `.env` está en `.gitignore` y nunca debe subirse al repositorio.
```

---

## Ejecutar el proyecto

Docker levanta la aplicación y la base de datos MySQL juntas, sin necesidad de instalar Java, Maven ni MySQL.

**1. Desde la carpeta `game-comparer/`, ejecuta:**

```bash
docker compose up --build
```

**2. Abre la aplicación en:** [http://localhost:8080](http://localhost:8080)

```{note}
La base de datos MySQL usa el puerto `localhost:3307` si quieres conectarte con MySQL Workbench.
```

---

## Detener los contenedores

```bash
docker compose down
```

Los datos de la base de datos se conservan entre reinicios gracias al volumen `mysql-data`.
Para borrarlos:

```bash
docker compose down --volumes
```
