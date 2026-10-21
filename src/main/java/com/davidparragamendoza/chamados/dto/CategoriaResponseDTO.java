package com.davidparragamendoza.chamados.dto;

import com.davidparragamendoza.chamados.model.Categoria;

public record CategoriaResponseDTO(
        Long id,
        String nome,
        Long setorId
) {

    public static CategoriaResponseDTO fromEntity(Categoria categoria) {
        return new CategoriaResponseDTO(
                categoria.getId(),
                categoria.getNome(),
                categoria.getSetor().getId()
        );
    }
}
