package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.InteracaoRequestDTO;
import com.davidparragamendoza.chamados.dto.InteracaoResponseDTO;

import java.util.List;

public interface InteracaoService {

    InteracaoResponseDTO criar(Long chamadoId, InteracaoRequestDTO dto);

    List<InteracaoResponseDTO> listarPorChamado(Long chamadoId);
}
