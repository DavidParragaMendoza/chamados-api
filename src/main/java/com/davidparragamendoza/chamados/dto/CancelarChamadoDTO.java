package com.davidparragamendoza.chamados.dto;

import jakarta.validation.constraints.NotNull;

public record CancelarChamadoDTO(

        @NotNull(message = "El usuario es obligatorio")
        Long usuarioId
) {
}
