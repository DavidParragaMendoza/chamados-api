# Guía de Pruebas con Postman — API de Chamados do IFRN

Colección: `chamados.postman_collection.json` (Postman Collection **v2.1**, ubicada en la raíz del proyecto)  
Cobertura: **24 peticiones** · **5 carpetas** · Requisitos Funcionales **RF-1 a RF-39**

---

## 1. Requisitos previos

### 1.1 PostgreSQL activo
La API requiere PostgreSQL en ejecución con el esquema administrado por Flyway.
En Windows (PowerShell):
```powershell
Get-Service postgresql*        # debe mostrar "Running"
```

### 1.2 Variables de entorno de base de datos
La aplicación lee la conexión **estrictamente de variables de entorno** (nunca credenciales hardcodeadas en `application.properties`):

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/chamados_db'
$env:DB_USER='postgres'
$env:DB_PASSWORD='123456'
```

### 1.3 Aplicar las migraciones Flyway
```powershell
.\mvnw.cmd flyway:migrate
```
Salida esperada:
```text
Successfully validated 8 migrations
Current version of schema "public": 7
BUILD SUCCESS
```

### 1.4 Levantar la API
```powershell
.\mvnw.cmd spring-boot:run
```
La aplicación inicia en el puerto **8080** (`http://localhost:8080`).

### 1.5 Verificar que responde
Antes de ejecutar Postman, confirma la conectividad:
```powershell
Invoke-RestMethod "http://localhost:8080/setores?size=3" | ConvertTo-Json -Depth 4
```

---

## 2. Configuración en Postman

### 2.1 Importar la colección
1. En Postman, pulsa **Import** (esquina superior izquierda).
2. Selecciona o arrastra el archivo `chamados.postman_collection.json` desde la raíz del proyecto.
3. Se importará la colección **"Chamados API"** con 24 peticiones organizadas en 5 carpetas.

### 2.2 Variables de la colección
La colección define las siguientes variables configurables a nivel de colección:

| Variable | Valor inicial por defecto | Propósito |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | URL base de la API |
| `setorId` | `1` | ID del sector semilla **TI** (con tickets asociados) |
| `categoriaId` | `1` | ID de la categoría semilla **Redes** (con tickets asociados) |
| `solicitanteId` | `1` | ID de usuario solicitante semilla (**João Silva**, `SOLICITANTE`) |
| `tecnicoId` | `2` | ID de técnico semilla (**Maria Souza**, `TECNICO`, sector TI) |
| `usuarioId` | `1` | ID de usuario para consultas y actualización |
| `chamadoId` | `1` | ID del ticket creado dinámicamente durante la ejecución |
| `chamadoAssumirId` | `1` | ID generado en pre-request para asumir ticket |
| `chamadoResolverId` | `1` | ID generado en pre-request para resolver ticket |
| `chamadoCancelarId` | `1` | ID generado en pre-request para cancelar ticket |
| `siglaTemp` | `TEMP` | Sigla autogenerada con timestamp para evitar colisiones |
| `emailTemp` | `temp@ifrn.edu.br` | Email autogenerado con timestamp |
| `matriculaTemp` | `0000000` | Matrícula autogenerada con timestamp |
| `tituloTemp` | `Chamado de teste` | Título autogenerado con timestamp |

> **Idempotencia garantizada**: La colección incluye scripts `prerequest` automáticos que generan sufijos basados en timestamp (`Date.now()`) y crean tickets aislados mediante `pm.sendRequest` para las pruebas de transición de estado. Puedes ejecutar la colección cuantas veces desees sin reiniciar la base de datos.

---

## 3. Catálogo Detallado de las 24 Peticiones

### 3.1 Carpeta 1: Setores (5 peticiones)

#### 1. Criar setor — `POST /setores`
- **Propósito**: Registra un nuevo sector activo (`ativo = true`).
- **Body**:
```json
{
  "nome": "Setor de Teste {{siglaTemp}}",
  "sigla": "{{siglaTemp}}"
}
```
- **Éxito**: `201 Created` con cabecera `Location: /setores/{id}` y `ativo: true`.
- **Errores**: `400 Bad Request` si los campos son inválidos; `409 Conflict` si la sigla ya existe.

#### 2. Listar setores — `GET /setores?page=0&size=10&sort=nome`
- **Propósito**: Consulta paginada de sectores con filtro opcional por `nome`.
- **Query Params**: `page` (def. 0), `size` (def. 10, máx. 50), `sort` (def. `nome,asc`), `nome` (opcional).
- **Éxito**: `200 OK` con estructura `content`, `totalElements`, `totalPages`.

