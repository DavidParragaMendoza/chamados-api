package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.SetorRequestDTO;
import com.davidparragamendoza.chamados.dto.SetorResponseDTO;
import com.davidparragamendoza.chamados.exception.BusinessConflictException;
import com.davidparragamendoza.chamados.exception.ResourceNotFoundException;
import com.davidparragamendoza.chamados.model.Setor;
import com.davidparragamendoza.chamados.repository.SetorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetorService {

    private final SetorRepository setorRepository;

    public SetorService(SetorRepository setorRepository) {
        this.setorRepository = setorRepository;
    }

    @Transactional(readOnly = true)
    public Page<SetorResponseDTO> listarTodos(String nome, Pageable pageable) {
        Page<Setor> setores = (nome != null && !nome.isBlank())
                ? setorRepository.findByNomeContainingIgnoreCase(nome, pageable)
                : setorRepository.findAll(pageable);

        return setores.map(SetorResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public SetorResponseDTO buscarPorId(Long id) {
        Setor setor = setorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sector no encontrado con el ID: " + id));
        return SetorResponseDTO.fromEntity(setor);
    }

    @Transactional
    public SetorResponseDTO atualizar(Long id, SetorRequestDTO dto) {
        Setor setor = setorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sector no encontrado con el ID: " + id));

        if (setorRepository.existsBySiglaIgnoreCaseAndIdNot(dto.sigla(), id)) {
            throw new BusinessConflictException("Ya existe un sector registrado con la sigla: " + dto.sigla());
        }

        setor.setNome(dto.nome());
        setor.setSigla(dto.sigla());
        return SetorResponseDTO.fromEntity(setorRepository.save(setor));
    }

    @Transactional
    public SetorResponseDTO salvar(SetorRequestDTO dto) {
        if (setorRepository.existsBySiglaIgnoreCase(dto.sigla())) {
            throw new BusinessConflictException("Ya existe un sector registrado con la sigla: " + dto.sigla());
        }

        Setor novoSetor = Setor.builder()
                .nome(dto.nome())
                .sigla(dto.sigla())
                .ativo(true)
                .build();

        return SetorResponseDTO.fromEntity(setorRepository.save(novoSetor));
    }

    @Transactional
    public void deletar(Long id) {
        if (!setorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Sector no encontrado con el ID: " + id);
        }

        if (setorRepository.existsCategoriaBySetorId(id) || setorRepository.existsUsuarioBySetorId(id)) {
            throw new BusinessConflictException(
                    "No se puede eliminar el sector porque tiene categorías o usuarios asociados");
        }

        setorRepository.deleteById(id);
    }
}
