package com.davidparragamendoza.chamados.controller;

import com.davidparragamendoza.chamados.dto.AssumirChamadoDTO;
import com.davidparragamendoza.chamados.dto.CancelarChamadoDTO;
import com.davidparragamendoza.chamados.dto.ChamadoCreateDTO;
import com.davidparragamendoza.chamados.dto.ChamadoDetalheResponseDTO;
import com.davidparragamendoza.chamados.dto.ChamadoResponseDTO;
import com.davidparragamendoza.chamados.dto.InteracaoRequestDTO;
import com.davidparragamendoza.chamados.dto.InteracaoResponseDTO;
import com.davidparragamendoza.chamados.dto.ResolverChamadoDTO;
import com.davidparragamendoza.chamados.exception.BusinessValidationException;
import com.davidparragamendoza.chamados.exception.ResourceNotFoundException;
import com.davidparragamendoza.chamados.model.PrioridadeChamado;
import com.davidparragamendoza.chamados.model.StatusChamado;
import com.davidparragamendoza.chamados.service.ChamadoService;
import com.davidparragamendoza.chamados.service.InteracaoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChamadoController.class)
class ChamadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChamadoService chamadoService;

    @MockitoBean
    private InteracaoService interacaoService;

    @Test
    void abrirChamadoRetorna201() throws Exception {
        when(chamadoService.abrir(any(ChamadoCreateDTO.class)))
                .thenReturn(chamado(StatusChamado.ABERTO, null));

        mockMvc.perform(post("/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Computadora no enciende\","
                                + "\"descricao\":\"El equipo no responde\","
                                + "\"local\":\"Laboratorio 1\","
                                + "\"prioridade\":\"ALTA\","
                                + "\"categoriaId\":10,"
                                + "\"solicitanteId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.titulo").value("Computadora no enciende"))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.dataAbertura").exists())
                .andExpect(jsonPath("$.categoriaId").value(10))
                .andExpect(jsonPath("$.solicitanteId").value(1));

        verify(chamadoService).abrir(any(ChamadoCreateDTO.class));
    }

    @Test
    void asumirChamadoRetorna200() throws Exception {
        when(chamadoService.assumir(eq(100L), any(AssumirChamadoDTO.class)))
                .thenReturn(chamado(StatusChamado.EM_ATENDIMENTO, 2L));

        mockMvc.perform(patch("/chamados/100/assumir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\":2}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.tecnicoId").value(2));

        verify(chamadoService).assumir(eq(100L), any(AssumirChamadoDTO.class));
    }

    @Test
    void asumirChamadoInvalidoRetorna409ConErrorEstandarizado() throws Exception {
        when(chamadoService.assumir(eq(100L), any(AssumirChamadoDTO.class)))
                .thenThrow(new BusinessValidationException(
                        "El chamado debe estar en estado ABERTO para ser asumido"));

        mockMvc.perform(patch("/chamados/100/assumir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\":2}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "El chamado debe estar en estado ABERTO para ser asumido"))
                .andExpect(jsonPath("$.path").value("/chamados/100/assumir"))
                .andExpect(jsonPath("$.camposInvalidos").isMap());
    }

    @Test
    void resolverChamadoRetorna200() throws Exception {
        when(chamadoService.resolver(eq(100L), any(ResolverChamadoDTO.class)))
                .thenReturn(chamado(StatusChamado.RESOLVIDO, 2L));

        mockMvc.perform(patch("/chamados/100/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solucao\":\"Se reemplazo la fuente de poder\",\"tecnicoId\":2}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("RESOLVIDO"))
                .andExpect(jsonPath("$.solucao").value("Se reemplazo la fuente de poder"))
                .andExpect(jsonPath("$.dataFechamento").exists())
                .andExpect(jsonPath("$.tecnicoId").value(2));

        verify(chamadoService).resolver(eq(100L), any(ResolverChamadoDTO.class));
    }

    @Test
    void resolverChamadoSinTecnicoIdRetorna400ConCamposInvalidos() throws Exception {
        mockMvc.perform(patch("/chamados/100/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solucao\":\"Solucion aplicada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                .andExpect(jsonPath("$.path").value("/chamados/100/resolver"))
                .andExpect(jsonPath("$.camposInvalidos.tecnicoId").value("El técnico es obligatorio"));
    }

    @Test
    void resolverChamadoPorTecnicoNoAsignadoRetorna409ConErrorEstandarizado() throws Exception {
        when(chamadoService.resolver(eq(100L), any(ResolverChamadoDTO.class)))
                .thenThrow(new BusinessValidationException(
                        "Solo el técnico asignado puede resolver el chamado"));

        mockMvc.perform(patch("/chamados/100/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solucao\":\"Solucion aplicada\",\"tecnicoId\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Solo el técnico asignado puede resolver el chamado"))
                .andExpect(jsonPath("$.path").value("/chamados/100/resolver"));
    }

    @Test
    void cancelarChamadoRetorna200() throws Exception {
        when(chamadoService.cancelar(eq(100L), any(CancelarChamadoDTO.class)))
                .thenReturn(chamado(StatusChamado.CANCELADO, null));

        mockMvc.perform(patch("/chamados/100/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":1}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        verify(chamadoService).cancelar(eq(100L), any(CancelarChamadoDTO.class));
    }

    @Test
    void cancelarChamadoNoAutorizadoRetorna409ConErrorEstandarizado() throws Exception {
        when(chamadoService.cancelar(eq(100L), any(CancelarChamadoDTO.class)))
                .thenThrow(new BusinessValidationException(
                        "Solo el solicitante o el técnico asignado pueden cancelar el chamado"));

        mockMvc.perform(patch("/chamados/100/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Solo el solicitante o el técnico asignado pueden cancelar el chamado"))
                .andExpect(jsonPath("$.path").value("/chamados/100/cancelar"));
    }

    @Test
    void listarChamadosConFiltrosRetorna200Paginado() throws Exception {
        LocalDateTime dataInicio = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime dataFim = LocalDateTime.of(2026, 12, 31, 23, 59, 59);
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dataAbertura"));
        Page<ChamadoResponseDTO> page = new PageImpl<>(
                List.of(chamado(StatusChamado.ABERTO, null)), pageable, 1);

        when(chamadoService.listar(
                eq(StatusChamado.ABERTO),
                eq(PrioridadeChamado.ALTA),
                eq(1L),
                eq(2L),
                eq(3L),
                eq(4L),
                eq(dataInicio),
                eq(dataFim),
                any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/chamados")
                        .param("status", "ABERTO")
                        .param("prioridade", "ALTA")
                        .param("setorId", "1")
                        .param("categoriaId", "2")
                        .param("solicitanteId", "3")
                        .param("tecnicoId", "4")
                        .param("dataInicio", "2026-01-01T00:00:00")
                        .param("dataFim", "2026-12-31T23:59:59")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "dataAbertura,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].status").value("ABERTO"))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(chamadoService).listar(
                eq(StatusChamado.ABERTO),
                eq(PrioridadeChamado.ALTA),
                eq(1L),
                eq(2L),
                eq(3L),
                eq(4L),
                eq(dataInicio),
                eq(dataFim),
                pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber()).isZero();
        assertThat(capturedPageable.getPageSize()).isEqualTo(5);
        assertThat(capturedPageable.getSort().getOrderFor("dataAbertura")).isNotNull();
        assertThat(capturedPageable.getSort().getOrderFor("dataAbertura").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void crearInteracaoRetorna201() throws Exception {
        when(interacaoService.criar(eq(100L), any(InteracaoRequestDTO.class)))
                .thenReturn(new InteracaoResponseDTO(
                        50L, "Comentario de seguimiento",
                        LocalDateTime.of(2026, 10, 21, 11, 0),
                        1L, "Ana Souza"));

        mockMvc.perform(post("/chamados/100/interacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Comentario de seguimiento\",\"autorId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.texto").value("Comentario de seguimiento"))
                .andExpect(jsonPath("$.autorId").value(1))
                .andExpect(jsonPath("$.autorNome").value("Ana Souza"));

        verify(interacaoService).criar(eq(100L), any(InteracaoRequestDTO.class));
    }

    @Test
    void listarInteracoesRetorna200() throws Exception {
        when(interacaoService.listarPorChamado(100L)).thenReturn(List.of(
                new InteracaoResponseDTO(
                        50L, "Primera interaccion",
                        LocalDateTime.of(2026, 10, 21, 11, 0),
                        1L, "Ana Souza"),
                new InteracaoResponseDTO(
                        51L, "Segunda interaccion",
                        LocalDateTime.of(2026, 10, 21, 12, 0),
                        2L, "Carlos Lima")));

        mockMvc.perform(get("/chamados/100/interacoes"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(jsonPath("$[1].id").value(51));

        verify(interacaoService).listarPorChamado(100L);
    }

    private ChamadoResponseDTO chamado(StatusChamado status, Long tecnicoId) {
        boolean resuelto = status == StatusChamado.RESOLVIDO;
        return new ChamadoResponseDTO(
                100L,
                "Computadora no enciende",
                "El equipo no responde",
                "Laboratorio 1",
                PrioridadeChamado.ALTA,
                status,
                LocalDateTime.of(2026, 10, 21, 10, 0),
                resuelto ? LocalDateTime.of(2026, 10, 21, 12, 0) : null,
                resuelto ? "Se reemplazo la fuente de poder" : null,
                10L,
                1L,
                tecnicoId);
    }

    private ChamadoDetalheResponseDTO chamadoDetalhe(
            StatusChamado status, Long tecnicoId, List<InteracaoResponseDTO> interacoes) {
        ChamadoResponseDTO resp = chamado(status, tecnicoId);
        return new ChamadoDetalheResponseDTO(
                resp.id(),
                resp.titulo(),
                resp.descricao(),
                resp.local(),
                resp.prioridade(),
                resp.status(),
                resp.dataAbertura(),
                resp.dataFechamento(),
                resp.solucao(),
                resp.categoriaId(),
                resp.solicitanteId(),
                resp.tecnicoId(),
                interacoes != null ? interacoes : List.of()
        );
    }

    @Test
    void buscarChamadoPorIdRetorna200ConDetalleEInteracoes() throws Exception {
        InteracaoResponseDTO interacao1 = new InteracaoResponseDTO(
                1L, "Primera interacción", LocalDateTime.of(2026, 10, 21, 10, 30), 1L, "Ana Souza");
        InteracaoResponseDTO interacao2 = new InteracaoResponseDTO(
                2L, "Segunda interacción", LocalDateTime.of(2026, 10, 21, 11, 0), 2L, "Carlos Técnico");

        when(chamadoService.buscarPorId(100L))
                .thenReturn(chamadoDetalhe(StatusChamado.EM_ATENDIMENTO, 2L, List.of(interacao1, interacao2)));

        mockMvc.perform(get("/chamados/100"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.titulo").value("Computadora no enciende"))
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.tecnicoId").value(2))
                .andExpect(jsonPath("$.interacoes").isArray())
                .andExpect(jsonPath("$.interacoes.length()").value(2))
                .andExpect(jsonPath("$.interacoes[0].texto").value("Primera interacción"))
                .andExpect(jsonPath("$.interacoes[0].autorNome").value("Ana Souza"))
                .andExpect(jsonPath("$.interacoes[1].texto").value("Segunda interacción"))
                .andExpect(jsonPath("$.interacoes[1].autorNome").value("Carlos Técnico"));

        verify(chamadoService).buscarPorId(100L);
    }

    @Test
    void buscarChamadoInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        when(chamadoService.buscarPorId(999L))
                .thenThrow(new ResourceNotFoundException("Chamado no encontrado con el ID: 999"));

        mockMvc.perform(get("/chamados/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensagem").value("Chamado no encontrado con el ID: 999"))
                .andExpect(jsonPath("$.caminho").value("/chamados/999"));

        verify(chamadoService).buscarPorId(999L);
    }

    @Test
    void abrirChamadoInvalidoRetorna400ConCamposInvalidos() throws Exception {
        mockMvc.perform(post("/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"descricao\":\"\",\"local\":\"\","
                                + "\"categoriaId\":10,\"solicitanteId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                .andExpect(jsonPath("$.path").value("/chamados"))
                .andExpect(jsonPath("$.camposInvalidos.titulo").value("El título es obligatorio"))
                .andExpect(jsonPath("$.camposInvalidos.local").value("El local es obligatorio"))
                .andExpect(jsonPath("$.camposInvalidos.descricao").value("La descripción es obligatoria"));
    }

    @Test
    void abrirChamadoComCategoriaInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        when(chamadoService.abrir(any(ChamadoCreateDTO.class)))
                .thenThrow(new ResourceNotFoundException("Categoría no encontrada con el ID: 99"));

        mockMvc.perform(post("/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Computadora no enciende\","
                                + "\"descricao\":\"El equipo no responde\","
                                + "\"local\":\"Laboratorio 1\","
                                + "\"prioridade\":\"ALTA\","
                                + "\"categoriaId\":99,"
                                + "\"solicitanteId\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Categoría no encontrada con el ID: 99"))
                .andExpect(jsonPath("$.path").value("/chamados"))
                .andExpect(jsonPath("$.camposInvalidos").isMap());
    }

    @Test
    void abrirChamadoComSolicitanteInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        when(chamadoService.abrir(any(ChamadoCreateDTO.class)))
                .thenThrow(new ResourceNotFoundException("Usuario solicitante no encontrado con el ID: 99"));

        mockMvc.perform(post("/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Computadora no enciende\","
                                + "\"descricao\":\"El equipo no responde\","
                                + "\"local\":\"Laboratorio 1\","
                                + "\"prioridade\":\"ALTA\","
                                + "\"categoriaId\":10,"
                                + "\"solicitanteId\":99}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Usuario solicitante no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.path").value("/chamados"));
    }

    @Test
    void assumirChamadoSemTecnicoIdRetorna400ConCamposInvalidos() throws Exception {
        mockMvc.perform(patch("/chamados/100/assumir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                .andExpect(jsonPath("$.path").value("/chamados/100/assumir"))
                .andExpect(jsonPath("$.camposInvalidos.tecnicoId").value("El técnico es obligatorio"));
    }

    @Test
    void assumirChamadoInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        when(chamadoService.assumir(eq(99L), any(AssumirChamadoDTO.class)))
                .thenThrow(new ResourceNotFoundException("Chamado no encontrado con el ID: 99"));

        mockMvc.perform(patch("/chamados/99/assumir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\":2}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Chamado no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.path").value("/chamados/99/assumir"));
    }

    @Test
    void resolverChamadoInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        when(chamadoService.resolver(eq(99L), any(ResolverChamadoDTO.class)))
                .thenThrow(new ResourceNotFoundException("Chamado no encontrado con el ID: 99"));

        mockMvc.perform(patch("/chamados/99/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solucao\":\"Solucion aplicada\",\"tecnicoId\":2}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Chamado no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.path").value("/chamados/99/resolver"));
    }

    @Test
    void resolverChamadoConSolucionEnBlancoRetorna400ConCamposInvalidos() throws Exception {
        mockMvc.perform(patch("/chamados/100/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solucao\":\"   \",\"tecnicoId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                .andExpect(jsonPath("$.path").value("/chamados/100/resolver"))
                .andExpect(jsonPath("$.camposInvalidos.solucao").value("La solución es obligatoria"));
    }

    @Test
    void cancelarChamadoSemUsuarioIdRetorna400ConCamposInvalidos() throws Exception {
        mockMvc.perform(patch("/chamados/100/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                .andExpect(jsonPath("$.path").value("/chamados/100/cancelar"))
                .andExpect(jsonPath("$.camposInvalidos.usuarioId").value("El usuario es obligatorio"));
    }

    @Test
    void cancelarChamadoResueltoRetorna409ConErrorEstandarizado() throws Exception {
        when(chamadoService.cancelar(eq(100L), any(CancelarChamadoDTO.class)))
                .thenThrow(new BusinessValidationException(
                        "No se puede cancelar un chamado que ya está RESOLVIDO"));

        mockMvc.perform(patch("/chamados/100/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "No se puede cancelar un chamado que ya está RESOLVIDO"))
                .andExpect(jsonPath("$.path").value("/chamados/100/cancelar"));
    }

    @Test
    void cancelarChamadoInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        when(chamadoService.cancelar(eq(99L), any(CancelarChamadoDTO.class)))
                .thenThrow(new ResourceNotFoundException("Chamado no encontrado con el ID: 99"));

        mockMvc.perform(patch("/chamados/99/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Chamado no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.path").value("/chamados/99/cancelar"));
    }

    @Test
    void criarInteracaoEmChamadoInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        when(interacaoService.criar(eq(99L), any(InteracaoRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Chamado no encontrado con el ID: 99"));

        mockMvc.perform(post("/chamados/99/interacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Comentario de seguimiento\",\"autorId\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Chamado no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.path").value("/chamados/99/interacoes"))
                .andExpect(jsonPath("$.camposInvalidos").isMap());
    }

    @Test
    void criarInteracaoInvalidaRetorna400ConCamposInvalidos() throws Exception {
        mockMvc.perform(post("/chamados/100/interacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"\",\"autorId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                .andExpect(jsonPath("$.path").value("/chamados/100/interacoes"))
                .andExpect(jsonPath("$.camposInvalidos.texto").value("El texto es obligatorio"))
                .andExpect(jsonPath("$.camposInvalidos.autorId").value("El autor es obligatorio"));
    }

    @Test
    void listarInteracoesDeChamadoInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        when(interacaoService.listarPorChamado(99L))
                .thenThrow(new ResourceNotFoundException("Chamado no encontrado con el ID: 99"));

        mockMvc.perform(get("/chamados/99/interacoes"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Chamado no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.path").value("/chamados/99/interacoes"));
    }

    @Test
    void listarChamadosSemFiltrosUsaPaginacaoPorDefecto() throws Exception {
        when(chamadoService.listar(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(),
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dataAbertura")), 0));

        mockMvc.perform(get("/chamados"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(chamadoService).listar(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("dataAbertura")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("dataAbertura").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }
}
