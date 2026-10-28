# Sistema de Chamados do IFRN — API REST

API REST para la gestión integral de tickets de soporte técnico institucional del **Instituto Federal de Educação, Ciência e Tecnologia do Rio Grande do Norte (IFRN)**. 

Desarrollada con **Java 26 / 17+** y **Spring Boot 4.1.1 / 3+**, siguiendo los principios de arquitectura limpia en capas, inyección de dependencias por constructor, persistencia estricta mediante **Spring Data JPA** y versionado determinista de base de datos con **Flyway** (`ddl-auto=validate`).

---

## 1. Arquitectura y Tecnologías

### 1.1 Stack Tecnológico
- **Lenguaje**: Java 26 (compatible con Java 17+)
- **Framework**: Spring Boot 4.1.1 (Spring Boot 3+)
- **Persistencia**: Spring Data JPA, Hibernate 7, PostgreSQL 18+
- **Migraciones de Base de Datos**: Flyway Community 12
- **Validación de Entradas**: Hibernate Validator (Bean Validation `@Valid`)
- **Productividad**: Project Lombok
- **Pruebas Automatizadas**: JUnit 5, Mockito, AssertJ, MockMvc (`@WebMvcTest`)

### 1.2 Separación de Capas
- **Controller (`controller`)**: Manejo exclusivo de la capa web HTTP, serialización DTO y códigos de estado. No contiene lógica de negocio.
- **Service (`service`)**: Concentra el 100% de la lógica de negocio, validaciones de dominio y transacciones (`@Transactional`). Lanza excepciones de dominio especializadas.
- **Repository (`repository`)**: Interfaces que extienden `JpaRepository` y `JpaSpecificationExecutor` para consultas derivadas y dinámicas.
- **Model (`model`)**: Entidades JPA en portugués (`Setor`, `Categoria`, `Usuario`, `Chamado`, `Interacao`) con enums mapeados como `STRING` (`StatusChamado`, `PrioridadeChamado`, `TipoUsuario`).
- **DTOs (`dto`)**: Clases Java `record` inmutables para desacoplar los modelos de persistencia del contrato JSON público.
- **Exception (`exception`)**: Manejo global y centralizado con `@RestControllerAdvice` (`GlobalExceptionHandler`).

---

## 2. Política de Códigos HTTP y Errores

| Código HTTP | Significado | Uso en el Sistema |
|---|---|---|
| **200 OK** | Éxito en consulta o actualización | Listados, búsquedas por ID, transiciones de estado exitosas. |
| **201 Created** | Recurso creado exitosamente | Retorna el DTO creado y la cabecera `Location` con la URI del recurso. |
| **204 No Content** | Éxito sin contenido | Eliminación exitosa de recursos (`DELETE`). |
| **400 Bad Request** | Error sintáctico o validación de entrada | Errores de Bean Validation (`@Valid`), campos obligatorios faltantes, formatos no válidos o JSON malformado. Incluye el mapa `camposInvalidos`. |
| **404 Not Found** | Recurso no encontrado | Búsqueda o referencia por ID inexistente (sector, categoría, usuario, ticket). |
| **409 Conflict** | Violación de regla de negocio o conflicto de integridad | Transiciones de estado inválidas en tickets, técnico de sector incompatible, solicitante inválido, autor de interacción no autorizado, duplicidad de claves únicas (`sigla`, `email`, `matricula`) o eliminación de sectores/categorías con tickets asociados. |

### Formato Estándar de Error JSON
```json
{
  "timestamp": "2026-10-22T01:30:00Z",
  "status": 409,
  "error": "Conflict",
  "mensagem": "O chamado não pode ser cancelado pois está no status EM_ATENDIMENTO",
  "caminho": "/chamados/1/cancelar"
}
```

---

## 3. Configuración y Variables de Entorno

Por motivos de seguridad y buenas prácticas para entornos de producción, el archivo `application.properties` **no contiene credenciales en texto plano**. La configuración de base de datos se delega obligatoriamente en variables de entorno:

| Variable | Descripción | Valor Local por Defecto / Ejemplo |
|---|---|---|
| `DB_URL` | Cadena JDBC de conexión a PostgreSQL | `jdbc:postgresql://localhost:5432/chamados_db` |
| `DB_USER` | Usuario de la base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña del usuario | `123456` |

---

## 4. Instrucciones de Ejecución

### 4.1 Configurar Variables de Entorno
En Windows (PowerShell):
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/chamados_db"
$env:DB_USER="postgres"
$env:DB_PASSWORD="123456"
```

En Linux / macOS (Bash):
```bash
export DB_URL="jdbc:postgresql://localhost:5432/chamados_db"
export DB_USER="postgres"
export DB_PASSWORD="123456"
```

### 4.2 Ejecutar Migraciones de Base de Datos con Flyway
```bash
# Windows
.\mvnw.cmd flyway:migrate

