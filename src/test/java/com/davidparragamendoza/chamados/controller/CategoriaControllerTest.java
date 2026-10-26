package com.davidparragamendoza.chamados.controller;

import com.davidparragamendoza.chamados.dto.CategoriaRequestDTO;
import com.davidparragamendoza.chamados.dto.CategoriaResponseDTO;
import com.davidparragamendoza.chamados.exception.BusinessConflictException;
import com.davidparragamendoza.chamados.exception.ResourceNotFoundException;
import com.davidparragamendoza.chamados.service.CategoriaService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoriaController.class)
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoriaService categoriaService;

    @Test
    void crearCategoriaRetorna201() throws Exception {
        when(categoriaService.salvar(any(CategoriaRequestDTO.class)))
                .thenReturn(new CategoriaResponseDTO(10L, "Hardware", 1L));

        mockMvc.perform(post("/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Hardware\",\"setorId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nome").value("Hardware"))
                .andExpect(jsonPath("$.setorId").value(1));

        verify(categoriaService).salvar(any(CategoriaRequestDTO.class));
    }

    @Test
    void listarCategoriasPorSetorRetorna200() throws Exception {
        when(categoriaService.listarPorSetor(1L)).thenReturn(List.of(
                new CategoriaResponseDTO(10L, "Hardware", 1L),
                new CategoriaResponseDTO(11L, "Software", 1L)));

        mockMvc.perform(get("/setores/1/categorias"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].nome").value("Hardware"))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].nome").value("Software"));

        verify(categoriaService).listarPorSetor(1L);
    }

    @Test
    void eliminarCategoriaRetorna204() throws Exception {
        mockMvc.perform(delete("/categorias/10"))
                .andExpect(status().isNoContent());

        verify(categoriaService).deletar(10L);
    }

    @Test
    void eliminarCategoriaConTicketsRetorna409ConErrorEstandarizado() throws Exception {
        doThrow(new BusinessConflictException(
                "No se puede eliminar la categoria porque tiene chamados asociados"))
                .when(categoriaService).deletar(10L);

        mockMvc.perform(delete("/categorias/10"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "No se puede eliminar la categoria porque tiene chamados asociados"))
                .andExpect(jsonPath("$.path").value("/categorias/10"))
                .andExpect(jsonPath("$.camposInvalidos").isMap());
    }

    @Test
    void criarCategoriaComSetorInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        when(categoriaService.salvar(any(CategoriaRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Sector no encontrado con el ID: 99"));

        mockMvc.perform(post("/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Hardware\",\"setorId\":99}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Sector no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.path").value("/categorias"))
                .andExpect(jsonPath("$.camposInvalidos").isMap());
    }

    @Test
    void criarCategoriaInvalidaRetorna400ConCamposInvalidos() throws Exception {
        mockMvc.perform(post("/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"setorId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                .andExpect(jsonPath("$.path").value("/categorias"))
                .andExpect(jsonPath("$.camposInvalidos.nome").value("El nombre es obligatorio"));
    }

    @Test
    void listarCategoriasDeSetorInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        when(categoriaService.listarPorSetor(99L))
                .thenThrow(new ResourceNotFoundException("Sector no encontrado con el ID: 99"));

        mockMvc.perform(get("/setores/99/categorias"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Sector no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.path").value("/setores/99/categorias"));
    }

    @Test
    void eliminarCategoriaInexistenteRetorna404ConErrorEstandarizado() throws Exception {
        doThrow(new ResourceNotFoundException("Categoría no encontrada con el ID: 99"))
                .when(categoriaService).deletar(99L);

        mockMvc.perform(delete("/categorias/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Categoría no encontrada con el ID: 99"))
                .andExpect(jsonPath("$.path").value("/categorias/99"));
    }

    @Test
    void buscarCategoriaPorIdRetorna200() throws Exception {
        when(categoriaService.buscarPorId(10L))
                .thenReturn(new CategoriaResponseDTO(10L, "Hardware", 1L));

        mockMvc.perform(get("/categorias/10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nome").value("Hardware"))
                .andExpect(jsonPath("$.setorId").value(1));

        verify(categoriaService).buscarPorId(10L);
    }

    @Test
    void buscarCategoriaPorIdInexistenteRetorna404() throws Exception {
        when(categoriaService.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Categoría no encontrada con el ID: 99"));

        mockMvc.perform(get("/categorias/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensagem").value("Categoría no encontrada con el ID: 99"))
                .andExpect(jsonPath("$.caminho").value("/categorias/99"));

        verify(categoriaService).buscarPorId(99L);
    }

    @Test
    void atualizarCategoriaRetorna200() throws Exception {
        when(categoriaService.atualizar(eq(10L), any(CategoriaRequestDTO.class)))
                .thenReturn(new CategoriaResponseDTO(10L, "Hardware Avanzado", 1L));

        mockMvc.perform(put("/categorias/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Hardware Avanzado\",\"setorId\":1}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nome").value("Hardware Avanzado"))
                .andExpect(jsonPath("$.setorId").value(1));

        verify(categoriaService).atualizar(eq(10L), any(CategoriaRequestDTO.class));
    }

    @Test
    void atualizarCategoriaInexistenteRetorna404() throws Exception {
        when(categoriaService.atualizar(eq(99L), any(CategoriaRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Categoría no encontrada con el ID: 99"));

        mockMvc.perform(put("/categorias/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Hardware Avanzado\",\"setorId\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensagem").value("Categoría no encontrada con el ID: 99"))
                .andExpect(jsonPath("$.caminho").value("/categorias/99"));

        verify(categoriaService).atualizar(eq(99L), any(CategoriaRequestDTO.class));
    }

    @Test
    void atualizarCategoriaSetorInexistenteRetorna404() throws Exception {
        when(categoriaService.atualizar(eq(10L), any(CategoriaRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Sector no encontrado con el ID: 99"));

        mockMvc.perform(put("/categorias/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Hardware Avanzado\",\"setorId\":99}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensagem").value("Sector no encontrado con el ID: 99"))
                .andExpect(jsonPath("$.caminho").value("/categorias/10"));

        verify(categoriaService).atualizar(eq(10L), any(CategoriaRequestDTO.class));
    }

    @Test
    void atualizarCategoriaInvalidaRetorna400() throws Exception {
        mockMvc.perform(put("/categorias/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"setorId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.caminho").value("/categorias/10"))
                .andExpect(jsonPath("$.camposInvalidos.nome").value("El nombre es obligatorio"));

        verify(categoriaService, never()).atualizar(anyLong(), any(CategoriaRequestDTO.class));
    }

    @Test
    void listarCategoriasPaginadoConFiltrosRetorna200() throws Exception {
        Page<CategoriaResponseDTO> page = new PageImpl<>(
                List.of(new CategoriaResponseDTO(10L, "Hardware", 1L)),
                PageRequest.of(0, 10, Sort.by("nome")),
                1);

        when(categoriaService.listar(eq("Hardware"), eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/categorias")
                        .param("nome", "Hardware")
                        .param("setorId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].nome").value("Hardware"))
                .andExpect(jsonPath("$.content[0].setorId").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(categoriaService).listar(eq("Hardware"), eq(1L), any(Pageable.class));
    }

    @Test
    void listarCategoriasSemFiltrosUsaPaginacaoPorDefecto() throws Exception {
        when(categoriaService.listar(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10, Sort.by("nome")), 0));

        mockMvc.perform(get("/categorias"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(categoriaService).listar(isNull(), isNull(), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("nome")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("nome").getDirection()).isEqualTo(Sort.Direction.ASC);
    }
}
