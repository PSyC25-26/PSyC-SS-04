# Tests

El proyecto tiene tres niveles de tests. Todos se ejecutan desde la carpeta `game-comparer/`.

---

## Tests unitarios y de controlador

No requieren base de datos ni Docker.

```bash
./mvnw test
```

---

## Tests de rendimiento

Miden la latencia y el throughput de los servicios bajo carga concurrente.
No requieren base de datos ni Docker (usan mocks).

```bash
./mvnw test -Pperformance
```

---

## Tests de integración

Arrancan la aplicación completa contra la base de datos real. Requieren Docker en ejecución.

**1. Configura el perfil de test** (solo la primera vez):

```bash
cp src/test/resources/application-test.properties.example src/test/resources/application-test.properties
```

Edita el archivo con los valores de tu `.env`:

```properties
spring.datasource.password=YOUR_DB_ROOT_PASSWORD
igdb.client-id=YOUR_IGDB_CLIENT_ID
igdb.client-secret=YOUR_IGDB_CLIENT_SECRET
```

```{warning}
`application-test.properties` está en `.gitignore` y nunca debe subirse al repositorio.
```

**2. Levanta la base de datos:**

```bash
docker compose up -d
```

**3. Ejecuta el test:**

```bash
./mvnw test -Dtest=WishlistIntegrationTest
```