# Linux / macOS
./mvnw flyway:migrate
```

Para consultar el estado de las versiones aplicadas:
```bash
.\mvnw.cmd flyway:info
```

### 4.3 Ejecutar la Suite de Pruebas Automatizadas
La suite contiene más de 150 pruebas unitarias y de integración que validan controladores (`@WebMvcTest`), servicios, repositorios y el contexto de Spring:
```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

### 4.4 Iniciar la Aplicación en Modo Desarrollo
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
La API estará disponible y escuchando en `http://localhost:8080`.

---

## 5. Estudio sobre Migraciones de Base de Datos con Flyway

### 5.1 ¿Cómo registra Flyway las versiones en `flyway_schema_history`?
Flyway utiliza un enfoque de **migraciones basadas en estado versionado** (*migration-based schema management*). Para llevar el control estricto de qué cambios se han ejecutado, Flyway crea automáticamente en el esquema de base de datos una tabla de metadatos denominada `flyway_schema_history`.

Cada vez que se aplica un script de migración SQL (`V1__...`, `V2__...`, etc.), Flyway registra una nueva fila en esta tabla con las siguientes columnas clave:
1. **`installed_rank` (INTEGER)**: Orden secuencial en el que las migraciones fueron instaladas físicamente.
2. **`version` (VARCHAR)**: Número de versión extraído del prefijo del archivo (ej. `1`, `2`, `6`, `7`). Debe ser único en la tabla.
3. **`description` (VARCHAR)**: Texto descriptivo legible derivado del nombre del archivo (ej. `create table setores`).
4. **`type` (VARCHAR)**: Tipo de migración (`SQL`, `JDBC`, etc.).
5. **`script` (VARCHAR)**: Nombre exacto del archivo SQL en el repositorio (ej. `V1__create_table_setores.sql`).
6. **`checksum` (INTEGER)**: Valor hash numérico (calculado mediante algoritmo CRC32 / SHA-256 sobre el contenido exacto del archivo SQL).
7. **`installed_by` (VARCHAR)**: Usuario de base de datos que ejecutó la migración.
8. **`installed_on` (TIMESTAMP)**: Marca de tiempo exacta de cuándo se ejecutó el script.
9. **`execution_time` (INTEGER)**: Tiempo en milisegundos que tardó la ejecución del script.
10. **`success` (BOOLEAN)**: `true` si la migración finalizó exitosamente; `false` si falló.

Cuando la aplicación inicia o se ejecuta `flyway:migrate`, Flyway lee `flyway_schema_history`, compara las versiones registradas con los archivos presentes en `src/main/resources/db/migration`, y aplica únicamente aquellas migraciones cuya versión sea superior a la última registrada.

---

### 5.2 ¿Qué sucede cuando cambia el *checksum* de una migración ya aplicada?
El **checksum** garantiza la **inmutabilidad** del historial de base de datos. Representa la "firma digital" del contenido de la migración en el momento en que se ejecutó.

Si un desarrollador altera retroactivamente un archivo de migración que ya fue aplicado en una base de datos (por ejemplo, corrigiendo un tipo de dato, añadiendo un `INSERT` o simplemente modificando un espacio o comentario en `V6__insert_seed_data.sql`), el checksum calculado en el próximo inicio de la aplicación diferirá del checksum guardado en `flyway_schema_history`.

**Consecuencia inmediata**:
Flyway lanza una excepción de validación fatal y aborta el arranque de la aplicación:
```text
FlywayValidateException: Validate failed: Migrations have failed validation
Migration checksum mismatch for migration version 6:
  -> Applied to database : 1827401923
  -> Resolved locally    : 4918271048
```
Esto previene que diferentes entornos (desarrollo, pruebas, *staging*, producción) queden en estados asimétricos e indeterministas.

**Regla de Oro en Flyway**:
> **Una migración ya aplicada nunca se edita.** Cualquier corrección, nueva tabla, alteración de columnas o datos semilla adicionales debe implementarse creando un **nuevo archivo de migración con una versión superior** (por ejemplo, `V7__insert_usuario_seed.sql`).

---

### 5.3 ¿Por qué NO se debe usar `spring.jpa.hibernate.ddl-auto=update` en producción?
La propiedad `spring.jpa.hibernate.ddl-auto=update` instruye a Hibernate para que inspeccione las entidades Java JPA al arrancar e intente alterar automáticamente las tablas de la base de datos para hacerlas coincidir con las clases.

Aunque resulta cómodo en fases tempranas de prototipado o pruebas de laboratorio, su uso en entornos de producción está **terminantemente desaconsejado** por los siguientes motivos críticos:

