package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.UsuarioRequestDTO;
import com.davidparragamendoza.chamados.dto.UsuarioResponseDTO;
import com.davidparragamendoza.chamados.exception.BusinessConflictException;
import com.davidparragamendoza.chamados.exception.ResourceNotFoundException;
import com.davidparragamendoza.chamados.model.Setor;
import com.davidparragamendoza.chamados.model.TipoUsuario;
import com.davidparragamendoza.chamados.model.Usuario;
import com.davidparragamendoza.chamados.repository.SetorRepository;
import com.davidparragamendoza.chamados.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SetorRepository setorRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, SetorRepository setorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.setorRepository = setorRepository;
    }

    @Override
    @Transactional
    public UsuarioResponseDTO salvar(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new BusinessConflictException("Ya existe un usuario registrado con el email: " + dto.email());
        }

        Setor setor = null;
        if (dto.setorId() != null) {
            setor = setorRepository.findById(dto.setorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sector no encontrado con el ID: " + dto.setorId()));
        }

        Usuario novoUsuario = Usuario.builder()
                .nome(dto.nome())
                .email(dto.email())
                .matricula(dto.matricula())
                .tipo(dto.tipo())
                .setor(setor)
                .build();

        return UsuarioResponseDTO.fromEntity(usuarioRepository.save(novoUsuario));
    }

    @Override
    public Page<UsuarioResponseDTO> listar(String nome, TipoUsuario tipo, Long setorId, Pageable pageable) {
        Specification<Usuario> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (nome != null && !nome.isBlank()) {
            String nomeFiltro = "%" + nome.toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("nome")), nomeFiltro));
        }

        if (tipo != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("tipo"), tipo));
        }

        if (setorId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("setor").get("id"), setorId));
        }

        return usuarioRepository.findAll(specification, pageable).map(UsuarioResponseDTO::fromEntity);
    }

    @Override
    public UsuarioResponseDTO buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponseDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con el ID: " + id));
    }

    @Override
    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con el ID: " + id));

        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(dto.email(), id)) {
            throw new BusinessConflictException("Ya existe un usuario registrado con el email: " + dto.email());
        }

        Setor setor = null;
        if (dto.setorId() != null) {
            setor = setorRepository.findById(dto.setorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sector no encontrado con el ID: " + dto.setorId()));
        }

        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setMatricula(dto.matricula());
        usuario.setTipo(dto.tipo());
        usuario.setSetor(setor);

        return UsuarioResponseDTO.fromEntity(usuarioRepository.save(usuario));
    }
}
