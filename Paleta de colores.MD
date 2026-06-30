Guía de Diseño UI/UX
Sistema de Inventario y Control de Chalecos Antibalas
Objetivo
Diseñar una interfaz institucional, profesional, clara y segura para la gestión de chalecos antibalas asignados al personal policial, manteniendo una identidad visual alineada con organismos gubernamentales y facilitando la operación diaria del personal administrativo y operativo.
________________________________________
Principios de Diseño
•	Apariencia institucional y profesional.
•	Priorizar legibilidad y accesibilidad.
•	Reducir la carga cognitiva del usuario.
•	Resaltar información crítica mediante colores semánticos.
•	Facilitar la consulta rápida de inventario, asignaciones y vencimientos.
•	Mantener consistencia visual en todos los módulos.
________________________________________
Paleta de Colores Institucional
Colores Primarios
Elemento	Color	HEX
Primario Institucional	Guinda	#611232
Secundario Institucional	Dorado	#A57F2C
Fondo Principal	Blanco	#FFFFFF
Fondo Secundario	Gris Claro	#F5F5F5
Texto Principal	Gris Oscuro	#333333
Bordes	Gris Suave	#D1D5DB
________________________________________
Colores de Estado
Estado	Color	HEX
Disponible	Verde	#198754
Asignado	Azul	#0D6EFD
En Mantenimiento	Amarillo	#FFC107
Próximo a Vencer	Naranja	#FD7E14
Caducado	Rojo	#DC3545
Baja Definitiva	Gris	#6C757D
________________________________________
Variables CSS
:root {
    /* Institucional */
    --primary: #611232;
    --secondary: #A57F2C;

    /* Fondos */
    --background: #F5F5F5;
    --surface: #FFFFFF;

    /* Texto */
    --text-primary: #333333;
    --text-secondary: #6B7280;

    /* Bordes */
    --border: #D1D5DB;

    /* Estados */
    --success: #198754;
    --info: #0D6EFD;
    --warning: #FFC107;
    --orange: #FD7E14;
    --danger: #DC3545;
    --inactive: #6C757D;
}
________________________________________
Estructura General de la Aplicación
Barra Superior
Color de fondo:
#611232
Contenido:
•	Logotipo institucional
•	Nombre del sistema
•	Usuario autenticado
•	Rol del usuario
•	Notificaciones
•	Cerrar sesión
________________________________________
Menú Lateral
Secciones recomendadas:
Dashboard
Inventario
Asignaciones
Personal
Mantenimiento
Vencimientos
Reportes
Auditoría
Usuarios
Configuración
Características:
•	Fondo blanco.
•	Opción activa en color guinda.
•	Íconos simples y consistentes.
•	Menú colapsable.
________________________________________
Dashboard Ejecutivo
Indicadores Principales
Total de Chalecos
Muestra:
1,258
Color:
Guinda
________________________________________
Disponibles
Color:
Verde
________________________________________
Asignados
Color:
Azul
________________________________________
Próximos a Vencer
Color:
Naranja
________________________________________
Caducados
Color:
Rojo
________________________________________
En Mantenimiento
Color:
Amarillo
________________________________________
Gestión de Inventario
Cada chaleco debe registrar:
•	Número de inventario
•	Número de serie
•	Marca
•	Modelo
•	Nivel balístico
•	Fecha de fabricación
•	Fecha de adquisición
•	Fecha de vencimiento
•	Estado actual
•	Ubicación
•	Observaciones
•	Fotografía
•	Documentación adjunta
________________________________________
Estados del Chaleco
Disponible
Asignado
En mantenimiento
Extraviado
Próximo a vencer
Caducado
Baja definitiva
________________________________________
Alertas Inteligentes
Alerta Preventiva
Mostrar cuando falten:
90 días o menos para el vencimiento
Color:
Naranja
________________________________________
Alerta Crítica
Mostrar cuando:
La fecha de vencimiento ha sido superada
Color:
Rojo
________________________________________
Alertas de Mantenimiento
Generar notificaciones para:
•	Inspecciones periódicas.
•	Daños reportados.
•	Sustituciones programadas.
________________________________________
Módulo de Asignaciones
Permitir:
•	Asignar chaleco a policía.
•	Reasignar chaleco.
•	Historial de movimientos.
•	Registro de entrega y recepción.
•	Firma electrónica.
•	Generación automática de resguardos.
________________________________________
Reportes
Reportes Operativos
•	Inventario general.
•	Inventario por unidad.
•	Inventario por región.
•	Chalecos asignados.
•	Chalecos disponibles.
•	Chalecos en mantenimiento.
Reportes de Control
•	Próximos a vencer.
•	Caducados.
•	Historial de movimientos.
•	Auditoría de usuarios.
•	Cambios realizados.
________________________________________
Seguridad
Implementar control de acceso basado en roles.
Roles sugeridos
Administrador
Control total.
Supervisor
Consulta y autorización.
Almacén
Administración de inventario.
Auditor
Solo lectura y reportes.
Consulta
Visualización limitada.
________________________________________
Auditoría
Registrar:
•	Usuario.
•	Fecha.
•	Hora.
•	Acción realizada.
•	Registro afectado.
•	Dirección IP.
•	Evidencia del cambio.
________________________________________
Accesibilidad
•	Contraste mínimo AA.
•	Tamaño de fuente mínimo 14px.
•	Compatible con dispositivos móviles.
•	Compatible con modo oscuro.
•	Navegación mediante teclado.
________________________________________
Tecnologías Recomendadas
Frontend:
•	React
•	Angular
Backend:
•	NestJS
•	.NET
Base de Datos:
•	PostgreSQL
•	MySQL
Autenticación:
•	JWT
•	MFA (Autenticación Multifactor)
________________________________________
Objetivo Final
El sistema debe permitir mantener el control completo del ciclo de vida de cada chaleco antibalas, desde su adquisición hasta su baja definitiva, garantizando trazabilidad, transparencia, cumplimiento normativo y disponibilidad operativa para el personal policial.
