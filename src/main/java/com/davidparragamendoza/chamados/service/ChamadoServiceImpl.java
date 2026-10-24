package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.AssumirChamadoDTO;
import com.davidparragamendoza.chamados.dto.CancelarChamadoDTO;
import com.davidparragamendoza.chamados.dto.ChamadoCreateDTO;
import com.davidparragamendoza.chamados.dto.ChamadoDetalheResponseDTO;
import com.davidparragamendoza.chamados.dto.ChamadoResponseDTO;
import com.davidparragamendoza.chamados.dto.InteracaoResponseDTO;
import com.davidparragamendoza.chamados.dto.ResolverChamadoDTO;
import com.davidparragamendoza.chamados.exception.BusinessValidationException;
import com.davidparragamendoza.chamados.exception.ResourceNotFoundException;
import com.davidparragamendoza.chamados.model.Categoria;
import com.davidparragamendoza.chamados.model.Chamado;
import com.davidparragamendoza.chamados.model.PrioridadeChamado;
import com.davidparragamendoza.chamados.model.StatusChamado;
import com.davidparragamendoza.chamados.model.TipoUsuario;
import com.davidparragamendoza.chamados.model.Usuario;
import com.davidparragamendoza.chamados.repository.CategoriaRepository;
import com.davidparragamendoza.chamados.repository.ChamadoRepository;
import com.davidparragamendoza.chamados.repository.InteracaoRepository;
import com.davidparragamendoza.chamados.repository.UsuarioRepository;
import com.davidparragamendoza.chamados.repository.specification.ChamadoSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ChamadoServiceImpl implements ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final InteracaoRepository interacaoRepository;

    public ChamadoServiceImpl(ChamadoRepository chamadoRepository,
                              CategoriaRepository categoriaRepository,
                              UsuarioRepository usuarioRepository,
                              InteracaoRepository interacaoRepository) {
        this.chamadoRepository = chamadoRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.interacaoRepository = interacaoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ChamadoDetalheResponseDTO buscarPorId(Long id) {
        Chamado chamado = buscarChamado(id);
        List<InteracaoResponseDTO> interacoes = interacaoRepository
                .findByChamadoIdOrderByDataHoraAsc(id)
                .stream()
                .map(InteracaoResponseDTO::fromEntity)
                .toList();
        return ChamadoDetalheResponseDTO.of(chamado, interacoes);
    }

    @Override
    @Transactional
    public ChamadoResponseDTO abrir(ChamadoCreateDTO dto) {
        Categoria categoria = categoriaRepository.findById(dto.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoría no encontrada con el ID: " + dto.categoriaId()));

        Usuario solicitante = usuarioRepository.findById(dto.solicitanteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario solicitante no encontrado con el ID: " + dto.solicitanteId()));

        if (solicitante.getTipo() != TipoUsuario.SOLICITANTE) {
            throw new BusinessValidationException(
                    "Solo los usuarios de tipo SOLICITANTE pueden abrir tickets");
        }

        PrioridadeChamado prioridade = dto.prioridade() != null ? dto.prioridade() : PrioridadeChamado.MEDIA;

        Chamado chamado = Chamado.builder()
                .titulo(dto.titulo())
                .descricao(dto.descricao())
                .local(dto.local())
                .prioridade(prioridade)
                .status(StatusChamado.ABERTO)
                .dataAbertura(LocalDateTime.now())
                .categoria(categoria)
                .solicitante(solicitante)
                .build();

        return ChamadoResponseDTO.fromEntity(chamadoRepository.save(chamado));
    }

    @Override
    @Transactional
    public ChamadoResponseDTO assumir(Long id, AssumirChamadoDTO dto) {
        Chamado chamado = buscarChamado(id);

        if (chamado.getStatus() != StatusChamado.ABERTO) {
            throw new BusinessValidationException(
                    "El chamado debe estar en estado ABERTO para ser asumido");
        }

        Usuario tecnico = usuarioRepository.findById(dto.tecnicoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El técnico indicado no existe con el ID: " + dto.tecnicoId()));

        if (tecnico.getTipo() != TipoUsuario.TECNICO) {
            throw new BusinessValidationException(
                    "El usuario indicado no es de tipo TECNICO");
        }

        Long setorCategoriaId = (chamado.getCategoria() != null && chamado.getCategoria().getSetor() != null)
                ? chamado.getCategoria().getSetor().getId() : null;
        Long setorTecnicoId = (tecnico.getSetor() != null) ? tecnico.getSetor().getId() : null;

        if (setorCategoriaId == null || !Objects.equals(setorCategoriaId, setorTecnicoId)) {
            throw new BusinessValidationException(
                    "El técnico solo puede asumir chamados cuya categoría pertenezca a su sector");
        }

        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        chamado.setTecnico(tecnico);

        return ChamadoResponseDTO.fromEntity(chamadoRepository.save(chamado));
    }

    @Override
    @Transactional
    public ChamadoResponseDTO resolver(Long id, ResolverChamadoDTO dto) {
        Chamado chamado = buscarChamado(id);

        if (chamado.getStatus() != StatusChamado.EM_ATENDIMENTO) {
            throw new BusinessValidationException(
                    "El chamado debe estar en estado EM_ATENDIMENTO para ser resuelto");
        }

        if (dto.solucao() == null || dto.solucao().isBlank()) {
            throw new BusinessValidationException(
                    "La solución no puede estar vacía o en blanco");
        }

        if (chamado.getTecnico() == null
                || !Objects.equals(chamado.getTecnico().getId(), dto.tecnicoId())) {
            throw new BusinessValidationException(
                    "Solo el técnico asignado puede resolver el chamado");
        }

        chamado.setStatus(StatusChamado.RESOLVIDO);
        chamado.setSolucao(dto.solucao());
        chamado.setDataFechamento(LocalDateTime.now());

        return ChamadoResponseDTO.fromEntity(chamadoRepository.save(chamado));
    }

    @Override
    @Transactional
    public ChamadoResponseDTO cancelar(Long id, CancelarChamadoDTO dto) {
        Chamado chamado = buscarChamado(id);

        if (chamado.getStatus() != StatusChamado.ABERTO) {
            throw new BusinessValidationException(
                    "Solo se pueden cancelar chamados en estado ABERTO");
        }

        boolean esSolicitante = chamado.getSolicitante() != null
                && Objects.equals(chamado.getSolicitante().getId(), dto.usuarioId());

        if (!esSolicitante) {
            throw new BusinessValidationException(
                    "Solo el solicitante que abrió el chamado puede cancelarlo");
        }

        chamado.setStatus(StatusChamado.CANCELADO);

        return ChamadoResponseDTO.fromEntity(chamadoRepository.save(chamado));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChamadoResponseDTO> listar(
            StatusChamado status,
            PrioridadeChamado prioridade,
            Long setorId,
            Long categoriaId,
            Long solicitanteId,
            Long tecnicoId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            Pageable pageable) {

        Specification<Chamado> specification = ChamadoSpecification.comFiltros(
                status, prioridade, setorId, categoriaId, solicitanteId, tecnicoId, dataInicio, dataFim);

        return chamadoRepository.findAll(specification, pageable)
                .map(ChamadoResponseDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChamadoResponseDTO> listarPorUsuario(Long usuarioId, StatusChamado status, Pageable pageable) {
        return chamadoRepository.findAll(ChamadoSpecification.porUsuario(usuarioId, status), pageable)
                .map(ChamadoResponseDTO::fromEntity);
    }

    private Chamado buscarChamado(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chamado no encontrado con el ID: " + id));
    }
}
