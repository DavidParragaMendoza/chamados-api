## 1. Presentación

### Sistema de Tickets (Chamados) del IFRN

Desarrollarás, de forma individual, una API REST con Spring Boot para un sistema de tickets de soporte del IFRN. El tema es sencillo a propósito: el enfoque está en aplicar bien la arquitectura en capas, la persistencia con JPA y el despliegue con Docker vistos en la asignatura.

Al final, cualquier persona debe poder clonar tu repositorio, ejecutar `docker compose up` y usar la API sin configurar nada en la máquina más allá de Docker.

---

## 2. Escenario

Hoy en día, cuando un proyector deja de funcionar o se cae el Wi-Fi del laboratorio, el funcionario o alumno avisa al sector responsable por WhatsApp, correo electrónico o en persona. No hay registro, prioridad ni historial de lo que se hizo.

La API centralizará estas solicitudes. Un solicitante abre un ticket informando el problema, la ubicación y la categoría. El ticket va al sector responsable (por ejemplo, COTIC para TI o Mantenimiento para infraestructura), donde un técnico lo asume, registra el progreso y lo cierra. El solicitante acompaña todo a través del historial del ticket.

---

## 3. Alcance

El sistema tiene cinco entidades. Los atributos a continuación son los mínimos; puedes añadir otros si lo justificas.

| Entidad | Atributos mínimos | Relaciones |
| --- | --- | --- |
| **Setor** *(Sector)* | `id`, `nome`, `sigla` (única) | tiene varias categorías y varios técnicos |
| **Categoria** *(Categoría)* | `id`, `nome`, `descrição` | pertenece a un sector |
| **Usuario** *(Usuario)* | `id`, `nome`, `email` (único), `matrícula`, `tipo` (`SOLICITANTE` | `TECNICO`) | el técnico pertenece a un sector |
| **Chamado** *(Ticket)* | `id`, `título`, `descrição`, `local`, `prioridade`, `status`, `dataAbertura`, `dataFechamento`, `solução` | tiene un solicitante, una categoría y, opcionalmente, un técnico |
| **Interacao** *(Interacción)* | `id`, `texto`, `dataHora` | pertenece a un ticket y tiene un autor (usuario) |

**Prioridade** *(Prioridad)* asume `BAIXA`, `MEDIA` o `ALTA`. **Status** *(Estado)* asume `ABERTO`, `EM_ATENDIMENTO`, `RESOLVIDO` o `CANCELADO`.

### Ciclo de vida del ticket

`ABERTO` $\rightarrow$ `EM_ATENDIMENTO` $\rightarrow$ `RESOLVIDO`, y `ABERTO` $\rightarrow$ `CANCELADO`. Ninguna otra transición está permitida.

### Reglas de negocio

Cada regla debe estar en la capa de `Service`, nunca en el `Controller`.

1. Todo ticket nace con estado `ABERTO`, `dataAbertura` completada por el sistema y prioridad `MEDIA` cuando no se informa.
2. Solo los usuarios de tipo `SOLICITANTE` pueden abrir tickets.
3. Un técnico solo puede asumir un ticket `ABERTO` cuya categoría pertenezca a su sector. Al asumirlo, el estado cambia a `EM_ATENDIMENTO`.
4. Solamente el técnico asignado puede resolver el ticket, y la solución es obligatoria. Al resolverlo, se completa `dataFechamento`.
5. Solamente el propio solicitante puede cancelar, y únicamente mientras el ticket esté `ABERTO`.
6. Los tickets en estado `RESOLVIDO` o `CANCELADO` no aceptan modificaciones ni nuevas interacciones.
7. Solo el solicitante y el técnico asignado pueden registrar interacciones en el ticket.
8. No está permitido eliminar un sector o categoría que tenga tickets vinculados.

Como el proyecto no utiliza autenticación, el usuario que ejecuta la acción se informa en el cuerpo de la petición (por ejemplo, `solicitanteId` o `tecnicoId`).

---

## 4. Requisitos funcionales

La API debe exponer, como mínimo, los endpoints detallados a continuación. Utiliza los códigos HTTP indicados.

### Registros básicos

| Recurso | Endpoints | Observación |
| --- | --- | --- |
| **Setores** *(Sectores)* | `POST`, `GET` (lista y por id), `PUT`, `DELETE` en `/setores` | regla 8 en el `DELETE` |
| **Categorias** *(Categorías)* | `POST`, `GET` (lista y por id), `PUT`, `DELETE` en `/categorias` | `GET /setores/{id}/categorias` lista las de un sector |
| **Usuários** *(Usuarios)* | `POST`, `GET` (lista y por id), `PUT` en `/usuarios` | email duplicado devuelve `409` |

### Tickets (Chamados)

