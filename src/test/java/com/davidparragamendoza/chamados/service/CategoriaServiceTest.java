package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.CategoriaRequestDTO;
import com.davidparragamendoza.chamados.dto.CategoriaResponseDTO;
import com.davidparragamendoza.chamados.exception.BusinessConflictException;
import com.davidparragamendoza.chamados.exception.ResourceNotFoundException;
import com.davidparragamendoza.chamados.model.Categoria;
import com.davidparragamendoza.chamados.model.Setor;
import com.davidparragamendoza.chamados.repository.CategoriaRepository;
import com.davidparragamendoza.chamados.repository.SetorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private SetorRepository setorRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    @Test
    void crearCategoriaExitosamente() {
        Setor setor = Setor.builder().id(1L).nome("Tecnología de la Información").sigla("TI").ativo(true).build();
        CategoriaRequestDTO request = new CategoriaRequestDTO("Hardware", 1L);

        when(setorRepository.findById(1L)).thenReturn(Optional.of(setor));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocation -> {
            Categoria categoria = invocation.getArgument(0);
            categoria.setId(10L);
            return categoria;
        });

        CategoriaResponseDTO response = categoriaService.salvar(request);

        assertEquals(10L, response.id());
        assertEquals("Hardware", response.nome());
        assertEquals(1L, response.setorId());

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).save(captor.capture());
        assertEquals("Hardware", captor.getValue().getNome());
        assertEquals(1L, captor.getValue().getSetor().getId());
    }

    @Test
    void rechazarCreacionConSetorInexistente() {
        CategoriaRequestDTO request = new CategoriaRequestDTO("Hardware", 99L);

        when(setorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoriaService.salvar(request));

        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void listarCategoriasPorSetor() {
        Setor setor = Setor.builder().id(1L).nome("Tecnología de la Información").sigla("TI").ativo(true).build();
        List<Categoria> categorias = List.of(
                Categoria.builder().id(10L).nome("Hardware").setor(setor).build(),
                Categoria.builder().id(11L).nome("Software").setor(setor).build());

        when(setorRepository.existsById(1L)).thenReturn(true);
        when(categoriaRepository.findBySetorId(1L)).thenReturn(categorias);

        List<CategoriaResponseDTO> response = categoriaService.listarPorSetor(1L);

        assertEquals(2, response.size());
        assertEquals(10L, response.get(0).id());
        assertEquals("Hardware", response.get(0).nome());
        assertEquals(1L, response.get(0).setorId());
        assertEquals(11L, response.get(1).id());
        assertEquals("Software", response.get(1).nome());
        assertEquals(1L, response.get(1).setorId());
    }

    @Test
    void rechazarListadoDeCategoriasDeSetorInexistente() {
        when(setorRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> categoriaService.listarPorSetor(99L));

        verify(categoriaRepository, never()).findBySetorId(anyLong());
    }

    @Test
    void eliminarCategoriaExitosamente() {
        when(categoriaRepository.existsById(10L)).thenReturn(true);
        when(categoriaRepository.existsChamadoByCategoriaId(10L)).thenReturn(false);

        categoriaService.deletar(10L);

        verify(categoriaRepository).deleteById(10L);
    }

    @Test
    void bloquearEliminacionPorChamadosAsociados() {
        when(categoriaRepository.existsById(10L)).thenReturn(true);
        when(categoriaRepository.existsChamadoByCategoriaId(10L)).thenReturn(true);

        assertThrows(BusinessConflictException.class, () -> categoriaService.deletar(10L));

        verify(categoriaRepository, never()).deleteById(anyLong());
    }

    @Test
    void rechazarEliminacionDeCategoriaInexistente() {
        when(categoriaRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> categoriaService.deletar(99L));

        verify(categoriaRepository, never()).existsChamadoByCategoriaId(anyLong());
        verify(categoriaRepository, never()).deleteById(anyLong());
    }

    @Test
    void buscarPorIdExitoso() {
        Setor setor = Setor.builder().id(1L).nome("TI").sigla("TI").ativo(true).build();
        Categoria categoria = Categoria.builder().id(10L).nome("Hardware").setor(setor).build();

        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));

        CategoriaResponseDTO response = categoriaService.buscarPorId(10L);

        assertEquals(10L, response.id());
        assertEquals("Hardware", response.nome());
        assertEquals(1L, response.setorId());
        verify(categoriaRepository).findById(10L);
    }

    @Test
    void buscarPorIdInexistenteLanzaNotFound() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoriaService.buscarPorId(99L));
        verify(categoriaRepository).findById(99L);
    }

    @Test
    void atualizarCategoriaExitosamente() {
        Setor setorAntigo = Setor.builder().id(1L).nome("TI").sigla("TI").ativo(true).build();
        Setor setorNovo = Setor.builder().id(2L).nome("Redes").sigla("RED").ativo(true).build();
        Categoria categoria = Categoria.builder().id(10L).nome("Hardware").setor(setorAntigo).build();
        CategoriaRequestDTO dto = new CategoriaRequestDTO("Infraestructura", 2L);

        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(setorRepository.findById(2L)).thenReturn(Optional.of(setorNovo));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));

        CategoriaResponseDTO response = categoriaService.atualizar(10L, dto);

        assertEquals("Infraestructura", response.nome());
        assertEquals(2L, response.setorId());
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void atualizarCategoriaInexistenteLanzaNotFound() {
        CategoriaRequestDTO dto = new CategoriaRequestDTO("Infraestructura", 2L);
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoriaService.atualizar(99L, dto));
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void atualizarCategoriaSetorInexistenteLanzaNotFound() {
        Setor setor = Setor.builder().id(1L).nome("TI").sigla("TI").ativo(true).build();
        Categoria categoria = Categoria.builder().id(10L).nome("Hardware").setor(setor).build();
        CategoriaRequestDTO dto = new CategoriaRequestDTO("Infraestructura", 99L);

        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(setorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoriaService.atualizar(10L, dto));
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void listarCategoriasPaginadoSinFiltros() {
        Setor setor = Setor.builder().id(1L).nome("TI").sigla("TI").ativo(true).build();
        Categoria categoria = Categoria.builder().id(10L).nome("Hardware").setor(setor).build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nome"));
        Page<Categoria> page = new PageImpl<>(List.of(categoria), pageable, 1);

        when(categoriaRepository.findAll(ArgumentMatchers.<Specification<Categoria>>any(), eq(pageable)))
                .thenReturn(page);

        Page<CategoriaResponseDTO> response = categoriaService.listar(null, null, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals("Hardware", response.getContent().get(0).nome());
        verify(categoriaRepository).findAll(ArgumentMatchers.<Specification<Categoria>>any(), eq(pageable));
    }

    @Test
    void listarCategoriasPaginadoPorNome() {
        Setor setor = Setor.builder().id(1L).nome("TI").sigla("TI").ativo(true).build();
        Categoria categoria = Categoria.builder().id(10L).nome("Hardware").setor(setor).build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nome"));
        Page<Categoria> page = new PageImpl<>(List.of(categoria), pageable, 1);

        when(categoriaRepository.findAll(ArgumentMatchers.<Specification<Categoria>>any(), eq(pageable)))
                .thenReturn(page);

        Page<CategoriaResponseDTO> response = categoriaService.listar("hard", null, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals("Hardware", response.getContent().get(0).nome());
        verify(categoriaRepository).findAll(ArgumentMatchers.<Specification<Categoria>>any(), eq(pageable));
    }

    @Test
    void listarCategoriasPaginadoPorSetorId() {
        Setor setor = Setor.builder().id(1L).nome("TI").sigla("TI").ativo(true).build();
        Categoria categoria = Categoria.builder().id(10L).nome("Hardware").setor(setor).build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nome"));
        Page<Categoria> page = new PageImpl<>(List.of(categoria), pageable, 1);

        when(categoriaRepository.findAll(ArgumentMatchers.<Specification<Categoria>>any(), eq(pageable)))
                .thenReturn(page);

        Page<CategoriaResponseDTO> response = categoriaService.listar(null, 1L, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals(1L, response.getContent().get(0).setorId());
        verify(categoriaRepository).findAll(ArgumentMatchers.<Specification<Categoria>>any(), eq(pageable));
    }

    @Test
    void listarCategoriasPaginadoCombinado() {
        Setor setor = Setor.builder().id(1L).nome("TI").sigla("TI").ativo(true).build();
        Categoria categoria = Categoria.builder().id(10L).nome("Hardware").setor(setor).build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nome"));
        Page<Categoria> page = new PageImpl<>(List.of(categoria), pageable, 1);

        when(categoriaRepository.findAll(ArgumentMatchers.<Specification<Categoria>>any(), eq(pageable)))
                .thenReturn(page);

        Page<CategoriaResponseDTO> response = categoriaService.listar("hard", 1L, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals("Hardware", response.getContent().get(0).nome());
        assertEquals(1L, response.getContent().get(0).setorId());
        verify(categoriaRepository).findAll(ArgumentMatchers.<Specification<Categoria>>any(), eq(pageable));
    }
}
