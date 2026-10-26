package com.davidparragamendoza.chamados.controller;

import com.davidparragamendoza.chamados.dto.ChamadoResponseDTO;
import com.davidparragamendoza.chamados.dto.UsuarioRequestDTO;
import com.davidparragamendoza.chamados.dto.UsuarioResponseDTO;
import com.davidparragamendoza.chamados.exception.BusinessConflictException;
import com.davidparragamendoza.chamados.exception.ResourceNotFoundException;
import com.davidparragamendoza.chamados.model.PrioridadeChamado;
import com.davidparragamendoza.chamados.model.StatusChamado;
import com.davidparragamendoza.chamados.model.TipoUsuario;
import com.davidparragamendoza.chamados.service.ChamadoService;
import com.davidparragamendoza.chamados.service.UsuarioService;
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
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private ChamadoService chamadoService;

    @Test
    void registrarUsuarioRetorna201ConLocation() throws Exception {
        when(usuarioService.salvar(any(UsuarioRequestDTO.class)))
                .thenReturn(new UsuarioResponseDTO(
                        1L, "Ana Souza", "ana@ifrn.edu.br", "2026001",
                        TipoUsuario.SOLICITANTE, 1L));

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Souza\",\"email\":\"ana@ifrn.edu.br\","
                                + "\"matricula\":\"2026001\",\"tipo\":\"SOLICITANTE\",\"setorId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", endsWith("/usuarios/1")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value("ana@ifrn.edu.br"))
                .andExpect(jsonPath("$.tipo").value("SOLICITANTE"))
                .andExpect(jsonPath("$.setorId").value(1));

        verify(usuarioService).salvar(any(UsuarioRequestDTO.class));
    }

    @Test
    void listarUsuariosConFiltrosRetorna200Paginado() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nome"));
        Page<UsuarioResponseDTO> page = new PageImpl<>(
                List.of(new UsuarioResponseDTO(
                        1L, "Ana Souza", "ana@ifrn.edu.br", "2026001",
                        TipoUsuario.SOLICITANTE, 1L)),
                pageable,
                1);

        when(usuarioService.listar(
                eq("Ana"), eq(TipoUsuario.SOLICITANTE), eq(1L), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/usuarios")
                        .param("nome", "Ana")
                        .param("tipo", "SOLICITANTE")
                        .param("setorId", "1")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "nome"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Ana Souza"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(usuarioService).listar(
                eq("Ana"), eq(TipoUsuario.SOLICITANTE), eq(1L), any(Pageable.class));
    }

    @Test
    void buscarUsuarioPorIdRetorna200() throws Exception {
        when(usuarioService.buscarPorId(1L)).thenReturn(new UsuarioResponseDTO(
                1L, "Ana Souza", "ana@ifrn.edu.br", "2026001",
                TipoUsuario.SOLICITANTE, 1L));

        mockMvc.perform(get("/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Ana Souza"));

        verify(usuarioService).buscarPorId(1L);
    }

    @Test
    void buscarUsuarioInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        when(usuarioService.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado con el ID: 99"));

        mockMvc.perform(get("/usuarios/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Usuario no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.path").value("/usuarios/99"))
                .andExpect(jsonPath("$.camposInvalidos").isMap());
    }

    @Test
    void listarChamadosPorUsuarioRetorna200Paginado() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dataAbertura"));
        Page<ChamadoResponseDTO> page = new PageImpl<>(List.of(new ChamadoResponseDTO(
                100L,
                "Computadora no enciende",
                "El equipo no responde",
                "Laboratorio 1",
                PrioridadeChamado.ALTA,
                StatusChamado.ABERTO,
                LocalDateTime.of(2026, 10, 21, 10, 0),
                null,
                null,
                10L,
                1L,
                null)), pageable, 1);

        when(chamadoService.listarPorUsuario(eq(1L), eq(StatusChamado.ABERTO), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/usuarios/1/chamados")
                        .param("status", "ABERTO")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "dataAbertura,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].solicitanteId").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ABERTO"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(chamadoService).listarPorUsuario(
                eq(1L), eq(StatusChamado.ABERTO), any(Pageable.class));
    }

    @Test
    void registrarUsuarioComEmailDuplicadoRetorna409ConErrorEstandarizado() throws Exception {
        when(usuarioService.salvar(any(UsuarioRequestDTO.class)))
                .thenThrow(new BusinessConflictException(
                        "Ya existe un usuario registrado con el email: ana@ifrn.edu.br"));

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Souza\",\"email\":\"ana@ifrn.edu.br\","
                                + "\"matricula\":\"2026001\",\"tipo\":\"SOLICITANTE\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Ya existe un usuario registrado con el email: ana@ifrn.edu.br"))
                .andExpect(jsonPath("$.path").value("/usuarios"))
                .andExpect(jsonPath("$.camposInvalidos").isMap());
    }

    @Test
    void registrarUsuarioInvalidoRetorna400ConCamposInvalidos() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"email-invalido\","
                                + "\"matricula\":\"\",\"tipo\":\"SOLICITANTE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                .andExpect(jsonPath("$.path").value("/usuarios"))
                .andExpect(jsonPath("$.camposInvalidos.nome").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.camposInvalidos.email").value("El email no tiene un formato válido"))
                .andExpect(jsonPath("$.camposInvalidos.matricula").value("La matrícula es obligatoria"));
    }

    @Test
    void registrarUsuarioComSetorInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        when(usuarioService.salvar(any(UsuarioRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Sector no encontrado con el ID: 99"));

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Souza\",\"email\":\"ana@ifrn.edu.br\","
                                + "\"matricula\":\"2026001\",\"tipo\":\"SOLICITANTE\",\"setorId\":99}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Sector no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.path").value("/usuarios"));
    }

    @Test
    void listarUsuariosSemFiltrosUsaPaginacaoPorDefecto() throws Exception {
        when(usuarioService.listar(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10, Sort.by("nome")), 0));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(usuarioService).listar(isNull(), isNull(), isNull(), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("nome")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("nome").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void listarChamadosPorUsuarioSemFiltroDeStatusRetorna200Paginado() throws Exception {
        when(chamadoService.listarPorUsuario(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(),
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dataAbertura")), 0));

        mockMvc.perform(get("/usuarios/1/chamados"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(chamadoService).listarPorUsuario(eq(1L), isNull(), any(Pageable.class));
    }

    @Test
    void atualizarUsuarioRetorna200() throws Exception {
        when(usuarioService.atualizar(eq(1L), any(UsuarioRequestDTO.class)))
                .thenReturn(new UsuarioResponseDTO(1L, "Ana Paula", "anapaula@ifrn.edu.br", "2026001", TipoUsuario.SOLICITANTE, null));

        mockMvc.perform(put("/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Paula\",\"email\":\"anapaula@ifrn.edu.br\","
                                + "\"matricula\":\"2026001\",\"tipo\":\"SOLICITANTE\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Ana Paula"))
                .andExpect(jsonPath("$.email").value("anapaula@ifrn.edu.br"));

        verify(usuarioService).atualizar(eq(1L), any(UsuarioRequestDTO.class));
    }

    @Test
    void atualizarUsuarioInexistenteRetorna404() throws Exception {
        when(usuarioService.atualizar(eq(99L), any(UsuarioRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado con el ID: 99"));

        mockMvc.perform(put("/usuarios/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Paula\",\"email\":\"anapaula@ifrn.edu.br\","
                                + "\"matricula\":\"2026001\",\"tipo\":\"SOLICITANTE\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensagem").value("Usuario no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.caminho").value("/usuarios/99"));

        verify(usuarioService).atualizar(eq(99L), any(UsuarioRequestDTO.class));
    }

    @Test
    void atualizarUsuarioEmailDuplicadoRetorna409() throws Exception {
        when(usuarioService.atualizar(eq(1L), any(UsuarioRequestDTO.class)))
                .thenThrow(new BusinessConflictException("Ya existe un usuario registrado con el email: carlos@ifrn.edu.br"));

        mockMvc.perform(put("/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Paula\",\"email\":\"carlos@ifrn.edu.br\","
                                + "\"matricula\":\"2026001\",\"tipo\":\"SOLICITANTE\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.mensagem").value("Ya existe un usuario registrado con el email: carlos@ifrn.edu.br"))
                .andExpect(jsonPath("$.caminho").value("/usuarios/1"));

        verify(usuarioService).atualizar(eq(1L), any(UsuarioRequestDTO.class));
    }

    @Test
    void atualizarUsuarioInvalidoRetorna400() throws Exception {
        mockMvc.perform(put("/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"email-invalido\","
                                + "\"matricula\":\"\",\"tipo\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.caminho").value("/usuarios/1"))
                .andExpect(jsonPath("$.camposInvalidos.nome").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.camposInvalidos.email").value("El email no tiene un formato válido"))
                .andExpect(jsonPath("$.camposInvalidos.matricula").value("La matrícula es obligatoria"))
                .andExpect(jsonPath("$.camposInvalidos.tipo").value("El tipo de usuario es obligatorio"));

        verify(usuarioService, never()).atualizar(anyLong(), any(UsuarioRequestDTO.class));
    }
}
