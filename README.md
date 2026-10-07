# SGBiblioteca — API REST del Sistema de Gestión de Biblioteca

Backend de una biblioteca universitaria construido con **microservicios** en Spring Boot. Administra usuarios, roles, libros, préstamos, devoluciones e historial, con autenticación y autorización mediante **JWT**.

## Tecnologías

| Componente | Versión / detalle |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Web, Spring Security, Spring Data JPA, Validation | incluidos en Spring Boot |
| Base de datos | MySQL |
| JWT | JJWT 0.12.6 |
| Contraseñas | BCrypt |
| Build | Maven |
| Otros | Lombok |

## Arquitectura

Tres microservicios independientes, cada uno con su propia base de datos:

| Servicio | Responsabilidad | Puerto | Base de datos |
|---|---|---|---|
| `auth-service` | Usuarios, roles, registro, login, emisión de JWT | 8081 | `authdb_in5av` |
| `catalog-service` | Libros, catálogo, stock | 8083 | `catalogdb_in5av` |
| `loan-service` | Préstamos, devoluciones, historial, atrasados | 8085 | `loandb_in5av` |

- Los tres validan el **mismo JWT** con una clave secreta compartida (`jwt.secret`); cada servicio lee el rol directamente del token, sin consultar a `auth-service`.
- `loan-service` se comunica por HTTP (`RestClient`) con `auth-service` (consultar usuario y marcarlo `SANCIONADO`) y con `catalog-service` (ajustar stock), reenviando el JWT de quien hace la petición.
- No hay relaciones JPA entre servicios: `Prestamo` guarda `usuarioId` y `libroId` como identificadores simples.

```text
SGBiblioteca/
├── auth-service/      (paquete com.biblioteca.auth_service)
├── catalog-service/   (paquete com.biblioteca.catalog_service)
├── loan-service/      (paquete com.biblioteca.loan_service)
└── README.md
```

## Requisitos previos

- JDK 21
- MySQL en ejecución (puerto 3306 por defecto)
- Maven (o el wrapper `mvnw` incluido en cada servicio) o IntelliJ IDEA

## Configuración

Cada servicio tiene su `src/main/resources/application.properties`. Ajusta en los tres:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/<NOMBRE_BD>?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=<USUARIO_MYSQL>
spring.datasource.password=<CONTRASEÑA_MYSQL>

# Debe ser IDÉNTICO en los tres servicios (mínimo 32 caracteres)
jwt.secret=<CLAVE_SECRETA_COMPARTIDA>
```

Propiedades específicas:

| Servicio | Propiedades |
|---|---|
| `auth-service` | `server.port=8081`, `jwt.expiration-ms=3600000` (1 hora) |
| `catalog-service` | `server.port=8083` |
| `loan-service` | `server.port=8085`, `services.auth.url=http://localhost:8081`, `services.catalog.url=http://localhost:8083` |

Si cambias un puerto, actualiza también `services.auth.url` y `services.catalog.url` en `loan-service`.

## Cómo ejecutar

Orden de arranque:

1. MySQL.
2. `auth-service`
3. `catalog-service`
4. `loan-service`

Desde IntelliJ: abre la clase `...Application` de cada servicio y ejecútala. Desde la terminal, dentro de la carpeta de cada servicio:

```bash
./mvnw spring-boot:run        # Linux / macOS / Git Bash
mvnw.cmd spring-boot:run      # CMD de Windows
```

Cada servicio imprime `Started ...Application` cuando está listo.

## Base de datos e inicialización

Cada servicio crea sus tablas con `schema.sql` y carga datos iniciales con `data.sql` (`spring.jpa.hibernate.ddl-auto=none`, `spring.sql.init.mode=always`). Los scripts son idempotentes: se pueden ejecutar en cada arranque sin duplicar datos.

- **`auth-service`:** tablas `roles` y `usuarios`. Se insertan los 3 roles (`ADMIN`, `BIBLIOTECARIO`, `LECTOR`) y 1 usuario ADMIN inicial.
- **`catalog-service`:** tabla `libros` (con restricción `stock_disponible >= 0`) y 4 libros de ejemplo.
- **`loan-service`:** tabla `prestamos`.

