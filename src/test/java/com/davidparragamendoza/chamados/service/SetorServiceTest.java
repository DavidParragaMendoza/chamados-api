package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.SetorRequestDTO;
import com.davidparragamendoza.chamados.dto.SetorResponseDTO;
import com.davidparragamendoza.chamados.exception.BusinessConflictException;
import com.davidparragamendoza.chamados.exception.ResourceNotFoundException;
import com.davidparragamendoza.chamados.model.Setor;
import com.davidparragamendoza.chamados.repository.SetorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetorServiceTest {

    @Mock
    private SetorRepository setorRepository;

    @InjectMocks
    private SetorService setorService;

    @Test
    void crearSetorExitosamente() {
        SetorRequestDTO request = new SetorRequestDTO("Tecnología de la Información", "TI");

        when(setorRepository.existsBySiglaIgnoreCase("TI")).thenReturn(false);
        when(setorRepository.save(any(Setor.class))).thenAnswer(invocation -> {
            Setor setor = invocation.getArgument(0);
            setor.setId(1L);
            return setor;
        });

        SetorResponseDTO response = setorService.salvar(request);

        assertEquals(1L, response.id());
        assertEquals("Tecnología de la Información", response.nome());
        assertEquals("TI", response.sigla());
        assertTrue(response.ativo());

        ArgumentCaptor<Setor> captor = ArgumentCaptor.forClass(Setor.class);
        verify(setorRepository).save(captor.capture());
        assertEquals("Tecnología de la Información", captor.getValue().getNome());
        assertEquals("TI", captor.getValue().getSigla());
        assertTrue(captor.getValue().getAtivo());
    }

    @Test
    void rechazarCreacionPorSiglaDuplicada() {
        SetorRequestDTO request = new SetorRequestDTO("Tecnología de la Información", "ti");

        when(setorRepository.existsBySiglaIgnoreCase("ti")).thenReturn(true);

        BusinessConflictException exception = assertThrows(
                BusinessConflictException.class,
                () -> setorService.salvar(request));

        assertTrue(exception.getMessage().contains("ti"));
        verify(setorRepository, never()).save(any(Setor.class));
    }

    @Test
    void eliminarSetorExitosamente() {
        when(setorRepository.existsById(1L)).thenReturn(true);
        when(setorRepository.existsCategoriaBySetorId(1L)).thenReturn(false);
        when(setorRepository.existsUsuarioBySetorId(1L)).thenReturn(false);

        setorService.deletar(1L);

        verify(setorRepository).deleteById(1L);
    }

    @Test
    void bloquearEliminacionPorCategoriasAsociadas() {
        when(setorRepository.existsById(1L)).thenReturn(true);
        when(setorRepository.existsCategoriaBySetorId(1L)).thenReturn(true);

        assertThrows(BusinessConflictException.class, () -> setorService.deletar(1L));

        verify(setorRepository, never()).deleteById(anyLong());
    }

    @Test
    void bloquearEliminacionPorUsuariosAsociados() {
        when(setorRepository.existsById(1L)).thenReturn(true);
        when(setorRepository.existsCategoriaBySetorId(1L)).thenReturn(false);
        when(setorRepository.existsUsuarioBySetorId(1L)).thenReturn(true);

        assertThrows(BusinessConflictException.class, () -> setorService.deletar(1L));

        verify(setorRepository, never()).deleteById(anyLong());
    }

    @Test
    void rechazarEliminacionDeSetorInexistente() {
        when(setorRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> setorService.deletar(99L));

        verify(setorRepository, never()).existsCategoriaBySetorId(anyLong());
        verify(setorRepository, never()).existsUsuarioBySetorId(anyLong());
        verify(setorRepository, never()).deleteById(anyLong());
    }

    @Test
    void listarSetoresPaginadosSinFiltro() {
        Setor setor = Setor.builder()
                .id(1L)
                .nome("Tecnología de la Información")
                .sigla("TI")
                .ativo(true)
                .build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nome"));
        Page<Setor> page = new PageImpl<>(List.of(setor), pageable, 1);

        when(setorRepository.findAll(pageable)).thenReturn(page);

        Page<SetorResponseDTO> response = setorService.listarTodos(null, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getContent().get(0).id());
        assertEquals("TI", response.getContent().get(0).sigla());
        verify(setorRepository).findAll(pageable);
        verify(setorRepository, never())
                .findByNomeContainingIgnoreCase(any(String.class), any(Pageable.class));
    }

    @Test
    void listarSetoresFiltrandoPorNome() {
        Setor setor = Setor.builder()
                .id(1L)
                .nome("Tecnología de la Información")
                .sigla("TI")
                .ativo(true)
                .build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nome"));
        Page<Setor> page = new PageImpl<>(List.of(setor), pageable, 1);

        when(setorRepository.findByNomeContainingIgnoreCase("tec", pageable)).thenReturn(page);

        Page<SetorResponseDTO> response = setorService.listarTodos("tec", pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals("Tecnología de la Información", response.getContent().get(0).nome());
        verify(setorRepository).findByNomeContainingIgnoreCase("tec", pageable);
        verify(setorRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void buscarPorIdExitoso() {
        Setor setor = Setor.builder().id(1L).nome("TI").sigla("TI").ativo(true).build();
        when(setorRepository.findById(1L)).thenReturn(Optional.of(setor));

        SetorResponseDTO response = setorService.buscarPorId(1L);

        assertEquals(1L, response.id());
        assertEquals("TI", response.nome());
        verify(setorRepository).findById(1L);
    }

    @Test
    void buscarPorIdInexistenteLanzaNotFound() {
        when(setorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> setorService.buscarPorId(99L));
        verify(setorRepository).findById(99L);
    }

    @Test
    void atualizarSetorExitosamente() {
        Setor setor = Setor.builder().id(1L).nome("Tecnología").sigla("TEC").ativo(true).build();
        SetorRequestDTO dto = new SetorRequestDTO("Tecnología Moderna", "TM");

        when(setorRepository.findById(1L)).thenReturn(Optional.of(setor));
        when(setorRepository.existsBySiglaIgnoreCaseAndIdNot("TM", 1L)).thenReturn(false);
        when(setorRepository.save(any(Setor.class))).thenAnswer(i -> i.getArgument(0));

        SetorResponseDTO response = setorService.atualizar(1L, dto);

        assertEquals("Tecnología Moderna", response.nome());
        assertEquals("TM", response.sigla());
        verify(setorRepository).save(setor);
    }

    @Test
    void atualizarSetorInexistenteLanzaNotFound() {
        SetorRequestDTO dto = new SetorRequestDTO("Tecnología Moderna", "TM");
        when(setorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> setorService.atualizar(99L, dto));
        verify(setorRepository, never()).save(any(Setor.class));
    }

    @Test
    void atualizarSetorSiglaDuplicadaLanzaConflict() {
        Setor setor = Setor.builder().id(1L).nome("Tecnología").sigla("TEC").ativo(true).build();
        SetorRequestDTO dto = new SetorRequestDTO("Tecnología Moderna", "RH");

        when(setorRepository.findById(1L)).thenReturn(Optional.of(setor));
        when(setorRepository.existsBySiglaIgnoreCaseAndIdNot("RH", 1L)).thenReturn(true);

        assertThrows(BusinessConflictException.class, () -> setorService.atualizar(1L, dto));
        verify(setorRepository, never()).save(any(Setor.class));
    }
}
