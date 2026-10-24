package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.AssumirChamadoDTO;
import com.davidparragamendoza.chamados.dto.CancelarChamadoDTO;
import com.davidparragamendoza.chamados.dto.ChamadoCreateDTO;
import com.davidparragamendoza.chamados.dto.ChamadoDetalheResponseDTO;
import com.davidparragamendoza.chamados.dto.ChamadoResponseDTO;
import com.davidparragamendoza.chamados.dto.ResolverChamadoDTO;
import com.davidparragamendoza.chamados.model.PrioridadeChamado;
import com.davidparragamendoza.chamados.model.StatusChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ChamadoService {

    ChamadoResponseDTO abrir(ChamadoCreateDTO dto);

    ChamadoDetalheResponseDTO buscarPorId(Long id);

    ChamadoResponseDTO assumir(Long id, AssumirChamadoDTO dto);

    ChamadoResponseDTO resolver(Long id, ResolverChamadoDTO dto);

    ChamadoResponseDTO cancelar(Long id, CancelarChamadoDTO dto);

    Page<ChamadoResponseDTO> listar(
            StatusChamado status,
            PrioridadeChamado prioridade,
            Long setorId,
            Long categoriaId,
            Long solicitanteId,
            Long tecnicoId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            Pageable pageable);

    Page<ChamadoResponseDTO> listarPorUsuario(Long usuarioId, StatusChamado status, Pageable pageable);
}
