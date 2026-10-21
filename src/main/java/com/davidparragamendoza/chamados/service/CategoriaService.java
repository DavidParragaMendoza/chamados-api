package com.davidparragamendoza.chamados.service;

import com.davidparragamendoza.chamados.dto.CategoriaRequestDTO;
import com.davidparragamendoza.chamados.dto.CategoriaResponseDTO;
import com.davidparragamendoza.chamados.exception.BusinessConflictException;
import com.davidparragamendoza.chamados.exception.ResourceNotFoundException;
import com.davidparragamendoza.chamados.model.Categoria;
import com.davidparragamendoza.chamados.model.Setor;
import com.davidparragamendoza.chamados.repository.CategoriaRepository;
import com.davidparragamendoza.chamados.repository.SetorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final SetorRepository setorRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, SetorRepository setorRepository) {
        this.categoriaRepository = categoriaRepository;
        this.setorRepository = setorRepository;
    }

    @Transactional
    public CategoriaResponseDTO salvar(CategoriaRequestDTO dto) {
        Setor setor = setorRepository.findById(dto.setorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sector no encontrado con el ID: " + dto.setorId()));

        Categoria novaCategoria = Categoria.builder()
                .nome(dto.nome())
                .setor(setor)
                .build();

        return CategoriaResponseDTO.fromEntity(categoriaRepository.save(novaCategoria));
    }

    @Transactional(readOnly = true)
    public CategoriaResponseDTO buscarPorId(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con el ID: " + id));
        return CategoriaResponseDTO.fromEntity(categoria);
    }

    @Transactional
    public CategoriaResponseDTO atualizar(Long id, CategoriaRequestDTO dto) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con el ID: " + id));

        Setor setor = setorRepository.findById(dto.setorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sector no encontrado con el ID: " + dto.setorId()));

        categoria.setNome(dto.nome());
        categoria.setSetor(setor);
        return CategoriaResponseDTO.fromEntity(categoriaRepository.save(categoria));
    }

    @Transactional(readOnly = true)
    public Page<CategoriaResponseDTO> listar(String nome, Long setorId, Pageable pageable) {
        Specification<Categoria> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (nome != null && !nome.isBlank()) {
            String nomeFiltro = "%" + nome.toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("nome")), nomeFiltro));
        }

        if (setorId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("setor").get("id"), setorId));
        }

        return categoriaRepository.findAll(specification, pageable).map(CategoriaResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listarPorSetor(Long setorId) {
        if (!setorRepository.existsById(setorId)) {
            throw new ResourceNotFoundException("Sector no encontrado con el ID: " + setorId);
        }

        return categoriaRepository.findBySetorId(setorId).stream()
                .map(CategoriaResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void deletar(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Categoría no encontrada con el ID: " + id);
        }

        if (categoriaRepository.existsChamadoByCategoriaId(id)) {
            throw new BusinessConflictException(
                    "No se puede eliminar la categoría porque tiene chamados asociados");
        }

        categoriaRepository.deleteById(id);
    }
}
