package com.davidparragamendoza.chamados.dto;

import com.fasterxml.jackson.annotation.JsonGetter;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponseDTO(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensagem,
        String caminho,
        Map<String, String> camposInvalidos
) {
    @JsonGetter("message")
    public String message() {
        return mensagem;
    }

    @JsonGetter("path")
    public String path() {
        return caminho;
    }
}