1. **Riesgo severo de pérdida o corrupción de datos**:
   - Hibernate solo agrega columnas nuevas o índices básicos; **no renombra columnas ni tablas**. Si se renombra un atributo en una entidad Java (ej. de `telefone` a `celular`), Hibernate crea una nueva columna `celular` vacía y deja la columna original `telefone` huérfana con los datos históricos inaccesibles.
   - No puede gestionar transformaciones de datos complejas (como migrar datos entre tablas normalizadas o cambiar tipos de datos con conversión explícita).

2. **Falta de auditoría, trazabilidad y control de versiones**:
   - Los cambios aplicados por Hibernate no quedan registrados en ningún log de auditoría ni script SQL reproducible. Resulta imposible saber qué usuario, en qué fecha o qué versión de código modificó la estructura de la base de datos.
   - No permite realizar *rollbacks* controlados ante despliegues fallidos.

3. **Bloqueos de tablas y caídas de servicio**:
   - Hibernate puede ejecutar instrucciones `ALTER TABLE` que bloquean de forma exclusiva (*exclusive lock*) tablas con millones de registros durante el arranque, degradando el rendimiento o provocando caídas de servicio (*downtime*) no planificadas.

4. **Incompatibilidad con arquitecturas distribuidas y escalado horizontal**:
   - En despliegues modernos (Kubernetes, AWS ECS, Docker Swarm), múltiples réplicas de la misma API arrancan simultáneamente. Si todas intentan ejecutar `ddl-auto=update` en paralelo sobre la misma base de datos, se producen **condiciones de carrera (*race conditions*)**, bloqueos mutuos (*deadlocks*) y estados corruptos en el catálogo del motor de base de datos.

5. **El Estándar Profesional**:
   - En producción se utiliza estrictamente `spring.jpa.hibernate.ddl-auto=validate`. Hibernate únicamente valida que el esquema existente en la base de datos coincida con el modelo de objetos JPA. Toda modificación al esquema debe realizarse de forma explícita, determinista y auditable mediante scripts SQL versionados con **Flyway**.

---

## 6. Secuencia de Migraciones del Proyecto

El esquema del sistema se gestiona con 7 versiones secuenciales en `src/main/resources/db/migration`:

1. **`V1__create_table_setores.sql`**: Creación de la tabla `setores` (columnas `id`, `nome`, `sigla` con restricción `UNIQUE`, `ativo`).
2. **`V2__create_table_categorias.sql`**: Creación de la tabla `categorias` (columnas `id`, `nome`, clave foránea `setor_id`).
3. **`V3__create_table_usuarios.sql`**: Creación de la tabla `usuarios` (columnas `id`, `nome`, `email` y `matricula` con restricciones `UNIQUE`, `tipo`, clave foránea `setor_id`).
4. **`V4__create_table_chamados.sql`**: Creación de la tabla `chamados` (columnas `id`, `titulo`, `descricao`, `local`, `prioridade`, `status`, `data_abertura`, `data_fechamento`, `solucao`, claves foráneas a categoría, solicitante y técnico).
5. **`V5__create_table_interacoes.sql`**: Creación de la tabla `interacoes` (columnas `id`, `texto`, `data_hora`, claves foráneas a chamado y autor).
6. **`V6__insert_seed_data.sql`**: Inserción de datos semilla iniciales: 3 sectores (`TI`, `RH`, `INFRA`), 5 categorías (`Redes`, `Hardware`, `Sistemas`, `Folha de Pagamento`, `Elétrica`), 3 usuarios iniciales y tickets de prueba.
7. **`V7__insert_usuario_seed.sql`**: Inserción de usuario adicional (`Ana Lima`, `SOLICITANTE`, sector `RH`) para garantizar el cumplimiento del requisito de al menos 4 usuarios iniciales, respetando la inmutabilidad de la migración `V6` previamente aplicada.

---

## 7. Catálogo y Ejemplos de Peticiones

### 7.1 Sectores (`/setores`)

#### Crear Sector
- **Método**: `POST /setores`
- **Body**:
```json
{
  "nome": "Recursos Humanos",
  "sigla": "RH"
}
```
- **Respuesta**: `201 Created` con cabecera `Location: /setores/2`.

#### Listar Sectores Paginado
- **Método**: `GET /setores?page=0&size=10&sort=nome,asc`
- **Respuesta**: `200 OK` con listado paginado.

#### Buscar Sector por ID
- **Método**: `GET /setores/1`
- **Respuesta**: `200 OK` con el DTO del sector.

#### Actualizar Sector
- **Método**: `PUT /setores/1`
- **Body**:
```json
{
  "nome": "Tecnologia da Informação e Comunicação",
  "sigla": "TIC",
  "ativo": true
}
```
- **Respuesta**: `200 OK`.

