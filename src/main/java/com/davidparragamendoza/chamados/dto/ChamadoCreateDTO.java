package com.davidparragamendoza.chamados.dto;

import com.davidparragamendoza.chamados.model.PrioridadeChamado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChamadoCreateDTO(

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 255, message = "El título no puede superar los 255 caracteres")
        String titulo,

        @NotBlank(message = "La descripción es obligatoria")
        String descricao,

        @NotBlank(message = "El local es obligatorio")
        @Size(max = 255, message = "El local no puede superar los 255 caracteres")
        String local,

        PrioridadeChamado prioridade,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @NotNull(message = "El solicitante es obligatorio")
        Long solicitanteId
) {
}
