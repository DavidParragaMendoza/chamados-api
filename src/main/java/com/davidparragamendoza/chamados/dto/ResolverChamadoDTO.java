package com.davidparragamendoza.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResolverChamadoDTO(

        @NotBlank(message = "La solución es obligatoria")
        String solucao,

        @NotNull(message = "El técnico es obligatorio")
        Long tecnicoId
) {
}