### Usuario ADMIN inicial

| Campo | Valor |
|---|---|
| Email | `admin@biblioteca.com` |
| Contraseña | `Admin123*` |
| Rol | `ADMIN` |

## Seguridad

- `SecurityFilterChain` con CSRF deshabilitado y `SessionCreationPolicy.STATELESS`.
- `JwtAuthenticationFilter` (`OncePerRequestFilter`) valida el token `Authorization: Bearer <TOKEN>` en cada petición.
- El JWT contiene el correo (subject), el `id` y el `rol` del usuario, firmado con HS512 y con 1 hora de duración.
- Contraseñas almacenadas con `BCryptPasswordEncoder`; nunca se devuelven en las respuestas.
- Autorización por rol con `@PreAuthorize` y respuestas JSON para `401` (sin token o token inválido) y `403` (sin permisos).

### Roles

| Rol | Permisos |
|---|---|
| `ADMIN` | Control total: catálogo, usuarios, préstamos, devoluciones, atrasados |
| `BIBLIOTECARIO` | Registrar préstamos y devoluciones, consultar atrasados |
| `LECTOR` | Consultar el catálogo y sus propios préstamos (historial) |

El registro público siempre crea usuarios con rol `LECTOR`.

## Endpoints

### auth-service (`http://localhost:8081`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Público | Registra un usuario con rol `LECTOR` (201) |
| POST | `/api/v1/auth/login` | Público | Devuelve `token`, `tipo`, `email` y `rol` |
| GET | `/api/v1/usuarios/{id}` | ADMIN, BIBLIOTECARIO | Consulta un usuario |
| PATCH | `/api/v1/usuarios/{id}/estado` | ADMIN, BIBLIOTECARIO | Cambia el estado (`ACTIVO` / `SANCIONADO`) |

### catalog-service (`http://localhost:8083`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/v1/libros` | Autenticado | Lista con paginación; filtros opcionales `titulo` y `categoria` (`page`, `size`) |
| GET | `/api/v1/libros/{id}` | Autenticado | Obtiene un libro |
| POST | `/api/v1/libros` | ADMIN | Crea un libro (`stockDisponible` = `stockTotal`) |
| PUT | `/api/v1/libros/{id}` | ADMIN | Actualiza un libro |
| DELETE | `/api/v1/libros/{id}` | ADMIN | Elimina un libro (eliminación física, 204) |
| PATCH | `/api/v1/libros/{id}/stock` | ADMIN, BIBLIOTECARIO | Ajusta el stock disponible con `{"cambio": -1}` o `{"cambio": 1}` (uso interno de `loan-service`) |

### loan-service (`http://localhost:8085`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/v1/prestamos` | ADMIN, BIBLIOTECARIO | Registra un préstamo (201) |
| PATCH | `/api/v1/prestamos/{id}/devolucion` | ADMIN, BIBLIOTECARIO | Registra la devolución |
| GET | `/api/v1/prestamos/mis-prestamos` | LECTOR | Historial y préstamos del usuario autenticado |
| GET | `/api/v1/prestamos/atrasados` | ADMIN, BIBLIOTECARIO | Préstamos con fecha esperada vencida |

### Ejemplos de cuerpos JSON

```json
// POST /api/v1/auth/register
{ "nombre": "Ana", "email": "ana@correo.com", "password": "123456" }

// POST /api/v1/auth/login
{ "email": "admin@biblioteca.com", "password": "Admin123*" }

// POST /api/v1/libros
{ "isbn": "EJ-0005", "titulo": "Redes", "autor": "Autor Cinco", "categoria": "Redes", "stockTotal": 4 }

// POST /api/v1/prestamos
{ "usuarioId": 2, "libroId": 1 }
```

## Reglas de negocio

Se evalúan al registrar un préstamo, en este orden y dentro de una transacción (`@Transactional`):

