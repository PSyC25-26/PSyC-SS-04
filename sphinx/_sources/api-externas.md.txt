# APIs externas

## IGDB

Proporciona metadatos de juegos: nombre, género, descripción, fecha de lanzamiento y portada.

- **Autenticación**: OAuth2 con Twitch. El token se gestiona automáticamente mediante `IgdbTokenService`.
- **Documentación oficial**: [api-docs.igdb.com](https://api-docs.igdb.com)
- **Credenciales necesarias**: `IGDB_CLIENT_ID` y `IGDB_CLIENT_SECRET` en el `.env`

---

## CheapShark

Proporciona precios actuales de juegos en Steam y Epic Games Store.

- **Autenticación**: no requiere API key
- **Documentación oficial**: [apidocs.cheapshark.com](https://apidocs.cheapshark.com)
- **Tiendas utilizadas**: Steam (storeID=1), Epic Games Store (storeID=25)

---

## Steam Store

Se consulta directamente cuando CheapShark no tiene precio para un juego.

- Se usa el Steam App ID obtenido de CheapShark o IGDB
- Como último recurso se realiza una búsqueda por nombre en Steam Store
