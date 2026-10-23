package com.davidparragamendoza.chamados.dto;

import com.davidparragamendoza.chamados.model.TipoUsuario;
import com.davidparragamendoza.chamados.model.Usuario;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String matricula,
        TipoUsuario tipo,
        Long setorId
) {

    public static UsuarioResponseDTO fromEntity(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getMatricula(),
                usuario.getTipo(),
                usuario.getSetor() != null ? usuario.getSetor().getId() : null
        );
    }
}
