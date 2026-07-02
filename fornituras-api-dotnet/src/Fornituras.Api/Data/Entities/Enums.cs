namespace Fornituras.Api.Data.Entities;

/// <summary>
/// Roles del sistema (RBAC). Persistidos como cadena en <c>users.role</c>.
/// </summary>
public enum Role
{
    ADMIN,
    SUPER_ADMIN,
    SUPERVISOR,
    ALMACEN,
    AUDITOR,
    CAPTURISTA
}

/// <summary>
/// Estado operativo de una fornitura. Persistido en <c>equipment.status</c>.
/// </summary>
public enum EquipmentStatus
{
    DISPONIBLE,
    ASIGNADA,
    EN_MANTENIMIENTO,
    EN_TRASLADO,
    EXTRAVIADA,
    BAJA_DEFINITIVA
}

/// <summary>
/// Estado de vigencia derivado de <c>fecha_vencimiento</c>; no se persiste.
/// </summary>
public enum ExpiryStatus
{
    VIGENTE,
    PROXIMA_A_VENCER,
    CADUCADA
}

/// <summary>
/// Enum legado de tipo de almacén (pre-ADR 0007). Sustituido por catálogo <c>TIPO_ALMACEN</c>.
/// </summary>
public enum WarehouseType
{
    CENTRAL,
    REGIONAL,
    MOVIL,
    TEMPORAL
}

/// <summary>
/// Posición del código legible en la etiqueta QR.
/// </summary>
public enum LabelPosition
{
    NONE,
    TOP,
    BOTTOM
}

/// <summary>
/// Estado de un traslado entre almacenes.
/// </summary>
public enum TransferStatus
{
    ENVIADO,
    RECIBIDO,
    CANCELADO
}

/// <summary>
/// Tipo de incidencia sobre una fornitura.
/// </summary>
public enum IncidentType
{
    DANO,
    FALLA,
    EXTRAVIO,
    MANTENIMIENTO
}

/// <summary>
/// Estado de seguimiento de una incidencia.
/// </summary>
public enum IncidentStatus
{
    ABIERTA,
    EN_PROCESO,
    RESUELTA,
    CERRADA
}

/// <summary>
/// Cara de la landing configurable (pública o panel autenticado).
/// </summary>
public enum LandingScope
{
    PUBLIC,
    HOME
}

/// <summary>
/// Tipo de sección de contenido en la landing.
/// </summary>
public enum LandingSectionType
{
    HERO,
    ANNOUNCEMENT,
    QUICK_LINKS,
    RICH_TEXT
}