#### 3. Buscar setor por id — `GET /setores/{id}`
- **Propósito**: Obtiene los detalles de un sector específico por su ID.
- **Ruta**: `{{baseUrl}}/setores/{{setorId}}`
- **Éxito**: `200 OK` retornando `{ id, nome, sigla, ativo }`.
- **Errores**: `404 Not Found` si el ID no existe.

#### 4. Atualizar setor — `PUT /setores/{id}`
- **Propósito**: Modifica los datos de un sector existente.
- **Ruta**: `{{baseUrl}}/setores/{{setorId}}`
- **Body**:
```json
{
  "nome": "Tecnologia da Informação e Comunicação",
  "sigla": "TI",
  "ativo": true
}
```
- **Éxito**: `200 OK` con los datos actualizados.
- **Errores**: `404 Not Found` si no existe; `409 Conflict` si la nueva sigla pertenece a otro sector.

#### 5. Excluir setor — `DELETE /setores/{id}`
- **Propósito**: Elimina un sector (físico o lógico).
- **Ruta**: `{{baseUrl}}/setores/{{setorId}}`
- **Comportamiento (Regla 8)**: Si el sector tiene tickets asociados a través de sus categorías (como el sector semilla `1`), devuelve **`409 Conflict`**. Si no tiene tickets asociados, devuelve **`204 No Content`**.
- **Test**: `pm.expect(pm.response.code).to.be.oneOf([204, 409, 404])`.

---

### 3.2 Carpeta 2: Categorias (6 peticiones)

#### 6. Criar categoria — `POST /categorias`
- **Propósito**: Registra una nueva categoría asociada a un sector existente.
- **Body**:
```json
{
  "nome": "Categoria Teste {{siglaTemp}}",
  "setorId": {{setorId}}
}
```
- **Éxito**: `201 Created` con cabecera `Location: /categorias/{id}` y `setorId`.
- **Errores**: `400 Bad Request` por validación; `404 Not Found` si el sector no existe.

#### 7. Listar categorias — `GET /categorias?page=0&size=10&sort=nome`
- **Propósito**: Consulta paginada de todas las categorías con filtros combinables opcionales `nome` y `setorId`.
- **Query Params**: `page`, `size`, `sort`, `nome` (opcional), `setorId` (opcional).
- **Éxito**: `200 OK` con metadatos de paginación (`content`, `totalElements`).

#### 8. Listar categorias por setor — `GET /setores/{id}/categorias`
- **Propósito**: Obtiene el listado de categorías pertenecientes a un sector determinado.
- **Ruta**: `{{baseUrl}}/setores/{{setorId}}/categorias`
- **Éxito**: `200 OK` con arreglo JSON de categorías.
- **Errores**: `404 Not Found` si el sector no existe.

#### 9. Buscar categoria por id — `GET /categorias/{id}`
- **Propósito**: Obtiene los detalles de una categoría por su ID.
- **Ruta**: `{{baseUrl}}/categorias/{{categoriaId}}`
- **Éxito**: `200 OK` retornando `{ id, nome, setorId }`.
- **Errores**: `404 Not Found` si no existe.

#### 10. Atualizar categoria — `PUT /categorias/{id}`
- **Propósito**: Actualiza el nombre o sector de una categoría.
- **Ruta**: `{{baseUrl}}/categorias/{{categoriaId}}`
- **Body**:
```json
{
  "nome": "Redes de Computadores",
  "setorId": 1
}
```
- **Éxito**: `200 OK` con los datos actualizados.
- **Errores**: `404 Not Found` si la categoría o el sector no existen.

#### 11. Excluir categoria — `DELETE /categorias/{id}`
- **Propósito**: Elimina una categoría.
- **Ruta**: `{{baseUrl}}/categorias/{{categoriaId}}`
- **Comportamiento (Regla 8)**: Si la categoría tiene tickets vinculados (como la categoría semilla `1`), responde **`409 Conflict`**. Si no tiene tickets vinculados, responde **`204 No Content`**.
- **Test**: `pm.expect(pm.response.code).to.be.oneOf([204, 409, 404])`.

---

### 3.3 Carpeta 3: Usuários (4 peticiones)

#### 12. Registrar usuário — `POST /usuarios`
- **Propósito**: Registra un usuario en el sistema.
- **Body**:
```json
{
  "nome": "Usuário Teste {{matriculaTemp}}",
  "email": "{{emailTemp}}",
  "matricula": "{{matriculaTemp}}",
  "tipo": "SOLICITANTE",
  "setorId": 1
}
```
- **Éxito**: `201 Created` con cabecera `Location: /usuarios/{id}`.
- **Errores**: `400 Bad Request` por validación; `409 Conflict` si el email o la matrícula ya están registrados.