---

### 7.2 Categorías (`/categorias`)

#### Crear Categoría
- **Método**: `POST /categorias`
- **Body**:
```json
{
  "nome": "Redes de Computadores",
  "setorId": 1
}
```
- **Respuesta**: `201 Created`.

#### Listar Categorías con Filtros
- **Método**: `GET /categorias?nome=Redes&setorId=1&page=0&size=10`
- **Respuesta**: `200 OK`.

#### Buscar Categoría por ID
- **Método**: `GET /categorias/1`
- **Respuesta**: `200 OK`.

#### Actualizar Categoría
- **Método**: `PUT /categorias/1`
- **Body**:
```json
{
  "nome": "Infraestrutura de Redes",
  "setorId": 1
}
```
- **Respuesta**: `200 OK`.

---

### 7.3 Usuarios (`/usuarios`)

#### Registrar Usuario
- **Método**: `POST /usuarios`
- **Body**:
```json
{
  "nome": "João Silva",
  "email": "joao@ifrn.edu.br",
  "matricula": "2023101",
  "tipo": "SOLICITANTE",
  "setorId": 1
}
```
- **Respuesta**: `201 Created`.

#### Listar Usuarios con Filtros
- **Método**: `GET /usuarios?nome=João&tipo=SOLICITANTE&setorId=1`
- **Respuesta**: `200 OK`.

#### Buscar Usuario por ID
- **Método**: `GET /usuarios/1`
- **Respuesta**: `200 OK`.

#### Actualizar Usuario
- **Método**: `PUT /usuarios/1`
- **Body**:
```json
{
  "nome": "João Silva Atualizado",
  "email": "joao.silva@ifrn.edu.br",
  "matricula": "2023101",
  "tipo": "SOLICITANTE",
  "setorId": 1
}
```
- **Respuesta**: `200 OK`.

---

### 7.4 Chamados (`/chamados`)

#### Abrir Chamado
- **Método**: `POST /chamados`
- **Body** (prioridad opcional, asume `MEDIA` por defecto):
```json
{
  "titulo": "Falha na conexão com a internet",
  "descricao": "O computador não obtém endereço IP via DHCP na sala 102.",
  "local": "Bloco B - Sala 102",
  "categoriaId": 1,
  "solicitanteId": 1
}
```
- **Respuesta**: `201 Created`.

#### Detalle de Chamado con Interacciones
- **Método**: `GET /chamados/1`
- **Respuesta**: `200 OK`
```json
{
  "id": 1,
  "titulo": "Falha na conexão com a internet",
  "descricao": "O computador não obtém endereço IP via DHCP na sala 102.",
  "local": "Bloco B - Sala 102",
  "prioridade": "MEDIA",
  "status": "ABERTO",
  "dataAbertura": "2026-10-22T01:10:00",
  "dataFechamento": null,
  "solucao": null,
  "categoria": { "id": 1, "nome": "Redes", "setorId": 1 },
  "solicitante": { "id": 1, "nome": "João Silva", "email": "joao@ifrn.edu.br", "matricula": "2023101", "tipo": "SOLICITANTE", "setorId": 1 },
  "tecnico": null,
  "interacoes": []
}
```

#### Listar Chamados con Filtros
- **Método**: `GET /chamados?status=ABERTO&prioridade=MEDIA&categoriaId=1`
- **Respuesta**: `200 OK` (paginado, tamaño por defecto 10, máximo 50).

#### Asumir Chamado (Técnico del mismo sector)
- **Método**: `PATCH /chamados/1/assumir`
- **Body**:
```json
{
  "tecnicoId": 2
}
```
- **Respuesta**: `200 OK` (Estado pasa a `EM_ATENDIMENTO`).

#### Registrar Interacción
- **Método**: `POST /chamados/1/interacoes`
- **Body**:
```json
{
  "texto": "Estou me deslocando para a sala 102 para testar o ponto de rede.",
  "autorId": 2
}
```
- **Respuesta**: `201 Created`.

#### Resolver Chamado
- **Método**: `PATCH /chamados/1/resolver`
- **Body**:
```json
{
  "tecnicoId": 2,
  "solucao": "Cabo de rede substituído e conectividade restabelecida com sucesso."
}
```
- **Respuesta**: `200 OK` (Estado pasa a `RESOLVIDO` con `dataFechamento` y `solucao`).

#### Cancelar Chamado (Solo por el solicitante en estado ABERTO)
- **Método**: `PATCH /chamados/1/cancelar`
- **Body**:
```json
{
  "solicitanteId": 1
}
```
- **Respuesta**: `200 OK` (Estado pasa a `CANCELADO`).
