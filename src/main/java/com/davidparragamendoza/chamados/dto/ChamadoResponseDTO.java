package com.davidparragamendoza.chamados.dto;

import com.davidparragamendoza.chamados.model.Chamado;
import com.davidparragamendoza.chamados.model.PrioridadeChamado;
import com.davidparragamendoza.chamados.model.StatusChamado;

import java.time.LocalDateTime;

public record ChamadoResponseDTO(
        Long id,
        String titulo,
        String descricao,
        String local,
        PrioridadeChamado prioridade,
        StatusChamado status,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        String solucao,
        Long categoriaId,
        Long solicitanteId,
        Long tecnicoId
) {

    public static ChamadoResponseDTO fromEntity(Chamado chamado) {
        return new ChamadoResponseDTO(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getLocal(),
                chamado.getPrioridade(),
                chamado.getStatus(),
                chamado.getDataAbertura(),
                chamado.getDataFechamento(),
                chamado.getSolucao(),
                chamado.getCategoria() != null ? chamado.getCategoria().getId() : null,
                chamado.getSolicitante() != null ? chamado.getSolicitante().getId() : null,
                chamado.getTecnico() != null ? chamado.getTecnico().getId() : null
        );
    }
}
