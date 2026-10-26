package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.InteracaoRequestDTO;
import com.davidparragamendoza.chamados.dto.InteracaoResponseDTO;
import com.davidparragamendoza.chamados.exception.BusinessValidationException;
import com.davidparragamendoza.chamados.exception.ResourceNotFoundException;
import com.davidparragamendoza.chamados.model.Categoria;
import com.davidparragamendoza.chamados.model.Chamado;
import com.davidparragamendoza.chamados.model.Interacao;
import com.davidparragamendoza.chamados.model.PrioridadeChamado;
import com.davidparragamendoza.chamados.model.Setor;
import com.davidparragamendoza.chamados.model.StatusChamado;
import com.davidparragamendoza.chamados.model.TipoUsuario;
import com.davidparragamendoza.chamados.model.Usuario;
import com.davidparragamendoza.chamados.repository.ChamadoRepository;
import com.davidparragamendoza.chamados.repository.InteracaoRepository;
import com.davidparragamendoza.chamados.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteracaoServiceTest {

    @Mock
    private InteracaoRepository interacaoRepository;

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private InteracaoServiceImpl interacaoService;

    private Chamado chamado;
    private Usuario autor;
    private Usuario tecnico;
    private Usuario terceiro;

    @BeforeEach
    void setUp() {
        Setor setor = Setor.builder()
                .id(1L)
                .nome("Tecnología de la Información")
                .sigla("TI")
                .ativo(true)
                .build();

        Categoria categoria = Categoria.builder()
                .id(10L)
                .nome("Hardware")
                .setor(setor)
                .build();

        autor = Usuario.builder()
                .id(1L)
                .nome("Ana Souza")
                .email("ana.souza@ifrn.edu.br")
                .matricula("2026001")
                .tipo(TipoUsuario.SOLICITANTE)
                .setor(setor)
                .build();

        tecnico = Usuario.builder()
                .id(2L)
                .nome("Carlos Lima")
                .email("carlos.lima@ifrn.edu.br")
                .matricula("2026002")
                .tipo(TipoUsuario.TECNICO)
                .setor(setor)
                .build();

        terceiro = Usuario.builder()
                .id(3L)
                .nome("Beatriz Alves")
                .email("beatriz.alves@ifrn.edu.br")
                .matricula("2026003")
                .tipo(TipoUsuario.SOLICITANTE)
                .setor(setor)
                .build();

        chamado = Chamado.builder()
                .id(100L)
                .titulo("Computadora no enciende")
                .descricao("El equipo no responde al presionar el botón de encendido")
                .local("Laboratorio 1")
                .prioridade(PrioridadeChamado.ALTA)
                .status(StatusChamado.ABERTO)
                .dataAbertura(LocalDateTime.now().minusHours(1))
                .categoria(categoria)
                .solicitante(autor)
                .build();
    }

    @Test
    void crearInteracaoExitosamente() {
        InteracaoRequestDTO request = new InteracaoRequestDTO(
                "El equipo sigue sin responder después del reinicio", 1L);

        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(autor));
        when(interacaoRepository.save(any(Interacao.class))).thenAnswer(invocation -> {
            Interacao interacao = invocation.getArgument(0);
            interacao.setId(50L);
            return interacao;
        });

        LocalDateTime antes = LocalDateTime.now();
        InteracaoResponseDTO response = interacaoService.criar(100L, request);

        assertEquals(50L, response.id());
        assertEquals("El equipo sigue sin responder después del reinicio", response.texto());
        assertNotNull(response.dataHora());
        assertFalse(response.dataHora().isBefore(antes));
        assertEquals(1L, response.autorId());
        assertEquals("Ana Souza", response.autorNome());

        ArgumentCaptor<Interacao> captor = ArgumentCaptor.forClass(Interacao.class);
        verify(interacaoRepository).save(captor.capture());
        assertEquals(100L, captor.getValue().getChamado().getId());
        assertEquals(1L, captor.getValue().getAutor().getId());
    }

    @Test
    void rechazarCreacionConChamadoInexistente() {
        InteracaoRequestDTO request = new InteracaoRequestDTO(
                "Comentario sin chamado válido", 1L);

        when(chamadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> interacaoService.criar(99L, request));

        verifyNoInteractions(usuarioRepository);
        verify(interacaoRepository, never()).save(any(Interacao.class));
    }

    @Test
    void rechazarCreacionConAutorInexistente() {
        InteracaoRequestDTO request = new InteracaoRequestDTO(
                "Comentario sin autor válido", 99L);

        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> interacaoService.criar(100L, request));

        verify(interacaoRepository, never()).save(any(Interacao.class));
    }

    @Test
    void crearInteracaoPorTecnicoAsignadoExitosamente() {
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        chamado.setTecnico(tecnico);

        InteracaoRequestDTO request = new InteracaoRequestDTO(
                "Iniciando diagnóstico en el laboratorio", 2L);

        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tecnico));
        when(interacaoRepository.save(any(Interacao.class))).thenAnswer(invocation -> {
            Interacao interacao = invocation.getArgument(0);
            interacao.setId(51L);
            return interacao;
        });

        InteracaoResponseDTO response = interacaoService.criar(100L, request);

        assertEquals(51L, response.id());
        assertEquals("Iniciando diagnóstico en el laboratorio", response.texto());
        assertEquals(2L, response.autorId());
        assertEquals("Carlos Lima", response.autorNome());

        ArgumentCaptor<Interacao> captor = ArgumentCaptor.forClass(Interacao.class);
        verify(interacaoRepository).save(captor.capture());
        assertEquals(2L, captor.getValue().getAutor().getId());
    }

    @Test
    void rechazarCreacionEnChamadoResolvido() {
        chamado.setStatus(StatusChamado.RESOLVIDO);
        chamado.setTecnico(tecnico);

        InteracaoRequestDTO request = new InteracaoRequestDTO(
                "Comentario posterior al cierre", 1L);

        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(autor));

        assertThrows(BusinessValidationException.class,
                () -> interacaoService.criar(100L, request));

        verify(interacaoRepository, never()).save(any(Interacao.class));
    }

    @Test
    void rechazarCreacionEnChamadoCancelado() {
        chamado.setStatus(StatusChamado.CANCELADO);

        InteracaoRequestDTO request = new InteracaoRequestDTO(
                "Comentario en ticket cancelado", 1L);

        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(autor));

        assertThrows(BusinessValidationException.class,
                () -> interacaoService.criar(100L, request));

        verify(interacaoRepository, never()).save(any(Interacao.class));
    }

    @Test
    void rechazarCreacionPorUsuarioTercero() {
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        chamado.setTecnico(tecnico);

        InteracaoRequestDTO request = new InteracaoRequestDTO(
                "Comentario de alguien no involucrado", 3L);

        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(terceiro));

        assertThrows(BusinessValidationException.class,
                () -> interacaoService.criar(100L, request));

        verify(interacaoRepository, never()).save(any(Interacao.class));
    }

    @Test
    void listarInteracoesPorChamado() {
        Interacao primeira = Interacao.builder()
                .id(50L)
                .texto("Primera interacción")
                .dataHora(LocalDateTime.now().minusHours(2))
                .chamado(chamado)
                .autor(autor)
                .build();

        Interacao segunda = Interacao.builder()
                .id(51L)
                .texto("Segunda interacción")
                .dataHora(LocalDateTime.now().minusHours(1))
                .chamado(chamado)
                .autor(autor)
                .build();

        when(chamadoRepository.existsById(100L)).thenReturn(true);
        when(interacaoRepository.findByChamadoIdOrderByDataHoraAsc(100L))
                .thenReturn(List.of(primeira, segunda));

        List<InteracaoResponseDTO> response = interacaoService.listarPorChamado(100L);

        assertEquals(2, response.size());
        assertEquals(50L, response.get(0).id());
        assertEquals("Primera interacción", response.get(0).texto());
        assertEquals(51L, response.get(1).id());
        assertEquals("Segunda interacción", response.get(1).texto());
        assertEquals("Ana Souza", response.get(1).autorNome());
    }

    @Test
    void rechazarListadoDeInteraccionesDeChamadoInexistente() {
        when(chamadoRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> interacaoService.listarPorChamado(99L));

        verify(interacaoRepository, never()).findByChamadoIdOrderByDataHoraAsc(anyLong());
    }
}
