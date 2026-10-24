package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.InteracaoRequestDTO;
import com.davidparragamendoza.chamados.dto.InteracaoResponseDTO;
import com.davidparragamendoza.chamados.exception.BusinessValidationException;
import com.davidparragamendoza.chamados.exception.ResourceNotFoundException;
import com.davidparragamendoza.chamados.model.Chamado;
import com.davidparragamendoza.chamados.model.Interacao;
import com.davidparragamendoza.chamados.model.StatusChamado;
import com.davidparragamendoza.chamados.model.Usuario;
import com.davidparragamendoza.chamados.repository.ChamadoRepository;
import com.davidparragamendoza.chamados.repository.InteracaoRepository;
import com.davidparragamendoza.chamados.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class InteracaoServiceImpl implements InteracaoService {

    private final InteracaoRepository interacaoRepository;
    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;

    public InteracaoServiceImpl(InteracaoRepository interacaoRepository,
                                ChamadoRepository chamadoRepository,
                                UsuarioRepository usuarioRepository) {
        this.interacaoRepository = interacaoRepository;
        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public InteracaoResponseDTO criar(Long chamadoId, InteracaoRequestDTO dto) {
        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chamado no encontrado con el ID: " + chamadoId));

        Usuario autor = usuarioRepository.findById(dto.autorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autor no encontrado con el ID: " + dto.autorId()));

        if (chamado.getStatus() == StatusChamado.RESOLVIDO || chamado.getStatus() == StatusChamado.CANCELADO) {
            throw new BusinessValidationException(
                    "No se permiten registrar nuevas interacciones en chamados resueltos o cancelados");
        }

        boolean esSolicitante = chamado.getSolicitante() != null
                && Objects.equals(chamado.getSolicitante().getId(), autor.getId());
        boolean esTecnicoAsignado = chamado.getTecnico() != null
                && Objects.equals(chamado.getTecnico().getId(), autor.getId());

        if (!esSolicitante && !esTecnicoAsignado) {
            throw new BusinessValidationException(
                    "Solo el solicitante o el técnico asignado pueden registrar interacciones en el chamado");
        }

        Interacao interacao = Interacao.builder()
                .texto(dto.texto())
                .dataHora(LocalDateTime.now())
                .chamado(chamado)
                .autor(autor)
                .build();

        return InteracaoResponseDTO.fromEntity(interacaoRepository.save(interacao));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InteracaoResponseDTO> listarPorChamado(Long chamadoId) {
        if (!chamadoRepository.existsById(chamadoId)) {
            throw new ResourceNotFoundException(
                    "Chamado no encontrado con el ID: " + chamadoId);
        }

        return interacaoRepository.findByChamadoIdOrderByDataHoraAsc(chamadoId).stream()
                .map(InteracaoResponseDTO::fromEntity)
                .toList();
    }
}