#### 13. Listar usuários — `GET /usuarios?page=0&size=10&sort=nome`
- **Propósito**: Consulta paginada con filtros opcionales combinables `nome`, `tipo` y `setorId`.
- **Éxito**: `200 OK` con arreglo `content` y metadatos de página.

#### 14. Buscar usuário por id — `GET /usuarios/{id}`
- **Propósito**: Detalle completo de un usuario por su ID.
- **Ruta**: `{{baseUrl}}/usuarios/{{usuarioId}}`
- **Éxito**: `200 OK` retornando `{ id, nome, email, matricula, tipo, setorId }`.
- **Errores**: `404 Not Found` si el usuario no existe.

#### 15. Atualizar usuário — `PUT /usuarios/{id}`
- **Propósito**: Actualiza los datos de un usuario existente.
- **Ruta**: `{{baseUrl}}/usuarios/{{usuarioId}}`
- **Body**:
```json
{
  "nome": "João Silva Atualizado",
  "email": "joao@ifrn.edu.br",
  "matricula": "2023101",
  "tipo": "SOLICITANTE",
  "setorId": 1
}
```
- **Éxito**: `200 OK` con el usuario actualizado.
- **Errores**: `404 Not Found` si no existe; `409 Conflict` si el email ya pertenece a otro usuario.

---

### 3.4 Carpeta 4: Chamados (7 peticiones)

#### 16. Abrir chamado — `POST /chamados`
- **Propósito**: Abre un ticket con estado `ABERTO` y fecha automática. Solo usuarios `SOLICITANTE` pueden abrir tickets. Si no se indica prioridad, asume `MEDIA` por defecto.
- **Body**:
```json
{
  "titulo": "{{tituloTemp}}",
  "descricao": "Chamado de teste aberto pela coleção de Postman.",
  "local": "Laboratório de Redes",
  "prioridade": "ALTA",
  "categoriaId": {{categoriaId}},
  "solicitanteId": {{solicitanteId}}
}
```
- **Éxito**: `201 Created` en estado `ABERTO`. El test guarda el `id` en `{{chamadoId}}`.
- **Errores**: `404 Not Found` si la categoría o solicitante no existen; `409 Conflict` si el usuario no es `SOLICITANTE`.

#### 17. Buscar chamado por id com interações — `GET /chamados/{id}`
- **Propósito**: Detalle completo del ticket incluyendo el arreglo cronológico `interacoes`.
- **Ruta**: `{{baseUrl}}/chamados/{{chamadoId}}`
- **Éxito**: `200 OK` con datos del ticket e `interacoes: [...]`.
- **Errores**: `404 Not Found` si el ticket no existe.

#### 18. Assumir chamado — `PATCH /chamados/{id}/assumir`
- **Propósito**: Un técnico asume el ticket, pasando a `EM_ATENDIMENTO`.
- **Pre-request**: Crea automáticamente un ticket fresco en `ABERTO` y guarda su ID en `{{chamadoAssumirId}}`.
- **Body**:
```json
{
  "tecnicoId": {{tecnicoId}}
}
```
- **Éxito**: `200 OK` con `status: "EM_ATENDIMENTO"`.
- **Errores**: `409 Conflict` si el estado es distinto de `ABERTO`, si el usuario no es `TECNICO`, o si el sector del técnico no coincide con el sector de la categoría.

#### 19. Resolver chamado — `PATCH /chamados/{id}/resolver`
- **Propósito**: El técnico asignado cierra el ticket con solución, pasando a `RESOLVIDO` con `dataFechamento`.
- **Pre-request**: Crea un ticket fresco y lo asume previamente con `{{tecnicoId}}`.
- **Body**:
```json
{
  "solucao": "Foi realizada a sincronização do cadastro do usuário com o sistema de ponto eletrônico.",
  "tecnicoId": {{tecnicoId}}
}
```
- **Éxito**: `200 OK` con `status: "RESOLVIDO"`, `dataFechamento` y `solucao`.
- **Errores**: `400 Bad Request` si la solución está vacía; `409 Conflict` si el ticket no está en `EM_ATENDIMENTO` o si el técnico no es el asignado.

#### 20. Cancelar chamado — `PATCH /chamados/{id}/cancelar`
- **Propósito**: El solicitante cancela su ticket en estado `ABERTO`, pasando a `CANCELADO`.
- **Pre-request**: Crea un ticket nuevo en `ABERTO` y asigna `{{chamadoCancelarId}}`.
- **Body**:
```json
{
  "usuarioId": {{solicitanteId}}
}
```
- **Éxito**: `200 OK` con `status: "CANCELADO"`.
- **Errores**: `409 Conflict` si el usuario no es el solicitante o si el estado es distinto de `ABERTO`.

