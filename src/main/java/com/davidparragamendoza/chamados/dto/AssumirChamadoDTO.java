package com.davidparragamendoza.chamados.dto;

import jakarta.validation.constraints.NotNull;

public record AssumirChamadoDTO(

        @NotNull(message = "El técnico es obligatorio")
        Long tecnicoId
) {
}
