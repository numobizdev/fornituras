SIGEFOR (Sistema Integral de Gestión de Fornituras)
en este sistema se tendran las pantallas siguientes 
1.- Captura y asignacion: esta pantalla al cargar debe mostrar las asignaciones a quien esta asignado una fornitura y debemos tener  un boton con el cual asignar la cual debe permitir capturar como paso1 Idenfiicaciones de la fornitura buscar (debe detectar si es por lector qr, camara automaticamente) la fornitura por el numero de qr y aun lado un boton buscar al encontrarlo debe mostrar debao una descripcion de la fornitura y si esta disponible como segundo paso debe permitir buscar el elmento policial al que se le va asignar y debe presentar un input con donde pueda buscar por nombre, placa, curp,rfc  al encontrar al elemento debe mostrar la descripcion y el nombre del elemento con su respectriva foto al final y tener todos los paso capturados debe  ofrecer dos botones una de asignar fornitura y otra de limpiar 

2.- Elementos: en esta pantalla al cargar se debe mostrar de primera instancia un boton de reporte, un boton de nuevo elemento seguiedo de un panel de filtros con un cambpo de texto que busque por todas las columnas nombre, curp, rfc o placa  se seguido de un filtro como por municipio, sexo y un boton de buscar debajo una tabla con las columnas foto en miniatura, nombre completo, curp, id (placa), municipio, tipo de sangre, acciones

pantalla nuevo elemento se va capturar los datos de lo elementos policiales como son nombre, apelido paterno, apellido materno, sexo (catalogo), tipo de sangre(catalogo)
documentos curp, rfc, identificador (placa/serie), municipio y una seccion de foto del elemento

3.- Fornituras: 
Pantalla principal.- boton nueva fornitura con uno alado de alta por lote seguido de un panel de filtros qr descripcion,estado, almacen y seguido de una tabla de fornituras con las columnas QR, descripcion, tipo(cinturon,chaleco), talla, almacen, estado, vida util, acciones

Alta de fornitura por lote.- En esta pantalla se va capturar los datos generales del lote descripcion, talla(catalogo), tipo (catalogo)(cinturon,chaleco), almacen, vida util posteriormente se podra capturara codigos de QR por medio de un lector, camara, manual numero de codigo QR este tendre un campo con un input con un placeholder escanee con el lector o teclee el codigo  y un boton agregar y debajo una tabla de fornituras en el lote un boton crear lote y cancelar

> **Modelado de "vida útil" (recomendación adoptada en la spec 001).** El dato canónico es una
> **fecha de vencimiento absoluta** (`fecha_vencimiento`), porque elimina ambigüedad y permite
> derivar alertas y los estados de vigencia (vigente / próxima a vencer ≤ 90 días / caducada).
> En el alta por lote se captura la **vida útil en meses** (`vida_util_meses`) junto con la
> `fecha_fabricacion`/adquisición, y el sistema calcula
> `fecha_vencimiento = fecha_fabricacion + vida_util_meses`. Así el usuario piensa en "meses/años"
> y el sistema razona con una fecha concreta. Ver `specs/001-inventario-equipos/spec.md`.
Alta de fornitura individual.- este formulario solicitara registrar Numero de QR (Unico) la cual puede ser por lector QR, Carmara(si detecta el movi que automaitcamente reconosca la camara del movil o del pc par hacerlo flexible pero no se que tan viable sea este ultimo), Descripcion, Talla (catalogo), tipo(catalogo)(cinturon,chaleco), almacen, vida util

4.- Incidencias:
en esta pantalla en el header tendra un combo filtro de estados y estara un reporte en una tabla con columnas fornitura, problema, reportada (fecha), estado, Actualizar (accion) 

5.- Traslados: esta pantalla debe cargar los traslados realizados  con filtros y un boton de nuevo traslado que permita capturar alamacen origen y almacen destino despues se debe capturar el codigo QR este se puede obtener o leer desde un lector QR, CAMARA, MAnual y debe presentarse un compo de busqueda con un boton agregar
debajo una tabla de de fornituras a trasladar  seguida de una tabla traslados registrados  con las columnas origen, destino estado enviado(fecha), recibido(fecha),Accion

6.- Bajas: para las bajas se debe presentar opcion de baja de la fornitura la cual se debe buscar por QR o numero de qr y una tabla de las formituras dadas de baja con informacion relevante

7.- Reportes: presentar funcion de exporar a excel y reporte de un tatal de fornituras y de ahi que se describa cuantos estan disponibles, asignadas, incidencia, baja,elementos  y debajo una tabla de asignacion activas  con filtro de qr  de fornitura, nobre, rcf, placa, curp y un filtro por municipio
seguido de una talba con las columans relevante que puede ser fornitura,elemento y las que se considere relevante
8.- Usuarios
9.- Auditoria: en esta pantalla basicamente  se debe presnetar el log o las transaccion es echas fehca y accion  usando el toma de referencia el iso 27001

10.- Tipo de fornituras : aqui se registra la fornitura nombre, foto, descripcion opciones delta considera crud
11.- Almacenes

Los nombres si los puedes mejorar para que sea mas profesional y elegante orientado a los policias de preferencia que la carga de informacion sea por paginacion para no cargar todos los datos y tener un performace decente si consideras alguan mejora lo puedes hace o hazmelo saber.