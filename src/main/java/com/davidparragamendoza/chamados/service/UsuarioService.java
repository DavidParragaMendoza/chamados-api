package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.UsuarioRequestDTO;
import com.davidparragamendoza.chamados.dto.UsuarioResponseDTO;
import com.davidparragamendoza.chamados.model.TipoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    UsuarioResponseDTO salvar(UsuarioRequestDTO dto);

    Page<UsuarioResponseDTO> listar(String nome, TipoUsuario tipo, Long setorId, Pageable pageable);

    UsuarioResponseDTO buscarPorId(Long id);

    UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO dto);
}
