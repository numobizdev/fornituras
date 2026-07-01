# 0010. Rate limiting de la resolución por código con Bucket4j tras un puerto

- **Estado:** **Aceptado**
- **Fecha:** 2026-06-30
- **Feature:** [001-inventario-equipos](../../specs/001-inventario-equipos/) (tarea T040)

## Contexto

El endpoint `GET /api/v1/equipment/by-codigo/{codigo}` resuelve un código opaco (`FOR-XXXXX`,
[ADR 0005](0005-formato-qr-implementado.md)) a la ficha de la fornitura, del lado servidor y tras
autenticación. Aun autenticado, un actor podría **enumerar** el espacio de códigos para inventariar
el parque de blindajes (dato sensible). La tarea T040 pide **limitar la tasa** de esta resolución
para frenar la enumeración, sin filtrar detalles en los errores.

El proyecto no tenía mecanismo de rate limiting. Opciones consideradas:

1. **Limitador propio en memoria:** cero dependencias, pero reimplementa (y hay que mantener y
   probar) un token-bucket correcto ante concurrencia.
2. **Bucket4j** (`com.bucket4j:bucket4j-core`, **Apache-2.0**): librería estándar y madura de
   token-bucket, correcta ante concurrencia, con backend en memoria y opción distribuida (Redis,
   Hazelcast) sin cambiar la lógica de consumo.

## Decisión

1. **Programar contra un puerto** `RateLimiter` (`common/ratelimit`), no contra Bucket4j (LEGO/DIP).
   Los consumidores dependen del puerto; el mecanismo es sustituible.
2. **Implementación por defecto** `Bucket4jRateLimiter`: un token-bucket **en memoria** por clave
   (`operación + actor`), con capacidad y recarga configurables en
   `app.ratelimit.by-codigo` (por defecto **30 peticiones / 60 s por actor**).
3. **Aplicación:** el controlador consulta el puerto antes de resolver el código; si no hay cupo,
   lanza `TooManyRequestsException` → **HTTP 429** (mapeado en `GlobalExceptionHandler`), con mensaje
   genérico que **no filtra** si el código existe.
4. **Dependencia (Principio VI / regla 4):** se registra Bucket4j con **necesidad** (control de
   seguridad anti-enumeración), **licencia** aceptable (Apache-2.0) y **mantenimiento** activo.

## Consecuencias

- **Positivas:** control anti-enumeración real sobre el endpoint sensible; concurrencia correcta sin
  código propio; configurable por entorno; el puerto permite pasar a un limitador **distribuido**
  (Redis) para multi-instancia sin tocar el controlador.
- **Límites:** la implementación por defecto es **por instancia** (no comparte cupo entre réplicas);
  en despliegue multi-instancia el límite efectivo se multiplica por el nº de réplicas hasta migrar
  a un backend distribuido. La clave es por **actor autenticado**; reforzar con IP/límite global
  queda como extensión.
- **Alcance:** por ahora solo se limita `by-codigo` (el vector de enumeración). Otros endpoints se
  pueden sumar reutilizando el mismo puerto.
