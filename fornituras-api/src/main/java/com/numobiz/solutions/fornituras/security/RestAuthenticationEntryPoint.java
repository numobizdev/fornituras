package com.numobiz.solutions.fornituras.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Punto de entrada de autenticación para la API REST (stateless/JWT): cuando un cliente **no
 * autenticado** accede a un recurso protegido, responde <b>401</b> con el envoltorio estándar
 * {@code ApiResponse} en JSON, en lugar del 403 por defecto de Spring. Así el contrato REST distingue
 * "no autenticado" (401) de "autenticado sin permiso" (403) y el cliente puede cerrar sesión al expirar.
 * El mensaje es genérico: no filtra si el recurso existe.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write("{\"success\":false,\"message\":\"No autenticado\",\"data\":null}");
	}
}