1. **Usuario existente:** si no existe, `404`.
2. **Usuario sancionado:** no puede realizar préstamos (`400`).
3. **Préstamo vencido sin devolver:** el usuario pasa a `SANCIONADO` en `auth-service` y el préstamo se rechaza (`400`).
4. **Máximo 3 préstamos activos** para un `LECTOR` (`400`). Los préstamos `ACTIVO` y `ATRASADO` cuentan como no devueltos.
5. **Duración:** `fechaDevolucionEsperada = fechaPrestamo + 14 días`.
6. **Stock:** el libro debe existir y tener `stockDisponible > 0`; al prestar se descuenta 1 y al devolver se suma 1. Si el libro no existe o no hay stock, el préstamo guardado se revierte.

Estados de préstamo: `ACTIVO`, `DEVUELTO`, `ATRASADO`. El estado `ATRASADO` se calcula al consultar `/atrasados` y `/mis-prestamos`, comparando la fecha esperada con la fecha actual. `fechaDevolucionReal` es `null` hasta que se registra la devolución.

Estados de usuario: `ACTIVO`, `SANCIONADO`.

## Manejo de errores

Cada servicio tiene un `@RestControllerAdvice` con excepciones personalizadas (`ResourceNotFoundException`, `BusinessRuleException`). Formato de error:

```json
{
  "timestamp": "2026-10-07T01:55:47.03",
  "status": 400,
  "error": "Bad Request",
  "message": "El lector ya tiene 3 préstamos activos"
}
```

| Código | Cuándo |
|---|---|
| 400 | Regla de negocio incumplida o datos inválidos |
| 401 | Token ausente, inválido o vencido |
| 403 | Rol sin permisos para la operación |
| 404 | Recurso no encontrado |
| 503 | Un servicio del que depende `loan-service` no está disponible |

## Pruebas

### Flujo mínimo con Postman

1. Registrar un usuario (`/auth/register`).
2. Iniciar sesión (`/auth/login`) y copiar el `token`.
3. Consultar libros (`GET /libros`).
4. Iniciar sesión como ADMIN y crear un libro (`POST /libros`).
5. Registrar un préstamo (`POST /prestamos`).
6. Registrar la devolución (`PATCH /prestamos/{id}/devolucion`).
7. Consultar los préstamos del lector (`GET /prestamos/mis-prestamos`, con el token del LECTOR).
8. Consultar los atrasados (`GET /prestamos/atrasados`).

En Postman, usa **Authorization → Bearer Token** y pega solo el valor del `token`, sin comillas.

Colección exportable: _(agregar aquí el nombre del archivo `.json` de la colección)_.

### Script de seguridad y concurrencia

`test-api5.sh` valida el registro, el login, la creación de libros, el `403` para un `LECTOR` y una prueba de concurrencia sobre `GET /libros`. Requiere Git Bash y `jq`. Como el script apunta a un único `BASE_URL`, en este proyecto el login y el registro van a `auth-service` (8081) y los libros a `catalog-service` (8083).

```bash
chmod +x test-api5.sh
./test-api5.sh
```

## Decisiones de diseño y limitaciones

- **Eliminación de libros:** física, por simplicidad.
- **Actualización de stock total:** al cambiar `stockTotal`, `stockDisponible` se ajusta por la diferencia y se rechaza si quedaría negativo.
- **Concurrencia en el stock:** el ajuste usa bloqueo pesimista (`PESSIMISTIC_WRITE`) y la base tiene una restricción `CHECK`.
- **Consistencia entre servicios:** el stock se descuenta como último paso del registro del préstamo; si falla, el préstamo local se revierte. Existe un caso límite improbable (el descuento remoto funciona pero falla el commit local) que requeriría compensaciones fuera del alcance de este proyecto.
- **Reactivación de usuarios sancionados:** no es automática; la realiza un ADMIN o BIBLIOTECARIO con `PATCH /api/v1/usuarios/{id}/estado`.
- **No implementado:** listado general de préstamos y gestión completa de usuarios (solo consulta por id y cambio de estado).
- **Credenciales:** los `application.properties` contienen la configuración de desarrollo; no reutilices esas contraseñas en otros entornos.

## Flujo de ramas Git

```text
main (rama huérfana)
└── development
    └── psipac-2025347
```

El trabajo se realiza en `psipac-2025347` y se integra mediante Pull Requests: `psipac-2025347 → development` en cada checkpoint y, al final, `development → main`.
