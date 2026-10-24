package com.davidparragamendoza.chamados.dto;

import com.davidparragamendoza.chamados.model.Interacao;

import java.time.LocalDateTime;

public record InteracaoResponseDTO(
        Long id,
        String texto,
        LocalDateTime dataHora,
        Long autorId,
        String autorNome
) {

    public static InteracaoResponseDTO fromEntity(Interacao interacao) {
        return new InteracaoResponseDTO(
                interacao.getId(),
                interacao.getTexto(),
                interacao.getDataHora(),
                interacao.getAutor() != null ? interacao.getAutor().getId() : null,
                interacao.getAutor() != null ? interacao.getAutor().getNome() : null
        );
    }
}