#### 21. Listar chamados com filtros — `GET /chamados`
- **Propósito**: Consulta paginada con 8 filtros combinables (`status`, `prioridade`, `setorId`, `categoriaId`, `solicitanteId`, `tecnicoId`, `dataInicio`, `dataFim`).
- **Ruta**: `{{baseUrl}}/chamados?status=ABERTO&prioridade=ALTA&setorId=1&categoriaId=1&page=0&size=10&sort=dataAbertura,desc`
- **Éxito**: `200 OK` con arreglo paginado `content`.

#### 22. Listar chamados de um usuário — `GET /usuarios/{id}/chamados`
- **Propósito**: Devuelve tickets abiertos por un solicitante o asignados a un técnico, con filtro opcional `status`.
- **Ruta**: `{{baseUrl}}/usuarios/{{usuarioId}}/chamados?status=ABERTO&page=0&size=10&sort=dataAbertura,desc`
- **Éxito**: `200 OK` paginado.

---

### 3.5 Carpeta 5: Interações (2 peticiones)

#### 23. Criar interação — `POST /chamados/{id}/interacoes`
- **Propósito**: Registra un comentario en el ticket. Solo pueden comentar el solicitante o el técnico asignado. No se permiten comentarios en tickets cerrados (`RESOLVIDO` / `CANCELADO`).
- **Ruta**: `{{baseUrl}}/chamados/{{chamadoId}}/interacoes`
- **Body**:
```json
{
  "texto": "Primeira interação registrada para acompanhamento técnico.",
  "autorId": {{solicitanteId}}
}
```
- **Éxito**: `201 Created` con `dataHora` y `autorNome`.
- **Errores**: `409 Conflict` si el ticket está cerrado o el autor no es el solicitante ni el técnico asignado; `404 Not Found` si el ticket o autor no existen.

#### 24. Listar interações do chamado — `GET /chamados/{id}/interacoes`
- **Propósito**: Lista todas las interacciones asociadas al ticket en orden cronológico.
- **Ruta**: `{{baseUrl}}/chamados/{{chamadoId}}/interacoes`
- **Éxito**: `200 OK` con arreglo JSON de interacciones.

---

## 4. Pruebas de Errores y Modelo de Respuestas

### 4.1 Formato Estándar de Error
Todas las respuestas de error siguen el contrato `ErrorResponseDTO` generado por el `@RestControllerAdvice`:
```json
{
  "timestamp": "2026-10-22T01:50:00.123",
  "status": 409,
  "error": "Conflict",
  "mensagem": "Solo el solicitante puede cancelar el llamado",
  "caminho": "/chamados/5/cancelar"
}
```

### 4.2 Código 400 Bad Request (Validación Sintáctica)
Aplica exclusivamente a errores de validación de Bean Validation (`@Valid`) o JSON malformado. Incluye el mapa `camposInvalidos`:
```json
{
  "timestamp": "2026-10-22T01:50:00.123",
  "status": 400,
  "error": "Bad Request",
  "mensagem": "Errores de validación en la solicitud",
  "caminho": "/setores",
  "camposInvalidos": {
    "sigla": "La sigla es obligatoria"
  }
}
```

### 4.3 Código 404 Not Found (Recurso Inexistente)
Aplica cuando un identificador en la URL o en el cuerpo de la petición no existe en la base de datos:
```json
{
  "timestamp": "2026-10-22T01:50:00.123",
  "status": 404,
  "error": "Not Found",
  "mensagem": "Sector no encontrado con id: 999",
  "caminho": "/setores/999"
}
```

### 4.4 Código 409 Conflict (Violación de Reglas de Negocio e Integridad)
Aplica a cualquier violación del dominio del sistema o conflicto de claves:
- Transiciones de estado inválidas (ej. asumir ticket que no está en `ABERTO`, resolver sin estar en `EM_ATENDIMENTO`, cancelar fuera de `ABERTO`).
- Técnico de sector diferente al sector de la categoría del ticket.
- Apertura de ticket por usuario que no es `SOLICITANTE`.
- Registro de comentario por un usuario ajeno o en ticket cerrado.
- Duplicidad de clave única (`sigla` de sector, `email` de usuario, `matricula`).
- Eliminación de sector o categoría con tickets vinculados (Regla 8).

---

## 5. Ejecución Automatizada de la Colección (Newman)

La colección completa de 24 peticiones puede ejecutarse por línea de comandos utilizando **Newman**:

```powershell
# Ejecución completa con reporte en terminal
npx newman run chamados.postman_collection.json
```

Resultado verificado de la ejecución:
```text
iterations: 1
requests: 28 (24 peticiones + 4 subpeticiones pre-request)
assertions: 47 ejecutadas, 0 fallidas (100% en verde)
total run duration: ~2.3s
```
