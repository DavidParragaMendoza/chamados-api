package com.davidparragamendoza.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InteracaoRequestDTO(

        @NotBlank(message = "El texto es obligatorio")
        String texto,

        @NotNull(message = "El autor es obligatorio")
        Long autorId
) {
}
