# Palm Flow API

## Descripción

API REST construida con Java 21 y Spring Boot 4 siguiendo arquitectura limpia.

---

## Arquitectura

Capas:

- Controller
- Service
- Repository
- Entity
- DTO
- Mapper

No se exponen entidades directamente en los controllers.

---

## Estructura

Paquete base:

mx.uumbal.solutions.palmflow

Módulos:

- auth
- users
- productores

Cada módulo contiene:

- controller
- service
- repository
- entity
- dto
- mapper

---

## Seguridad

Implementada con JWT:

- AuthController
- AuthService
- JwtService
- JwtAuthenticationFilter
- SecurityConfig

Header requerido:

Authorization: Bearer <token>

Roles:

- administrador
- analista
- productor

---

## API

Versionado:

/api/v1

Formato de respuesta:

{
  "success": true,
  "message": "operation successful",
  "data": {}
}

Clase base:

ApiResponse<T>

---

## Manejo de errores

- @RestControllerAdvice
- Excepciones:
  - NotFoundException
  - BadRequestException
  - UnauthorizedException

---

## Base de datos

- SQL Server
- JPA / Hibernate
- ddl-auto: update

Configuración en:

- application.yml
- application.dev.yml
- application.prod.yml

---

## Autenticación

Endpoints:

- inicio de sesión
- registro con validación por correo
- cambio de contraseña
- recuperación de contraseña

Flujos:

Registro:
1. Solicitar correo y contraseña
2. Enviar link de validación
3. Activar cuenta

Recuperación:
1. Solicitar correo
2. Enviar link
3. Restablecer contraseña

---

## Ejemplo módulo users

Componentes:

- User entity
- UserRepository
- UserService
- UserController
- UserRequestDTO
- UserResponseDTO
- UserMapper

Endpoints:

- GET /api/v1/users/{id}
- POST /api/v1/users
- GET /api/v1/users

---

## Extras

- Validaciones con @Valid
- Logging con SLF4J
- Actuator
- Thymeleaf para correos