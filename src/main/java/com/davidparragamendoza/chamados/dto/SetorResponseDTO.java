package com.davidparragamendoza.chamados.dto;

import com.davidparragamendoza.chamados.model.Setor;

public record SetorResponseDTO (
        Long id,
        String nome,
        String sigla,
        Boolean ativo
) {
    // Método utilitario para convertir la Entidad JPA al DTO de respuesta
    public static SetorResponseDTO fromEntity(Setor setor){
        return new SetorResponseDTO(
                setor.getId(),
                setor.getNome(),
                setor.getSigla(),
                setor.getAtivo()
        );
    }
}
