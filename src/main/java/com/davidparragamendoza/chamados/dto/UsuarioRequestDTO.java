package com.davidparragamendoza.chamados.dto;

import com.davidparragamendoza.chamados.model.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        String nome,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 255, message = "El email no puede superar los 255 caracteres")
        String email,

        @NotBlank(message = "La matrícula es obligatoria")
        @Size(max = 100, message = "La matrícula no puede superar los 100 caracteres")
        String matricula,

        @NotNull(message = "El tipo de usuario es obligatorio")
        TipoUsuario tipo,

        Long setorId
) {
}
