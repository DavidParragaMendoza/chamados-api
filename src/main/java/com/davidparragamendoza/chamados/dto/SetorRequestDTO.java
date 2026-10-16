package com.davidparragamendoza.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetorRequestDTO (
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 250, message = "El nombre no puede superar los 250 caractres")
    String nome,

    @NotBlank(message = "La sigla es obligatoria")
    @Size(max = 50, message = "La sigla no puede superar los 50 caractres")
    String sigla

){}
