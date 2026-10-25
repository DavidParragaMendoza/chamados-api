package com.davidparragamendoza.chamados.dto;

import com.davidparragamendoza.chamados.model.Chamado;
import com.davidparragamendoza.chamados.model.PrioridadeChamado;
import com.davidparragamendoza.chamados.model.StatusChamado;

import java.time.LocalDateTime;
import java.util.List;

public record ChamadoDetalheResponseDTO(
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
        Long tecnicoId,
        List<InteracaoResponseDTO> interacoes
) {

    public static ChamadoDetalheResponseDTO of(Chamado chamado, List<InteracaoResponseDTO> interacoes) {
        return new ChamadoDetalheResponseDTO(
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
                chamado.getTecnico() != null ? chamado.getTecnico().getId() : null,
                interacoes != null ? interacoes : List.of()
        );
    }
}
