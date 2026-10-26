package com.davidparragamendoza.chamados.controller;

import com.davidparragamendoza.chamados.dto.SetorRequestDTO;
import com.davidparragamendoza.chamados.dto.SetorResponseDTO;
import com.davidparragamendoza.chamados.exception.BusinessConflictException;
import com.davidparragamendoza.chamados.exception.ResourceNotFoundException;
import com.davidparragamendoza.chamados.service.SetorService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SetorController.class)
class SetorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SetorService setorService;

    @Test
    void crearSetorRetorna201ConLocation() throws Exception {
        when(setorService.salvar(any(SetorRequestDTO.class)))
                .thenReturn(new SetorResponseDTO(1L, "Tecnologia", "TI", true));

        mockMvc.perform(post("/setores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Tecnologia\",\"sigla\":\"TI\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", endsWith("/setores/1")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Tecnologia"))
                .andExpect(jsonPath("$.sigla").value("TI"))
                .andExpect(jsonPath("$.ativo").value(true));

        verify(setorService).salvar(any(SetorRequestDTO.class));
    }

    @Test
    void listarSetoresRetorna200Paginado() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nome"));
        Page<SetorResponseDTO> page = new PageImpl<>(
                List.of(new SetorResponseDTO(1L, "Tecnologia", "TI", true)),
                pageable,
                1);

        when(setorService.listarTodos(isNull(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/setores")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "nome"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].sigla").value("TI"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(setorService).listarTodos(isNull(), any(Pageable.class));
    }

    @Test
    void eliminarSetorRetorna204() throws Exception {
        mockMvc.perform(delete("/setores/1"))
                .andExpect(status().isNoContent());

        verify(setorService).deletar(1L);
    }

    @Test
    void eliminarSetorConRelacionesRetorna409ConErrorEstandarizado() throws Exception {
        doThrow(new BusinessConflictException(
                "No se puede eliminar el sector porque tiene categorias o usuarios asociados"))
                .when(setorService).deletar(1L);

        mockMvc.perform(delete("/setores/1"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "No se puede eliminar el sector porque tiene categorias o usuarios asociados"))
                .andExpect(jsonPath("$.path").value("/setores/1"))
                .andExpect(jsonPath("$.camposInvalidos").isMap());
    }

    @Test
    void crearSetorInvalidoRetorna400ConCamposInvalidos() throws Exception {
        mockMvc.perform(post("/setores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"sigla\":\"TI\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                .andExpect(jsonPath("$.path").value("/setores"))
                .andExpect(jsonPath("$.camposInvalidos.nome").value("El nombre es obligatorio"));
    }

    @Test
    void criarSetorConSiglaDuplicadaRetorna409ConErrorEstandarizado() throws Exception {
        when(setorService.salvar(any(SetorRequestDTO.class)))
                .thenThrow(new BusinessConflictException(
                        "Ya existe un sector registrado con la sigla: TI"));

        mockMvc.perform(post("/setores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Tecnologia\",\"sigla\":\"TI\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Ya existe un sector registrado con la sigla: TI"))
                .andExpect(jsonPath("$.path").value("/setores"))
                .andExpect(jsonPath("$.camposInvalidos").isMap());
    }

    @Test
    void eliminarSetorInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        doThrow(new ResourceNotFoundException("Sector no encontrado con el ID: 99"))
                .when(setorService).deletar(99L);

        mockMvc.perform(delete("/setores/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Sector no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.path").value("/setores/99"))
                .andExpect(jsonPath("$.camposInvalidos").isMap());
    }

    @Test
    void listarSetoresComFiltroNomeRetorna200YPropagaElFiltro() throws Exception {
        Page<SetorResponseDTO> page = new PageImpl<>(
                List.of(new SetorResponseDTO(1L, "Tecnologia", "TI", true)),
                PageRequest.of(0, 10, Sort.by("nome")),
                1);

        when(setorService.listarTodos(eq("Tec"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/setores")
                        .param("nome", "Tec"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Tecnologia"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(setorService).listarTodos(eq("Tec"), any(Pageable.class));
    }

    @Test
    void listarSetoresSinParametrosUsaPaginacaoPorDefecto() throws Exception {
        when(setorService.listarTodos(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10, Sort.by("nome")), 0));

        mockMvc.perform(get("/setores"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(setorService).listarTodos(isNull(), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("nome")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("nome").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void buscarSetorPorIdRetorna200() throws Exception {
        when(setorService.buscarPorId(1L))
                .thenReturn(new SetorResponseDTO(1L, "Tecnologia", "TI", true));

        mockMvc.perform(get("/setores/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Tecnologia"))
                .andExpect(jsonPath("$.sigla").value("TI"));

        verify(setorService).buscarPorId(1L);
    }

    @Test
    void buscarSetorPorIdInexistenteRetorna404() throws Exception {
        when(setorService.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Sector no encontrado con el ID: 99"));

        mockMvc.perform(get("/setores/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensagem").value("Sector no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.caminho").value("/setores/99"));

        verify(setorService).buscarPorId(99L);
    }

    @Test
    void atualizarSetorRetorna200() throws Exception {
        when(setorService.atualizar(eq(1L), any(SetorRequestDTO.class)))
                .thenReturn(new SetorResponseDTO(1L, "Tecnologia Atualizada", "TI2", true));

        mockMvc.perform(put("/setores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Tecnologia Atualizada\",\"sigla\":\"TI2\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Tecnologia Atualizada"))
                .andExpect(jsonPath("$.sigla").value("TI2"));

        verify(setorService).atualizar(eq(1L), any(SetorRequestDTO.class));
    }

    @Test
    void atualizarSetorInexistenteRetorna404() throws Exception {
        when(setorService.atualizar(eq(99L), any(SetorRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Sector no encontrado con el ID: 99"));

        mockMvc.perform(put("/setores/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Tecnologia Atualizada\",\"sigla\":\"TI2\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensagem").value("Sector no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.caminho").value("/setores/99"));

        verify(setorService).atualizar(eq(99L), any(SetorRequestDTO.class));
    }

    @Test
    void atualizarSetorSiglaDuplicadaRetorna409() throws Exception {
        when(setorService.atualizar(eq(1L), any(SetorRequestDTO.class)))
                .thenThrow(new BusinessConflictException("Ya existe un sector registrado con la sigla: RH"));

        mockMvc.perform(put("/setores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Tecnologia\",\"sigla\":\"RH\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.mensagem").value("Ya existe un sector registrado con la sigla: RH"))
                .andExpect(jsonPath("$.caminho").value("/setores/1"));

        verify(setorService).atualizar(eq(1L), any(SetorRequestDTO.class));
    }

    @Test
    void atualizarSetorInvalidoRetorna400() throws Exception {
        mockMvc.perform(put("/setores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"sigla\":\"TI\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.caminho").value("/setores/1"))
                .andExpect(jsonPath("$.camposInvalidos.nome").value("El nombre es obligatorio"));

        verify(setorService, never()).atualizar(anyLong(), any(SetorRequestDTO.class));
    }
}
