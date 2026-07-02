package com.numobiz.solutions.fornituras.modules.media.dto;

import java.util.UUID;

/**
 * Referencia devuelta tras subir una foto (017). {@code url} es la referencia interna opaca que la
 * entidad dueña guarda en su {@code fotoUrl}; nunca una URL externa.
 */
public record MediaUploadResponse(UUID id, String url, String contentType) {
}
