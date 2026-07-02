# 0020. Identidad del sistema: Sistema Integral de Gestión de Fornituras

- **Estado:** Aceptado
- **Fecha:** 2026-07-01

## Contexto

El sistema se presentaba públicamente como **"Sistema de Gestión de Blindajes"**, nombre
tomado del README, `Planeacion.md` y la constitución. El commit `6b99f21`
(`fix/landing-hero-title`) y la migración `V24` del backend Java (hoy obsoleto) incluso
*corrigieron* el hero público de la landing hacia ese nombre, tratando la expansión del
acrónimo SIGEFOR ("Sistema Integral de Gestión de Fornituras") como un error de siembra.

El cliente decidió lo contrario: la identidad oficial del producto es la expansión del
acrónimo **SIGEFOR — Sistema Integral de Gestión de Fornituras**. Además pidió retirar la
marca "Gobierno de México" del título del navegador y hacer más descubrible el módulo de
administración de la landing (spec 016), cuya entrada de menú ("Contenido de bienvenida")
no se reconocía como tal.

## Decisión

1. El nombre oficial y visible del sistema es **"Sistema Integral de Gestión de Fornituras"**;
   "SIGEFOR" se conserva como acrónimo. Esta decisión **revierte la justificación del commit
   `6b99f21`** y deja sin efecto el criterio de la migración `V24` del backend Java.
2. El título del documento (pestaña del navegador) es
   `SIGEFOR | Sistema Integral de Gestión de Fornituras` — se elimina "Gobierno de México".
3. Las instalaciones existentes se actualizan mediante una **migración EF Core de solo datos**
   acotada al valor sembrado anterior exacto, para no sobrescribir títulos editados
   deliberadamente por el administrador (misma técnica que la V24, dirección inversa).
4. La entrada de menú del editor de landing pasa de "Contenido de bienvenida" a
   **"Configurar landing"** (sin cambios en el gating: solo rol ADMIN).
5. El **vocabulario del dominio no cambia**: el sistema sigue administrando blindajes
   (chalecos y equipo de seguridad). `Planeacion.md` y los documentos/specs históricos se
   conservan intactos. La constitución solo recibe una enmienda PATCH en su título nominal.

## Alternativas consideradas

- **Mantener "Sistema de Gestión de Blindajes"** (statu quo del `6b99f21`): descartado — el
  cliente definió la identidad oficial como la expansión de SIGEFOR.
- **Actualizar el título vía `DataSeeder` al arranque**: descartado — el seeder no corre en
  BD ya sembradas y un upsert al inicio pisaría ediciones del administrador; la constitución
  exige cambios de datos versionados (migraciones).
- **Reemplazo global de "blindajes" por "fornituras" en todo el repo**: descartado —
  reescribiría specs cerradas y documentos históricos; el cambio es de marca, no de dominio.

## Consecuencias

- (+) La identidad visible (hero, footer, pestaña) queda alineada con la decisión del cliente
  y coherente con el acrónimo SIGEFOR usado en el menú y el login.
- (+) Las personalizaciones del administrador sobre la landing se respetan (la migración solo
  toca el valor sembrado exacto y es idempotente).
- (−) La documentación histórica (Planeación, specs 001–018, ADRs previos) conserva el nombre
  antiguo; es intencional y este ADR es la referencia para resolver la aparente contradicción.
- Cualquier cambio futuro de identidad deberá registrarse como un nuevo ADR que reemplace a este.