| Método y ruta | Qué hace | Éxito |
| --- | --- | --- |
| `POST /chamados` | abre un ticket | `201` |
| `GET /chamados` | lista paginada con filtros (ver Paginación y filtros) | `200` |
| `GET /chamados/{id}` | detalla el ticket con sus interacciones | `200` |
| `GET /usuarios/{id}/chamados` | tickets abiertos por un solicitante o asignados a un técnico | `200` |
| `PATCH /chamados/{id}/assumir` | el técnico asume el ticket | `200` |
| `PATCH /chamados/{id}/resolver` | el técnico resuelve informando la solución | `200` |
| `PATCH /chamados/{id}/cancelar` | el solicitante cancela | `200` |
| `POST /chamados/{id}/interacoes` | registra una interacción | `201` |

### Paginación y filtros

Toda ruta de listado (`GET` que devuelve una colección) debe estar paginada y aceptar filtros opcionales por *query string*.

- Paginación con `Pageable` de Spring Data: parámetros `page`, `size` y `sort` (ejemplo: `?page=0&size=10&sort=dataAbertura,desc`). Tamaño por defecto de 10 ítems y máximo de 50.
- La respuesta trae los ítems y los metadatos de la página: número de página, tamaño, total de elementos y total de páginas.
- Filtros combinables entre sí; el filtro no informado es ignorado.

| Listado | Filtros mínimos |
| --- | --- |
| `GET /setores` | `nome` (contiene, sin diferenciar mayúsculas/minúsculas) |
| `GET /categorias` | `nome`, `setorId` |
| `GET /usuarios` | `nome`, `tipo`, `setorId` |
| `GET /chamados` | `status`, `prioridade`, `setorId`, `categoriaId`, `solicitanteId`, `tecnicoId`, período de apertura (`dataInicio` y `dataFim`) |
| `GET /usuarios/{id}/chamados` | `status` |

**Ejemplo:**

```
GET /chamados?status=ABERTO&setorId=1&prioridade=ALTA&page=0&size=10&sort=dataAbertura,desc
```

### Respuestas de error

- `400` para datos inválidos (campo obligatorio vacío, email con formato incorrecto).
- `404` para recurso inexistente.
- `409` para violación de regla de negocio (transición de estado inválida, técnico de otro sector).
- Todo error devuelve un JSON estandarizado con `timestamp`, `status`, `mensagem` y `caminho`, generado por un `@RestControllerAdvice`.

---

## 5. Requisitos técnicos obligatorios

### Stack

- Java 21 y Spring Boot 4, con Maven.
- Spring Web, Spring Data JPA y PostgreSQL.
- Se permite el uso de Lombok.

### Arquitectura en capas

- Paquetes separados para `controller`, `service`, `repository` y `model` (entidades).
- El `Controller` solo recibe la petición, llama al `Service` y devuelve la respuesta. Ninguna regla de negocio en él.
- El `Service` concentra las reglas de la sección 3 y lanza excepciones propias (por ejemplo, `RegraNegocioException`).
- Inyección de dependencias por constructor, sin `@Autowired` en atributo.
- El `Repository` extiende `JpaRepository`; filtros con métodos derivados o `@Query`.

### Persistencia

- Relaciones mapeadas con `@ManyToOne` y `@OneToMany`, enums con `@Enumerated(EnumType.STRING)`.
- Configuración de la base de datos por variables de entorno, nunca con contraseña fija en el `application.properties`.
- Esquema de la base de datos versionado con migraciones Flyway, en `src/main/resources/db/migration`. El Hibernate no crea tablas: utiliza `spring.jpa.hibernate.ddl-auto=validate`.
- Una migración por cambio, nombrada en secuencia (`V1__criar_tabelas.sql`, `V2__inserir_dados_iniciais.sql`, ...). Una migración ya aplicada nunca se edita: las correcciones entran en una nueva versión.
- Datos iniciales también en migración: al menos 2 sectores, 4 categorías y 4 usuarios.

> **Para estudiar:** las migraciones no se profundizaron en clase. Investiga cómo Flyway registra las versiones en la tabla `flyway_schema_history`, qué sucede cuando cambia el *checksum* de una migración aplicada y por qué no se usa `ddl-auto=update` en producción. Explica esto en una sección corta del `README.md`.
> 

### Docker

- `Dockerfile` *multi-stage* (*build* con Maven, ejecución con JRE).
- `docker-compose.yml` levantando la API y PostgreSQL, con volumen para los datos.
- `docker compose up` debe dejar la API respondiendo en `http://localhost:8080` sin ningún paso manual.

### Repositorio

- Público en GitHub, con historial de *commits* a lo largo del desarrollo (no un único *commit* al final).
- `README.md` con descripción del proyecto, cómo ejecutarlo y ejemplos de peticiones.
- Una colección de Postman o Insomnia (o archivo `.http`) con todas las peticiones de la sección 4.

---

## 6. Entrega

**Cómo entregar:** enlace del repositorio público en Google Classroom. Se evaluará el último *commit* en la rama `main`.

---