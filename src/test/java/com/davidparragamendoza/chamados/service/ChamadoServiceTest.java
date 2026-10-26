package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.AssumirChamadoDTO;
import com.davidparragamendoza.chamados.dto.CancelarChamadoDTO;
import com.davidparragamendoza.chamados.dto.ChamadoCreateDTO;
import com.davidparragamendoza.chamados.dto.ChamadoDetalheResponseDTO;
import com.davidparragamendoza.chamados.dto.ChamadoResponseDTO;
import com.davidparragamendoza.chamados.dto.ResolverChamadoDTO;
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
import com.davidparragamendoza.chamados.repository.CategoriaRepository;
import com.davidparragamendoza.chamados.repository.ChamadoRepository;
import com.davidparragamendoza.chamados.repository.InteracaoRepository;
import com.davidparragamendoza.chamados.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private InteracaoRepository interacaoRepository;

    @InjectMocks
    private ChamadoServiceImpl chamadoService;

    private Setor setor;
    private Categoria categoria;
    private Usuario solicitante;
    private Usuario tecnico;
    private Usuario naoTecnico;
    private Usuario tecnicoOutroSetor;

    @BeforeEach
    void setUp() {
        setor = Setor.builder()
                .id(1L)
                .nome("Tecnología de la Información")
                .sigla("TI")
                .ativo(true)
                .build();

        categoria = Categoria.builder()
                .id(10L)
                .nome("Hardware")
                .setor(setor)
                .build();

        solicitante = Usuario.builder()
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

        naoTecnico = Usuario.builder()
                .id(3L)
                .nome("Beatriz Alves")
                .email("beatriz.alves@ifrn.edu.br")
                .matricula("2026003")
                .tipo(TipoUsuario.SOLICITANTE)
                .setor(setor)
                .build();

        Setor setorOutro = Setor.builder()
                .id(2L)
                .nome("Mantenimiento")
                .sigla("MAN")
                .ativo(true)
                .build();

        tecnicoOutroSetor = Usuario.builder()
                .id(4L)
                .nome("Marcos Dias")
                .email("marcos.dias@ifrn.edu.br")
                .matricula("2026004")
                .tipo(TipoUsuario.TECNICO)
                .setor(setorOutro)
                .build();
    }

    @Test
    void abrirChamadoExitosamente() {
        ChamadoCreateDTO request = new ChamadoCreateDTO(
                "Computadora no enciende",
                "El equipo no responde al presionar el botón de encendido",
                "Laboratorio 1",
                PrioridadeChamado.ALTA,
                10L,
                1L);

        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(solicitante));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocation -> {
            Chamado chamado = invocation.getArgument(0);
            chamado.setId(100L);
            return chamado;
        });

        LocalDateTime antes = LocalDateTime.now();
        ChamadoResponseDTO response = chamadoService.abrir(request);

        assertEquals(100L, response.id());
        assertEquals("Computadora no enciende", response.titulo());
        assertEquals("El equipo no responde al presionar el botón de encendido", response.descricao());
        assertEquals("Laboratorio 1", response.local());
        assertEquals(PrioridadeChamado.ALTA, response.prioridade());
        assertEquals(StatusChamado.ABERTO, response.status());
        assertNotNull(response.dataAbertura());
        assertFalse(response.dataAbertura().isBefore(antes));
        assertNull(response.dataFechamento());
        assertNull(response.solucao());
        assertEquals(10L, response.categoriaId());
        assertEquals(1L, response.solicitanteId());
        assertNull(response.tecnicoId());

        ArgumentCaptor<Chamado> captor = ArgumentCaptor.forClass(Chamado.class);
        verify(chamadoRepository).save(captor.capture());
        assertEquals(StatusChamado.ABERTO, captor.getValue().getStatus());
        assertNull(captor.getValue().getTecnico());
    }

    @Test
    void abrirChamadoConPrioridadePorDefecto() {
        ChamadoCreateDTO request = new ChamadoCreateDTO(
                "Proyector no enciende",
                "Foco quemado",
                "Aula 10",
                null,
                10L,
                1L);

        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(solicitante));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocation -> {
            Chamado chamado = invocation.getArgument(0);
            chamado.setId(101L);
            return chamado;
        });

        ChamadoResponseDTO response = chamadoService.abrir(request);

        assertEquals(PrioridadeChamado.MEDIA, response.prioridade());
        assertEquals(StatusChamado.ABERTO, response.status());

        ArgumentCaptor<Chamado> captor = ArgumentCaptor.forClass(Chamado.class);
        verify(chamadoRepository).save(captor.capture());
        assertEquals(PrioridadeChamado.MEDIA, captor.getValue().getPrioridade());
    }

    @Test
    void rechazarAperturaConUsuarioNoSolicitante() {
        ChamadoCreateDTO request = new ChamadoCreateDTO(
                "Problema de red",
                "Sin conectividad",
                "Sala de servidores",
                PrioridadeChamado.ALTA,
                10L,
                2L);

        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tecnico));

        assertThrows(BusinessValidationException.class, () -> chamadoService.abrir(request));

        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void rechazarAperturaConCategoriaInexistente() {
        ChamadoCreateDTO request = new ChamadoCreateDTO(
                "Computadora no enciende",
                "El equipo no responde",
                "Laboratorio 1",
                PrioridadeChamado.ALTA,
                99L,
                1L);

        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> chamadoService.abrir(request));

        verify(chamadoRepository, never()).save(any(Chamado.class));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void rechazarAperturaConSolicitanteInexistente() {
        ChamadoCreateDTO request = new ChamadoCreateDTO(
                "Computadora no enciende",
                "El equipo no responde",
                "Laboratorio 1",
                PrioridadeChamado.ALTA,
                10L,
                99L);

        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> chamadoService.abrir(request));

        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void asumirChamadoExitosamente() {
        Chamado chamado = chamadoAberto();
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tecnico));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChamadoResponseDTO response = chamadoService.assumir(100L, new AssumirChamadoDTO(2L));

        assertEquals(100L, response.id());
        assertEquals(StatusChamado.EM_ATENDIMENTO, response.status());
        assertEquals(2L, response.tecnicoId());

        ArgumentCaptor<Chamado> captor = ArgumentCaptor.forClass(Chamado.class);
        verify(chamadoRepository).save(captor.capture());
        assertEquals(StatusChamado.EM_ATENDIMENTO, captor.getValue().getStatus());
        assertEquals(2L, captor.getValue().getTecnico().getId());
    }

    @Test
    void rechazarAsumirChamadoQueNoEstaAbierto() {
        Chamado chamado = chamadoAberto();
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));

        assertThrows(BusinessValidationException.class,
                () -> chamadoService.assumir(100L, new AssumirChamadoDTO(2L)));

        verifyNoInteractions(usuarioRepository);
        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void rechazarAsumirConUsuarioQueNoEsTecnico() {
        Chamado chamado = chamadoAberto();
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(naoTecnico));

        assertThrows(BusinessValidationException.class,
                () -> chamadoService.assumir(100L, new AssumirChamadoDTO(3L)));

        assertEquals(StatusChamado.ABERTO, chamado.getStatus());
        assertNull(chamado.getTecnico());
        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void rechazarAsumirConTecnicoInexistente() {
        Chamado chamado = chamadoAberto();
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> chamadoService.assumir(100L, new AssumirChamadoDTO(99L)));

        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void rechazarAsumirConTecnicoDeOtroSector() {
        Chamado chamado = chamadoAberto();
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(tecnicoOutroSetor));

        assertThrows(BusinessValidationException.class,
                () -> chamadoService.assumir(100L, new AssumirChamadoDTO(4L)));

        assertEquals(StatusChamado.ABERTO, chamado.getStatus());
        assertNull(chamado.getTecnico());
        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void resolverChamadoExitosamente() {
        Chamado chamado = chamadoEnAtendimento();
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime antes = LocalDateTime.now();
        ChamadoResponseDTO response = chamadoService.resolver(
                100L, new ResolverChamadoDTO("Se reemplazó la fuente de poder", 2L));

        assertEquals(StatusChamado.RESOLVIDO, response.status());
        assertEquals("Se reemplazó la fuente de poder", response.solucao());
        assertNotNull(response.dataFechamento());
        assertFalse(response.dataFechamento().isBefore(antes));
        assertEquals(2L, response.tecnicoId());
        assertEquals(10L, response.categoriaId());
        assertEquals(1L, response.solicitanteId());

        ArgumentCaptor<Chamado> captor = ArgumentCaptor.forClass(Chamado.class);
        verify(chamadoRepository).save(captor.capture());
        assertEquals(StatusChamado.RESOLVIDO, captor.getValue().getStatus());
        assertEquals("Se reemplazó la fuente de poder", captor.getValue().getSolucao());
        assertNotNull(captor.getValue().getDataFechamento());
    }

    @Test
    void rechazarResolucionConSolucionEnBlanco() {
        Chamado chamado = chamadoConEstado(StatusChamado.EM_ATENDIMENTO);
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));

        assertThrows(BusinessValidationException.class,
                () -> chamadoService.resolver(100L, new ResolverChamadoDTO("   ", 2L)));

        assertEquals(StatusChamado.EM_ATENDIMENTO, chamado.getStatus());
        assertNull(chamado.getDataFechamento());
        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void rechazarResolucionPorTecnicoNoAsignado() {
        Chamado chamado = chamadoEnAtendimento();
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));

        assertThrows(BusinessValidationException.class,
                () -> chamadoService.resolver(
                        100L, new ResolverChamadoDTO("Solución aplicada", 3L)));

        assertEquals(StatusChamado.EM_ATENDIMENTO, chamado.getStatus());
        assertNull(chamado.getDataFechamento());
        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void rechazarResolucionDeChamadoEnEstadoInvalido() {
        Chamado chamado = chamadoAberto();
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));

        assertThrows(BusinessValidationException.class,
                () -> chamadoService.resolver(100L, new ResolverChamadoDTO("Solución aplicada", 2L)));

        assertEquals(StatusChamado.ABERTO, chamado.getStatus());
        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void rechazarResolucionDeChamadoInexistente() {
        when(chamadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> chamadoService.resolver(99L, new ResolverChamadoDTO("Solución aplicada", 2L)));

        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void cancelarChamadoExitosamente() {
        Chamado chamado = chamadoAberto();
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChamadoResponseDTO response = chamadoService.cancelar(
                100L, new CancelarChamadoDTO(1L));

        assertEquals(100L, response.id());
        assertEquals(StatusChamado.CANCELADO, response.status());
        assertNull(response.dataFechamento());

        ArgumentCaptor<Chamado> captor = ArgumentCaptor.forClass(Chamado.class);
        verify(chamadoRepository).save(captor.capture());
        assertEquals(StatusChamado.CANCELADO, captor.getValue().getStatus());
    }

    @Test
    void rechazarCancelacionPorTecnicoAsignado() {
        Chamado chamado = chamadoAberto();
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));

        assertThrows(BusinessValidationException.class,
                () -> chamadoService.cancelar(100L, new CancelarChamadoDTO(2L)));

        assertEquals(StatusChamado.ABERTO, chamado.getStatus());
        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void rechazarCancelacionDeChamadoEnAtendimento() {
        Chamado chamado = chamadoEnAtendimento();
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));

        assertThrows(BusinessValidationException.class,
                () -> chamadoService.cancelar(100L, new CancelarChamadoDTO(1L)));

        assertEquals(StatusChamado.EM_ATENDIMENTO, chamado.getStatus());
        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void rechazarCancelacionPorUsuarioNoInvolucrado() {
        Chamado chamado = chamadoAberto();
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));

        assertThrows(BusinessValidationException.class,
                () -> chamadoService.cancelar(100L, new CancelarChamadoDTO(3L)));

        assertEquals(StatusChamado.ABERTO, chamado.getStatus());
        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void rechazarCancelacionDeChamadoResuelto() {
        Chamado chamado = chamadoConEstado(StatusChamado.RESOLVIDO);
        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));

        assertThrows(BusinessValidationException.class,
                () -> chamadoService.cancelar(100L, new CancelarChamadoDTO(1L)));

        assertEquals(StatusChamado.RESOLVIDO, chamado.getStatus());
        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void rechazarCancelacionDeChamadoInexistente() {
        when(chamadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> chamadoService.cancelar(99L, new CancelarChamadoDTO(1L)));

        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void listarChamadosConFiltrosPaginados() {
        Chamado chamado = chamadoAberto();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dataAbertura"));
        Page<Chamado> page = new PageImpl<>(List.of(chamado), pageable, 1);

        when(chamadoRepository.findAll(
                ArgumentMatchers.<Specification<Chamado>>any(), eq(pageable))).thenReturn(page);

        Page<ChamadoResponseDTO> response = chamadoService.listar(
                StatusChamado.ABERTO,
                PrioridadeChamado.ALTA,
                1L,
                10L,
                1L,
                2L,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals(100L, response.getContent().get(0).id());
        assertEquals(StatusChamado.ABERTO, response.getContent().get(0).status());
        verify(chamadoRepository).findAll(
                ArgumentMatchers.<Specification<Chamado>>any(), eq(pageable));
    }

    @Test
    void listarChamadosPorUsuarioPaginados() {
        Chamado chamado = chamadoEnAtendimento();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dataAbertura"));
        Page<Chamado> page = new PageImpl<>(List.of(chamado), pageable, 1);

        when(chamadoRepository.findAll(
                ArgumentMatchers.<Specification<Chamado>>any(), eq(pageable))).thenReturn(page);

        Page<ChamadoResponseDTO> response = chamadoService.listarPorUsuario(
                1L, StatusChamado.EM_ATENDIMENTO, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals(100L, response.getContent().get(0).id());
        assertEquals(1L, response.getContent().get(0).solicitanteId());
        assertEquals(2L, response.getContent().get(0).tecnicoId());
        verify(chamadoRepository).findAll(
                ArgumentMatchers.<Specification<Chamado>>any(), eq(pageable));
    }

    @Test
    void buscarPorIdConExitoDevuelveDetalleConInteracciones() {
        Chamado chamado = chamadoAberto();
        Interacao interacao1 = Interacao.builder()
                .id(1L)
                .chamado(chamado)
                .autor(solicitante)
                .texto("Primera interacción")
                .dataHora(LocalDateTime.now().minusMinutes(30))
                .build();
        Interacao interacao2 = Interacao.builder()
                .id(2L)
                .chamado(chamado)
                .autor(tecnico)
                .texto("Segunda interacción")
                .dataHora(LocalDateTime.now().minusMinutes(10))
                .build();

        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(interacaoRepository.findByChamadoIdOrderByDataHoraAsc(100L))
                .thenReturn(List.of(interacao1, interacao2));

        ChamadoDetalheResponseDTO response = chamadoService.buscarPorId(100L);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Computadora no enciende", response.titulo());
        assertEquals(StatusChamado.ABERTO, response.status());
        assertEquals(2, response.interacoes().size());
        assertEquals("Primera interacción", response.interacoes().get(0).texto());
        assertEquals("Segunda interacción", response.interacoes().get(1).texto());
        verify(chamadoRepository).findById(100L);
        verify(interacaoRepository).findByChamadoIdOrderByDataHoraAsc(100L);
    }

    @Test
    void buscarPorIdInexistenteLanzaResourceNotFoundException() {
        when(chamadoRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> chamadoService.buscarPorId(999L));

        assertTrue(ex.getMessage().contains("999"));
        verify(chamadoRepository).findById(999L);
        verifyNoInteractions(interacaoRepository);
    }

    private Chamado chamadoAberto() {
        return Chamado.builder()
                .id(100L)
                .titulo("Computadora no enciende")
                .descricao("El equipo no responde al presionar el botón de encendido")
                .local("Laboratorio 1")
                .prioridade(PrioridadeChamado.ALTA)
                .status(StatusChamado.ABERTO)
                .dataAbertura(LocalDateTime.now().minusHours(1))
                .categoria(categoria)
                .solicitante(solicitante)
                .build();
    }

    private Chamado chamadoConEstado(StatusChamado status) {
        return Chamado.builder()
                .id(100L)
                .titulo("Computadora no enciende")
                .descricao("El equipo no responde al presionar el botón de encendido")
                .local("Laboratorio 1")
                .prioridade(PrioridadeChamado.ALTA)
                .status(status)
                .dataAbertura(LocalDateTime.now().minusHours(1))
                .categoria(categoria)
                .solicitante(solicitante)
                .tecnico(tecnico)
                .build();
    }

    private Chamado chamadoEnAtendimento() {
        return chamadoConEstado(StatusChamado.EM_ATENDIMENTO);
    }
}
