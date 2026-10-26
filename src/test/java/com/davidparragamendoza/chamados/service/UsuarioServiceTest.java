package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.UsuarioRequestDTO;
import com.davidparragamendoza.chamados.dto.UsuarioResponseDTO;
import com.davidparragamendoza.chamados.exception.BusinessConflictException;
import com.davidparragamendoza.chamados.exception.ResourceNotFoundException;
import com.davidparragamendoza.chamados.model.Setor;
import com.davidparragamendoza.chamados.model.TipoUsuario;
import com.davidparragamendoza.chamados.model.Usuario;
import com.davidparragamendoza.chamados.repository.SetorRepository;
import com.davidparragamendoza.chamados.repository.UsuarioRepository;
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
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SetorRepository setorRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Test
    void registrarUsuarioExitosamente() {
        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Ana Souza", "ana.souza@ifrn.edu.br", "2026001", TipoUsuario.SOLICITANTE, null);

        when(usuarioRepository.existsByEmail("ana.souza@ifrn.edu.br")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(1L);
            return usuario;
        });

        UsuarioResponseDTO response = usuarioService.salvar(request);

        assertEquals(1L, response.id());
        assertEquals("Ana Souza", response.nome());
        assertEquals("ana.souza@ifrn.edu.br", response.email());
        assertEquals("2026001", response.matricula());
        assertEquals(TipoUsuario.SOLICITANTE, response.tipo());
        assertNull(response.setorId());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("Ana Souza", captor.getValue().getNome());
        assertEquals(TipoUsuario.SOLICITANTE, captor.getValue().getTipo());
        assertNull(captor.getValue().getSetor());
        verifyNoInteractions(setorRepository);
    }

    @Test
    void registrarUsuarioConSetorExitosamente() {
        Setor setor = Setor.builder().id(1L).nome("Tecnología de la Información").sigla("TI").ativo(true).build();
        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Carlos Lima", "carlos.lima@ifrn.edu.br", "2026002", TipoUsuario.TECNICO, 1L);

        when(usuarioRepository.existsByEmail("carlos.lima@ifrn.edu.br")).thenReturn(false);
        when(setorRepository.findById(1L)).thenReturn(Optional.of(setor));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(2L);
            return usuario;
        });

        UsuarioResponseDTO response = usuarioService.salvar(request);

        assertEquals(2L, response.id());
        assertEquals(1L, response.setorId());
        assertEquals(TipoUsuario.TECNICO, response.tipo());
    }

    @Test
    void rechazarRegistroPorEmailDuplicado() {
        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Ana Souza", "ana.souza@ifrn.edu.br", "2026001", TipoUsuario.SOLICITANTE, null);

        when(usuarioRepository.existsByEmail("ana.souza@ifrn.edu.br")).thenReturn(true);

        BusinessConflictException exception = assertThrows(
                BusinessConflictException.class,
                () -> usuarioService.salvar(request));

        assertTrue(exception.getMessage().contains("ana.souza@ifrn.edu.br"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(setorRepository);
    }

    @Test
    void rechazarRegistroConSetorInexistente() {
        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Carlos Lima", "carlos.lima@ifrn.edu.br", "2026002", TipoUsuario.TECNICO, 99L);

        when(usuarioRepository.existsByEmail("carlos.lima@ifrn.edu.br")).thenReturn(false);
        when(setorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.salvar(request));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void buscarUsuarioPorIdExistente() {
        Setor setor = Setor.builder().id(1L).nome("Tecnología de la Información").sigla("TI").ativo(true).build();
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Ana Souza")
                .email("ana.souza@ifrn.edu.br")
                .matricula("2026001")
                .tipo(TipoUsuario.SOLICITANTE)
                .setor(setor)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UsuarioResponseDTO response = usuarioService.buscarPorId(1L);

        assertEquals(1L, response.id());
        assertEquals("Ana Souza", response.nome());
        assertEquals("ana.souza@ifrn.edu.br", response.email());
        assertEquals("2026001", response.matricula());
        assertEquals(TipoUsuario.SOLICITANTE, response.tipo());
        assertEquals(1L, response.setorId());
    }

    @Test
    void rechazarBusquedaDeUsuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.buscarPorId(99L));
    }

    @Test
    void listarUsuariosConFiltrosPaginados() {
        Setor setor = Setor.builder().id(1L).nome("Tecnología de la Información").sigla("TI").ativo(true).build();
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Ana Souza")
                .email("ana.souza@ifrn.edu.br")
                .matricula("2026001")
                .tipo(TipoUsuario.SOLICITANTE)
                .setor(setor)
                .build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nome"));
        Page<Usuario> page = new PageImpl<>(List.of(usuario), pageable, 1);

        when(usuarioRepository.findAll(
                ArgumentMatchers.<Specification<Usuario>>any(), eq(pageable))).thenReturn(page);

        Page<UsuarioResponseDTO> response = usuarioService.listar("ana", TipoUsuario.SOLICITANTE, 1L, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getContent().get(0).id());
        assertEquals("Ana Souza", response.getContent().get(0).nome());
        assertEquals(1L, response.getContent().get(0).setorId());
        verify(usuarioRepository).findAll(ArgumentMatchers.<Specification<Usuario>>any(), eq(pageable));
    }

    @Test
    void atualizarUsuarioExitosamente() {
        Setor setor = Setor.builder().id(1L).nome("TI").sigla("TI").ativo(true).build();
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Ana Souza")
                .email("ana.souza@ifrn.edu.br")
                .matricula("2026001")
                .tipo(TipoUsuario.SOLICITANTE)
                .build();
        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Ana Paula Souza", "anapaula@ifrn.edu.br", "2026001-A", TipoUsuario.TECNICO, 1L);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdNot("anapaula@ifrn.edu.br", 1L)).thenReturn(false);
        when(setorRepository.findById(1L)).thenReturn(Optional.of(setor));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponseDTO response = usuarioService.atualizar(1L, request);

        assertEquals("Ana Paula Souza", response.nome());
        assertEquals("anapaula@ifrn.edu.br", response.email());
        assertEquals("2026001-A", response.matricula());
        assertEquals(TipoUsuario.TECNICO, response.tipo());
        assertEquals(1L, response.setorId());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void atualizarUsuarioInexistenteLanzaNotFound() {
        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Ana Paula", "anapaula@ifrn.edu.br", "2026001", TipoUsuario.SOLICITANTE, null);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.atualizar(99L, request));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void atualizarUsuarioEmailPertenceAOtroLanzaConflict() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Ana Souza")
                .email("ana@ifrn.edu.br")
                .matricula("2026001")
                .tipo(TipoUsuario.SOLICITANTE)
                .build();
        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Ana Souza", "carlos@ifrn.edu.br", "2026001", TipoUsuario.SOLICITANTE, null);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdNot("carlos@ifrn.edu.br", 1L)).thenReturn(true);

        assertThrows(BusinessConflictException.class, () -> usuarioService.atualizar(1L, request));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void atualizarUsuarioSetorInexistenteLanzaNotFound() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Ana Souza")
                .email("ana@ifrn.edu.br")
                .matricula("2026001")
                .tipo(TipoUsuario.SOLICITANTE)
                .build();
        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Ana Souza", "ana@ifrn.edu.br", "2026001", TipoUsuario.TECNICO, 99L);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdNot("ana@ifrn.edu.br", 1L)).thenReturn(false);
        when(setorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.atualizar(1L, request));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
