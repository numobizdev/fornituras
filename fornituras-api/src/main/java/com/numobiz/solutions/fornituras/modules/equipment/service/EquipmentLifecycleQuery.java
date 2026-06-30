package com.numobiz.solutions.fornituras.modules.equipment.service;

/**
 * Puerto que responde si una fornitura está comprometida por otras features: ¿tiene una
 * asignación vigente (004)? ¿un traslado en curso (007)? Aísla a {@code EquipmentService} de
 * módulos que aún no existen: la implementación por defecto responde "no". Cuando 004/007 se
 * implementen, proveen una implementación real (consultando sus repositorios) y las reglas de
 * baja/traslado quedan activas sin tocar el servicio.
 */
public interface EquipmentLifecycleQuery {

	boolean hasActiveAssignment(Long equipmentId);

	boolean hasOngoingTransfer(Long equipmentId);
}
